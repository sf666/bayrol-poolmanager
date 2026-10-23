package de.sf666.bayrol.export;

import de.sf666.bayrol.domain.BayrolMainDisplayValues;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrometheusLightTest {
    @Test
    void exportsLightWithoutTurningUnknownIntoOff() {
        BayrolMainDisplayValues values = new BayrolMainDisplayValues();
        PrometheusExporter exporter = new PrometheusExporter();
        String[][] cases = {{"0", "1", "1"}, {"1", "0", "1"}, {"", "NaN", "0"}, {"11", "NaN", "0"}};
        for (String[] c : cases) {
            values.lightState = c[0];
            String metrics = exporter.formatMetrics(values, "12799");
            assertTrue(metrics.contains("BAYROL_POOL_LIGHT{pool_id=\"12799\"} " + c[1] + "\n"));
            assertTrue(metrics.contains("BAYROL_POOL_LIGHT_KNOWN{pool_id=\"12799\"} " + c[2] + "\n"));
            assertTrue(metrics.contains("BAYROL_POOL_PH{pool_id=\"12799\"}"));
        }
    }
}
