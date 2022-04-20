package de.sf666.bayrol.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BayrolHttpConnector
{
    private static final Logger log = LoggerFactory.getLogger(BayrolHttpConnector.class.getName());

    private static final String BASE_URL = "https://www.bayrol-poolaccess.de";
    private static final String BASE_PATH = "/webview/p";
    private static final String LOGIN_URI = "/login.php?r=reg";
    private static final String PLANTS_URI = "/plants.php";
    private static final String DATA_PATH = "/webview/getdata.php?cid=";

    private static final Pattern cidPattern = Pattern.compile("<a href=\\\"plant_settings\\.php\\?c=([0-9]+)", Pattern.DOTALL);
    private static final Pattern dataPattern = Pattern.compile("\\[pH\\].*?<h1>(\\d+\\.\\d+)</h1></div>.*?\\[mg/l\\].*?<h1>(\\d+\\.\\d+).*C].*<h1>(\\d+\\.\\d+)", Pattern.DOTALL);

    private String username = "";
    private String password = "";

    private Set<String> cids = new HashSet<>();
    private Map<String, BayrolMainDisplayValues> currentStates = new HashMap<>();

    private OkHttpClient okClient = null;

    public BayrolHttpConnector(String username, String password)
    {
        this.username = username;
        this.password = password;

        initOkClient();

    }

    private void initOkClient()
    {
        okClient = new OkHttpClient.Builder().cookieJar(new CookieJar()
        {
            private List<Cookie> c = new ArrayList<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies)
            {
                for (Cookie nc : cookies)
                {
                    deleteIfExists(nc);
                }
                c.addAll(cookies);
            }

            private void deleteIfExists(Cookie nc)
            {
                List<Cookie> deleteMe = new ArrayList<>();
                for (Cookie cookie : c)
                {
                    if (cookie.name().equals(nc.name()))
                    {
                        deleteMe.add(cookie);
                    }
                }
                c.removeAll(deleteMe);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url)
            {
                log.debug("request cookies");
                for (Cookie cookie : c)
                {
                    log.debug(cookie.toString());
                }

                return c;
            }
        }).build();
    }

    public void getSessionID() throws UnsupportedEncodingException
    {
        Request request = new Request.Builder().url(BASE_URL + BASE_PATH + LOGIN_URI).build();

        Call call = okClient.newCall(request);
        try (Response response = call.execute())
        {
            log.info("Getting session ID. return code should be 200. Actual value : " + response.code());

            printHeaders(response);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public BayrolMainDisplayValues getCurrentState(String cid)
    {
        return currentStates.get(cid);
    }

    public void updateAllStates()
    {
        for (String cid : cids)
        {
            updateAndGetState(cid);
        }
    }

    public BayrolMainDisplayValues updateAndGetState(String cid)
    {
        Request request = new Request.Builder().url(BASE_URL + DATA_PATH + cid).build();

        Call call = okClient.newCall(request);
        try (Response response = call.execute())
        {

            if (response.code() != 200)
            {
                log.warn("retuned code is " + response.code());
            }

            String resp = response.body().string();

            Matcher m = dataPattern.matcher(resp);
            if (m.find())
            {
                BayrolMainDisplayValues currentState = getCurrentStateForCid(cid);
                currentState.ph = parseAsDouble(m.group(1));
                currentState.cl = parseAsDouble(m.group(2));
                currentState.temp = parseAsDouble(m.group(3));
                log.debug(currentState.toString());
                currentStates.put(cid, currentState);
                return currentState;
            }
            else
            {
                log.error("unable to parse data from repsonse. " + resp);
                log.error(resp);
            }
        }
        catch (Exception e)
        {
            log.error("getData error", e);
        }
        throw new RuntimeException("current state not retrievable");
    }

    private BayrolMainDisplayValues getCurrentStateForCid(String cid)
    {
        BayrolMainDisplayValues currentState = currentStates.get(cid);
        if (currentState == null)
        {
            currentState = new BayrolMainDisplayValues();
            currentStates.put(cid, currentState);
        }
        return currentState;
    }

    private double parseAsDouble(String group)
    {
        NumberFormat format = NumberFormat.getInstance(Locale.US);
        try
        {
            Number number = format.parse(group);
            return number.doubleValue();
        }
        catch (ParseException e)
        {
            log.error("parsing value failed", e);
            return -999;
        }
    }

    public void login() throws UnsupportedEncodingException
    {
        RequestBody formBody = new FormBody.Builder().add("username", username).add("password", password).add("login", "Anmelden").build();

        Request request = new Request.Builder().url(BASE_URL + BASE_PATH + LOGIN_URI).post(formBody).build();

        Call call = okClient.newCall(request);
        try (Response response = call.execute())
        {
            printHeaders(response);
        }
        catch (IOException e)
        {
            log.error("login error", e);
        }
    }

    private void printHeaders(Response response)
    {
        if (log.isDebugEnabled())
        {
            for (Pair<? extends String, ? extends String> header : response.headers())
            {
                log.debug(header.component1() + " : " + header.component2());
            }
        }
    }

    public Set<Integer> scanForPlantIds()
    {
        Set<Integer> plants = new HashSet<>();
        Request request = new Request.Builder().url(BASE_URL + BASE_PATH + PLANTS_URI).build();
        try (Response response = okClient.newCall(request).execute())
        {
            String resp = response.body().string();

            Matcher m = cidPattern.matcher(resp);
            while (m.find())
            {
                String cid = m.group(1);
                log.debug("found cid : " + cid);
                cids.add(cid);
            }
        }
        catch (IOException e)
        {
            log.error("getPlantIDs error", e);
        }
        return plants;
    }

    public void connectToWebPortal() throws UnsupportedEncodingException
    {
        cids.clear();
        getSessionID();
        login();
        scanForPlantIds();
    }

    public Set<String> getPlantCids()
    {
        return cids;
    }

    /**
     * Howto use as a lib ...
     * 
     * @param args
     * @throws UnsupportedEncodingException
     */
    public static void main(String[] args) throws UnsupportedEncodingException
    {
        // args : Username - Password
        BayrolHttpConnector c = new BayrolHttpConnector(args[0], args[1]);
        c.connectToWebPortal();
        Optional<String> cid = c.getPlantCids().stream().findAny();
        if (cid.isPresent())
        {
            c.updateAndGetState(cid.get());
        }
        else
        {
            log.warn("NO CIDs found ");
        }
    }
}
