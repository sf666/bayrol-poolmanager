package de.sf666.bayrol.rest;

import java.util.Set;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import de.sf666.bayrol.bridge.BayrolBridge;
import de.sf666.bayrol.domain.BayrolMainDisplayValues;
import de.sf666.bayrol.export.ExportFactory;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RestController
@RequestMapping("/Bayrol")
public class RestBayrolBridge {

	private static final Logger log = LoggerFactory.getLogger(RestBayrolBridge.class.getName());

	@Autowired
	private BayrolBridge bayrolBridge = null;

	@Autowired
	private ExportFactory exportFactory = null;

	@GetMapping("/light/{cid}")
	public String lightStatus(@PathVariable("cid") String cid) {
		// TODO
		return "0";
	}

	@GetMapping("/setLight/{cid}/{on}")
	public void lightOff(@PathVariable("cid") String cid, @PathVariable("on") String on) {
		if (StringUtil.isBlank(cid)) {
			log.debug("pool plantid shall not be empty");
			return;
		}
		if ("1".equals(on)) {
			log.debug("light on");
			bayrolBridge.lightOn(cid);
		} else if ("0".equals(on)) {
			log.debug("light off");
			bayrolBridge.lightOff(cid);
		} else {
			log.debug("unknown light status");
		}
	}

	/**
	 * Deliver cached data
	 * 
	 * @param cid
	 * @return
	 */
	@GetMapping("/currentState/{cid}")
	public BayrolMainDisplayValues getCurrentState(@PathVariable("cid") String cid) {
		return bayrolBridge.getCurrentState(cid);
	}

	/**
	 * Deliver cached data
	 * 
	 * @param cid
	 * @param resturn format
	 * @return
	 */
	@GetMapping(value = "/currentState/{cid}/{format}", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getCurrentState(@PathVariable("cid") String cid, @PathVariable("format") String format) {
		return exportFactory.lookupExporter(format).formatMetrics(bayrolBridge.getCurrentState(cid), cid);
	}

	@GetMapping("/getPlantIds")
	public Set<String> getPlantIds() {
		return bayrolBridge.getPlantCids();
	}

	/**
	 * Deliver live data
	 * 
	 * @param cid
	 * @return
	 */
	@GetMapping("/currentLiveState/{cid}")
	public BayrolMainDisplayValues getCurrentLiveState(@PathVariable("cid") String cid) {
		return bayrolBridge.updateAndGetState(cid);
	}

	/**
	 * Deliver live data
	 * 
	 * @param cid
	 * @param resturn format
	 * @return
	 */
	@GetMapping(value = "/currentLiveState/{cid}/{format}", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getCurrentLiveState(@PathVariable("cid") String cid, @PathVariable("format") String format) {
		return exportFactory.lookupExporter(format).formatMetrics(bayrolBridge.updateAndGetState(cid), cid);
	}

}
