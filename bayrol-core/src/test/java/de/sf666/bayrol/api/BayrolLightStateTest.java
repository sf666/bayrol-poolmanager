package de.sf666.bayrol.api;

import de.sf666.bayrol.domain.BayrolLightState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BayrolLightStateTest {
    @Test
    void readsFullLightModeAndRejectsAmbiguousModes() {
        assertEquals(BayrolLightState.ON, BayrolLightState.fromRaw(BayrolHttpConnector.extractLightState("{\"60.5433.value\":\"1\"}")));
        assertEquals(BayrolLightState.OFF, BayrolLightState.fromRaw(BayrolHttpConnector.extractLightState("{\"60.5433.value\":\"4\"}")));
        for (String raw : new String[]{"11", "10", "0", "2", "", " 1", "1foo", null}) {
            assertEquals(BayrolLightState.UNKNOWN, BayrolLightState.fromRaw(raw));
        }
        assertEquals("11", BayrolHttpConnector.extractLightState("{\"60.5433.value\":\"11\"}"));
        assertEquals("", BayrolHttpConnector.extractLightState("{\"34.4001.value\":\"7.2\"}"));
        assertEquals("", BayrolHttpConnector.extractLightState("<html>Login</html>"));
        assertEquals("", BayrolHttpConnector.extractLightState(null));
    }
}
