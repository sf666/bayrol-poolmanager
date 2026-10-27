package de.sf666.bayrol.api;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OutputRequestTest {
    private String payload(String name) throws Exception {
        Field field = BayrolHttpConnector.class.getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(null);
    }
    @Test void outputIndicatorsAreRequestedSeparatelyFromMeasurements() throws Exception {
        String measurements = payload("TEMP_CL_PH_PAYLOAD");
        assertTrue(measurements.contains("34.4001.value"));
        assertFalse(measurements.contains("55."));
        String outputs = payload("OUTPUT_STATUS_PAYLOAD");
        assertFalse(outputs.contains("34."));
        assertTrue(outputs.contains("55.17106.status"));
        assertTrue(outputs.contains("55.17101.status"));
        String response = "{\"data\":{\"55.17101.status\":\"0\",\"55.17106.status\":\"1\"},\"status\":{\"code\":0}}";
        assertEquals("0", BayrolHttpConnector.extractLightState(response));
        assertEquals("1", BayrolHttpConnector.extractFilterPumpState(response));
        assertEquals("", BayrolHttpConnector.extractFilterPumpState("{\"55.17106.opmode\":\"0\"}"));
    }
}
