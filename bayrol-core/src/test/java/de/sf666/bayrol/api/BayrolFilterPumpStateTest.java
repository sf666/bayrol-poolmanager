package de.sf666.bayrol.api;

import de.sf666.bayrol.domain.BayrolFilterPumpState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BayrolFilterPumpStateTest {
    @Test
    void readsActualIndicatorRatherThanOperatingMode() {
        assertEquals(BayrolFilterPumpState.ON, BayrolFilterPumpState.fromStatus(BayrolHttpConnector.extractFilterPumpState("{\"55.17106.status\":\"0\"}")));
        assertEquals(BayrolFilterPumpState.OFF, BayrolFilterPumpState.fromStatus(BayrolHttpConnector.extractFilterPumpState("{\"55.17106.status\":\"1\"}")));
        for (String raw : new String[]{"11", "10", "4", "2", "", " 1", "1foo", null}) {
            assertEquals(BayrolFilterPumpState.UNKNOWN, BayrolFilterPumpState.fromStatus(raw));
        }
        assertEquals("11", BayrolHttpConnector.extractFilterPumpState("{\"55.17106.status\":\"11\"}"));
        assertEquals("", BayrolHttpConnector.extractFilterPumpState("{\"34.4001.value\":\"7.2\"}"));
        assertEquals("", BayrolHttpConnector.extractFilterPumpState("<html>Login</html>"));
        assertEquals("", BayrolHttpConnector.extractFilterPumpState(null));
        assertEquals("1", BayrolHttpConnector.extractFilterPumpState("{\"55.17106.status\":1}"));
        assertEquals("", BayrolHttpConnector.extractFilterPumpState("{\"60.5433.value\":\"1\"}"));
        assertEquals("1", BayrolHttpConnector.extractFilterPumpState("{\"60.5433.value\":\"1\",\"55.17106.status\":\"1\"}"));
    }
}
