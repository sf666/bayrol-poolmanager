package de.sf666.bayrol.export;
import de.sf666.bayrol.domain.BayrolMainDisplayValues;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PrometheusFilterPumpTest {
    @Test void staleOrUnknownPumpNeverLooksRunning() {
        BayrolMainDisplayValues v = new BayrolMainDisplayValues();
        PrometheusExporter e = new PrometheusExporter();
        String[][] cases = {{"0","1","1"},{"1","0","1"},{"","NaN","0"},{"11","NaN","0"}};
        for (String[] c : cases) {
            v.date = Instant.now().toString(); v.filterPumpState = c[0];
            String m = e.formatMetrics(v,"12799");
            assertTrue(m.contains("BAYROL_POOL_FILTER_PUMP{pool_id=\"12799\"} " + c[1] + "\n"));
            assertTrue(m.contains("BAYROL_POOL_FILTER_PUMP_KNOWN{pool_id=\"12799\"} " + c[2] + "\n"));
        }
        v.filterPumpState="0";
        for (String date : new String[]{"",null,Instant.now().minusSeconds(301).toString(),Instant.now().plusSeconds(60).toString()}) {
            v.date=date;
            assertTrue(e.formatMetrics(v,"12799").contains("BAYROL_POOL_FILTER_PUMP{pool_id=\"12799\"} NaN\n"));
        }
    }
}
