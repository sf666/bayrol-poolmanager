package de.sf666.bayrol.export;

import org.springframework.stereotype.Service;

import de.sf666.bayrol.domain.BayrolMainDisplayValues;

/**
 * Factory for Exporter
 */
@Service
public class ExportFactory
{

    public ExportFactory()
    {
    }

    public IMetricExporter lookupExporter(String exporterFormat)
    {
        if (exporterFormat == null)
            throw new RuntimeException("exporterFormat shouln't be null");

        if ("Prometheus".equalsIgnoreCase(exporterFormat))
        {
            return new PrometheusExporter();
        }
        throw new RuntimeException("Unknown exporter : " + exporterFormat);
    }
}
