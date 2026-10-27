package de.sf666.bayrol.export;

import de.sf666.bayrol.domain.BayrolMainDisplayValues;
import de.sf666.bayrol.domain.BayrolLightState;
import de.sf666.bayrol.domain.BayrolFilterPumpState;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Format domain data to Prometheus
 */

public class PrometheusExporter implements IMetricExporter
{
    private static final String prefix = "BAYROL_POOL_";
    private static final String temp = "TEMP";
    private static final String cl = "CL";
    private static final String ph = "PH";
    private static final String nl = "\n";

    public PrometheusExporter()
    {
    }

    @Override
    public String formatMetrics(BayrolMainDisplayValues value, String poolId)
    {
        String pool = String.format("{pool_id=\"%s\"} ", poolId);
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(temp).append(pool).append(value.temp).append(nl);
        sb.append(prefix).append(cl).append(pool).append(value.cl).append(nl);
        sb.append(prefix).append(ph).append(pool).append(value.ph).append(nl);
        BayrolLightState light = BayrolLightState.fromStatus(value.lightState);
        sb.append(prefix).append("LIGHT").append(pool).append(light.metricValue()).append(nl);
        sb.append(prefix).append("LIGHT_KNOWN").append(pool).append(light == BayrolLightState.UNKNOWN ? 0 : 1).append(nl);
        double sampleTime = sampleTimestamp(value.date);
        double age = Instant.now().toEpochMilli() / 1000.0 - sampleTime;
        boolean fresh = Double.isFinite(sampleTime) && age >= -30 && age <= 300;
        BayrolFilterPumpState pump = fresh ? BayrolFilterPumpState.fromStatus(value.filterPumpState) : BayrolFilterPumpState.UNKNOWN;
        sb.append(prefix).append("FILTER_PUMP").append(pool).append(pump.metricValue()).append(nl);
        sb.append(prefix).append("FILTER_PUMP_KNOWN").append(pool).append(pump == BayrolFilterPumpState.UNKNOWN ? 0 : 1).append(nl);
        sb.append(prefix).append("SAMPLE_TIMESTAMP_SECONDS").append(pool).append(sampleTime).append(nl);
        return sb.toString();
    }

    private static double sampleTimestamp(String date) {
        try {
            return OffsetDateTime.parse(date).toInstant().toEpochMilli() / 1000.0;
        } catch (DateTimeParseException | NullPointerException e) {
            return Double.NaN;
        }
    }
}
