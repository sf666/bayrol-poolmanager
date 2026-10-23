package de.sf666.bayrol.api;

import de.sf666.bayrol.domain.BayrolLightState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BayrolLightStateTest {
    @Test
    void readsActualIndicatorRatherThanOperatingMode() {
        assertEquals(BayrolLightState.ON, BayrolLightState.fromStatus(BayrolHttpConnector.extractLightState("{\"55.17101.status\":\"0\"}")));
        assertEquals(BayrolLightState.OFF, BayrolLightState.fromStatus(BayrolHttpConnector.extractLightState("{\"55.17101.status\":\"1\"}")));
        for (String raw : new String[]{"11", "10", "4", "2", "", " 1", "1foo", null}) {
            assertEquals(BayrolLightState.UNKNOWN, BayrolLightState.fromStatus(raw));
        }
        assertEquals("11", BayrolHttpConnector.extractLightState("{\"55.17101.status\":\"11\"}"));
        assertEquals("", BayrolHttpConnector.extractLightState("{\"34.4001.value\":\"7.2\"}"));
        assertEquals("", BayrolHttpConnector.extractLightState("<html>Login</html>"));
        assertEquals("", BayrolHttpConnector.extractLightState(null));
        assertEquals("1", BayrolHttpConnector.extractLightState("{\"55.17101.status\":1}"));
        assertEquals("", BayrolHttpConnector.extractLightState("{\"60.5433.value\":\"1\"}"));
        assertEquals("1", BayrolHttpConnector.extractLightState("{\"60.5433.value\":\"1\",\"55.17101.status\":\"1\"}"));
    }
}
