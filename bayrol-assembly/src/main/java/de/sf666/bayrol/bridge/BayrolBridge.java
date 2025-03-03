package de.sf666.bayrol.bridge;

import java.io.UnsupportedEncodingException;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import de.sf666.bayrol.api.BayrolHttpConnector;
import de.sf666.bayrol.domain.BayrolMainDisplayValues;

public class BayrolBridge
{
    private static final Logger log = LoggerFactory.getLogger(BayrolBridge.class.getName());

    private BayrolHttpConnector bayrol = null;

    @Autowired
    public BayrolBridge(BayrolHttpConnector bayrol)
    {
        this.bayrol = bayrol;
        init();
    }

    @Scheduled(fixedRate = 120000)
    private void doIt()
    {
        bayrol.updateAllStates();
    }

    private void init()
    {
        try
        {
            bayrol.connectToWebPortal();
        }
        catch (Exception e)
        {
            log.warn("cannot connect to web portal : ", e);
        }
    }

    public BayrolMainDisplayValues getCurrentState(String cid)
    {
        return bayrol.getCurrentState(cid);
    }

    public void updateAllStates()
    {
        bayrol.updateAllStates();
    }

    public BayrolMainDisplayValues updateAndGetState(String cid)
    {
        return bayrol.updateAndGetState(cid);
    }

    public void connectToWebPortal() throws UnsupportedEncodingException
    {
        bayrol.connectToWebPortal();
    }

    public Set<String> getPlantCids()
    {
        return bayrol.getPlantCids();
    }
    
    public void lightOn(String cid) {
    	bayrol.lightOn(cid);
    }

    public void lightOff(String cid) {
    	bayrol.lightOff(cid);
    }
}
