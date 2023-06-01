package de.sf666.bayrol.export;

import de.sf666.bayrol.domain.BayrolMainDisplayValues;

/**
 * Format domain data to Prometheus
 */

public class PrometheusExporter implements IMetricExporter
{
    private static final String prefix = "BAYROL_POOL_";
    private static final String temp = "TEMP";
    private static final String cl = "CL";
    private static final String ph = "PH";
    private static final String nl = "\r\n";

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
        return sb.toString();
    }

}
