package de.sf666.bayrol.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.sf666.bayrol.domain.BayrolMainDisplayValues;
import kotlin.Pair;
import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BayrolHttpConnector {

	private static final Logger log = LoggerFactory.getLogger(BayrolHttpConnector.class.getName());

	private static final String BASE_URL = "https://www.bayrol-poolaccess.de";
	private static final String BASE_PATH = "/webview/p";
	private static final String LOGIN_URI = "/login.php?r=reg";
	private static final String PLANTS_URI = "/plants.php";
	private static final String DATA_PATH = "/webview/getdata.php?cid=";
	private static final String WEBGUI_PATH = "/cgi-bin/webgui.fcgi?sid=";
	private static final String LIGHT_STATUS_PAYLOAD = "{\"get\" :[\"60.5433.value\"]}";
	private static final String LIGHT_ON_PAYLOAD = "{\"get\" :[\"60.5433.value\"],\"set\" :{\"60.5433.value\" :\"1\" }}";
	private static final String LIGHT_OFF_PAYLOAD = "{\"get\" :[\"60.5433.value\"],\"set\" :{\"60.5433.value\" :\"4\" }}";
	private static final String WEB_VIEW_PATH = "/webview/pm5/?c=";

	private static final Pattern lightStatePattern = Pattern.compile(".*60\\.5433\\.value\"\\s*:\\s*\"(.)", Pattern.DOTALL);
	private static final Pattern cidPattern = Pattern.compile("<a href=\\\"plant_settings\\.php\\?c=([0-9]+)", Pattern.DOTALL);
	private static final Pattern sidPattern = Pattern.compile(".*init\\('(\\w*)'", Pattern.DOTALL);
	private static final Pattern cgiUserPass = Pattern.compile(".*17401\\.user\" value=\"(\\w*).*17401.pass\" value=\"(\\w*)",
		Pattern.DOTALL);
	private static final Pattern dataPattern = Pattern
		.compile("\\[pH\\].*?<h1>(\\d+\\.\\d+)</h1></div>.*?\\[mg/l\\].*?<h1>(\\d+\\.\\d+).*C].*<h1>(\\d+\\.\\d+)", Pattern.DOTALL);

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

	private String username = "";
	private String password = "";

	private HashMap<String, String> plantIdSidMap = new HashMap<>();
	private Map<String, BayrolMainDisplayValues> currentStates = new HashMap<>();

	private OkHttpClient okClient = null;

	private int errorCount = 0;
	private boolean loginSuccess = false;
	private volatile boolean reconnectRunning = false;

	public BayrolHttpConnector(String username, String password) {
		this.username = username;
		this.password = password;

		initOkClient();
	}

	private void initOkClient() {
		okClient = new OkHttpClient.Builder().cookieJar(new CookieJar() {

			private List<Cookie> c = new ArrayList<>();

			@Override
			public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
				for (Cookie nc : cookies) {
					deleteIfExists(nc);
				}
				c.addAll(cookies);
			}

			private void deleteIfExists(Cookie nc) {
				List<Cookie> deleteMe = new ArrayList<>();
				for (Cookie cookie : c) {
					if (cookie.name().equals(nc.name())) {
						deleteMe.add(cookie);
					}
				}
				c.removeAll(deleteMe);
			}

			@Override
			public List<Cookie> loadForRequest(HttpUrl url) {
				log.debug("request cookies");
				for (Cookie cookie : c) {
					log.debug(cookie.toString());
				}

				return c;
			}
		}).build();
	}

	public UserPass initCgiSession(String plantId) {
		String u = BASE_URL + WEBGUI_PATH + plantIdSidMap.get(plantId) + "&cmd=9.17401.0";
		Request request = new Request.Builder().url(u).get().build();

		Call call = okClient.newCall(request);
		try (Response response = call.execute()) {
			String b = response.body().string();
			log.debug(b);
			Matcher m = cgiUserPass.matcher(b);
			UserPass up = new UserPass();
			if (m.find()) {
				up.user = m.group(1);
				up.pass = m.group(2);
			}
			return up;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String readLightState(String plantId) {
		String readLightStateData = "{\"get\" :[\"60.5433.value\"]}";
		RequestBody body = RequestBody.create(readLightStateData, MediaType.parse("application/json"));
		String u = BASE_URL + WEBGUI_PATH + plantIdSidMap.get(plantId);
		Request request = new Request.Builder().url(u).post(body).build();

		Call call = okClient.newCall(request);
		try (Response response = call.execute()) {
			String b = response.body().string();
			log.debug(b);
			
			Matcher m = lightStatePattern.matcher(b);
			if (m.find()) {
				String state = m.group(1);
				log.debug("Light State : " + state);
				return state;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public String loginWebguiCgi(String plantId, UserPass up) {
		RequestBody body = RequestBody.create(
			String.format("{\"set\" :{\"9.17401.user\" :\"%s\" ,\"9.17401.pass\" :\"%s\" }}", up.user, up.pass),
			MediaType.parse("application/json"));
		String u = BASE_URL + WEBGUI_PATH + plantIdSidMap.get(plantId);
		Request request = new Request.Builder().url(u).post(body).build();

		Call call = okClient.newCall(request);
		try (Response response = call.execute()) {
			String b = response.body().string();
			log.debug("loginWebguiCgi",b);
			return b;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public void lightOn(String plantId) {
		// get sid from cid
		RequestBody body = RequestBody.create(LIGHT_ON_PAYLOAD, MediaType.parse("application/json"));
		String u = BASE_URL + WEBGUI_PATH + plantIdSidMap.get(plantId);
		Request request = new Request.Builder().url(u).post(body).build();

		Call call = okClient.newCall(request);
		try (Response response = call.execute()) {
			String b = response.body().string();
			log.debug(b);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void lightOff(String plantId) {
		// get sid from cid

		RequestBody body = RequestBody.create(LIGHT_OFF_PAYLOAD, MediaType.parse("application/json"));
		Request request = new Request.Builder().url(BASE_URL + WEBGUI_PATH + plantIdSidMap.get(plantId)).post(body).build();

		Call call = okClient.newCall(request);
		try (Response response = call.execute()) {
			String b = response.body().string();
			log.debug(b);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getWebviewSessionID() {
		Request request = new Request.Builder().url(BASE_URL + WEB_VIEW_PATH + LOGIN_URI).build();
		String sid = "";
		Call call = okClient.newCall(request);
		try (Response response = call.execute()) {
			if (response.header("set-cookie") != null) {
				sid = response.header("set-cookie").substring("PHPSESSID=".length());
				log.info("Getting session ID : " + sid);
			}

			printHeaders(response);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sid;

	}

	public String getSessionID() {
		Request request = new Request.Builder().url(BASE_URL + BASE_PATH + LOGIN_URI).build();
		String sid = "";
		Call call = okClient.newCall(request);
		try (Response response = call.execute()) {
			if (response.header("set-cookie") != null) {
				sid = response.header("set-cookie").substring("PHPSESSID=".length());
				log.info("Getting session ID : " + sid);
			}

			printHeaders(response);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sid;
	}

	public BayrolMainDisplayValues getCurrentState(String cid) {
		return currentStates.get(cid);
	}

	public void updateAllStates() {
		for (String cid : plantIdSidMap.keySet()) {
			updateAndGetState(cid);
		}
	}

	public BayrolMainDisplayValues updateAndGetState(String cid) {
		Request request = new Request.Builder().url(BASE_URL + DATA_PATH + cid).build();

		Call call = okClient.newCall(request);
		try (Response response = call.execute()) {

			if (response.code() != 200) {
				log.warn("retuned code is " + response.code());
			}

			String resp = response.body().string();

			Matcher m = dataPattern.matcher(resp);
			if (m.find()) {
				BayrolMainDisplayValues currentState = getCurrentStateForCid(cid);
				currentState.date = getCurrentIsoDate();
				currentState.ph = parseAsDouble(m.group(1));
				currentState.cl = parseAsDouble(m.group(2));
				currentState.temp = parseAsDouble(m.group(3));
				log.info(currentState.toString());
				currentStates.put(cid, currentState);
				return currentState;
			} else {
				log.error("unable to parse data from repsonse. " + resp);
				log.error(resp);
				reconnectAfterFailure();
			}
		} catch (Exception e) {
			log.error("getData error", e);
			reconnectAfterFailure();
		}
		throw new RuntimeException("current state not retrievable");
	}

	private void reconnectAfterFailure() {
		if (!reconnectRunning) {
			reconnectRunning = true;
			Runnable recon = new Runnable() {

				@Override
				public void run() {
					errorCount++;
					loginSuccess = false;
					while (!loginSuccess) {
						try {
							log.warn("reconnect attempt " + errorCount);
							connectToWebPortal();
							log.warn("try to reconnect again in 5 Minutes");
							Thread.sleep(1000 * 60 * 5);
						} catch (Exception e) {
							log.error("failed with ", e);
						}
					}

					log.warn("successfully reconnected.");
					errorCount = 0;
					reconnectRunning = false;
				}
			};
			Thread reconThread = new Thread(recon, "Reconnection to web-portal");
			reconThread.start();
		} else {
			log.debug("reconnect already running.");
		}
	}

	private String getCurrentIsoDate() {
		// Input
		Date date = new Date(System.currentTimeMillis());
		sdf.setTimeZone(TimeZone.getTimeZone("CET"));
		return sdf.format(date);
	}

	private BayrolMainDisplayValues getCurrentStateForCid(String cid) {
		BayrolMainDisplayValues currentState = currentStates.get(cid);
		if (currentState == null) {
			currentState = new BayrolMainDisplayValues();
			currentStates.put(cid, currentState);
		}
		return currentState;
	}

	private double parseAsDouble(String group) {
		NumberFormat format = NumberFormat.getInstance(Locale.US);
		try {
			Number number = format.parse(group);
			return number.doubleValue();
		} catch (ParseException e) {
			log.error("parsing value failed", e);
			return -999;
		}
	}

	public void login() {
		RequestBody formBody = new FormBody.Builder().add("username", username).add("password", password).add("login", "Anmelden").build();

		Request request = new Request.Builder().url(BASE_URL + BASE_PATH + LOGIN_URI).post(formBody).build();

		Call call = okClient.newCall(request);
		try (Response response = call.execute()) {
			if (response.message().equalsIgnoreCase("found")) {
				loginSuccess = true;
			} else {
				loginSuccess = false;
			}
			printHeaders(response);
		} catch (IOException e) {
			log.error("login error", e);
		}
	}

	private void printHeaders(Response response) {
		if (log.isInfoEnabled()) {
			for (Pair<? extends String, ? extends String> header : response.headers()) {
				log.info(header.component1() + " : " + header.component2());
			}
		}
	}

	public void scanForPlantIds() {
		Request request = new Request.Builder().url(BASE_URL + BASE_PATH + PLANTS_URI).build();
		try (Response response = okClient.newCall(request).execute()) {
			String resp = response.body().string();

			Matcher m = cidPattern.matcher(resp);
			while (m.find()) {
				String cid = m.group(1);
				log.debug("found cid : " + cid);
				String sid = getSidForPlantId(cid);
				plantIdSidMap.put(cid, sid);

			}
		} catch (IOException e) {
			log.error("getPlantIDs error", e);
		}
	}

	public void connectToWebPortal() {
		plantIdSidMap.clear();
		getSessionID();
		login();
		scanForPlantIds();
		loginCgiAllPlanIt();
	}

	private void loginCgiAllPlanIt() {
		for (String plantId : plantIdSidMap.keySet()) {
			UserPass up = initCgiSession(plantId);
			loginWebguiCgi(plantId, up);
		}
	}

	private String getSidForPlantId(String plantId) {
		Request request = new Request.Builder().url(BASE_URL + WEB_VIEW_PATH + plantId).build();
		try (Response response = okClient.newCall(request).execute()) {
			String resp = response.body().string();

			Matcher m = sidPattern.matcher(resp);
			while (m.find()) {
				String sid = m.group(1);
				log.debug("found sid : " + sid);
				return sid;
			}
		} catch (IOException e) {
			log.error("getPlantIDs error", e);
		}
		return "";
	}

	public Set<String> getPlantCids() {
		return plantIdSidMap.keySet();
	}

	/**
	 * Howto use as a lib ...
	 * 
	 * @param args
	 * @throws UnsupportedEncodingException
	 */
	public static void main(String[] args) throws UnsupportedEncodingException {
		// args : Username - Password
		BayrolHttpConnector c = new BayrolHttpConnector(args[0], args[1]);
		c.connectToWebPortal();
		Optional<String> cid = c.getPlantCids().stream().findAny();
		if (cid.isPresent()) {
			c.updateAndGetState(cid.get());
		} else {
			log.warn("NO CIDs found ");
		}

		// Light ON
		c.lightOn("12799");
		try {
			Thread.sleep(10000l);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		c.lightOff("12799");
	}
}
