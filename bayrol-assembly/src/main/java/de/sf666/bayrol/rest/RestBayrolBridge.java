package de.sf666.bayrol.rest;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.sf666.bayrol.bridge.BayrolBridge;
import de.sf666.bayrol.domain.BayrolMainDisplayValues;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RestController
@RequestMapping("/Bayrol")
public class RestBayrolBridge
{
    @Autowired
    private BayrolBridge bayrolBridge = null;

    /**
     * Deliver cached data
     * 
     * @param cid
     * @return
     */
    @GetMapping("/currentState/{cid}")
    public BayrolMainDisplayValues getCurrentState(@PathVariable("cid") String cid)
    {
        return bayrolBridge.getCurrentState(cid);
    }

    @GetMapping("/getPlantIds")
    public Set<String> getPlantIds()
    {
        return bayrolBridge.getPlantCids();
    }

    /**
     * Deliver live data
     * 
     * @param cid
     * @return
     */
    @GetMapping("/currentLiveState/{cid}")
    public BayrolMainDisplayValues getCurrentLiveState(@PathVariable("cid") String cid)
    {
        return bayrolBridge.updateAndGetState(cid);
    }

}
