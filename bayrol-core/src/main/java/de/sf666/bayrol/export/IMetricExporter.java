package de.sf666.bayrol.export;

import de.sf666.bayrol.domain.BayrolMainDisplayValues;

public interface IMetricExporter
{
    public String formatMetrics(BayrolMainDisplayValues value, String poolId);
}
