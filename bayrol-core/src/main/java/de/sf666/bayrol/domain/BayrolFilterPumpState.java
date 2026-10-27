package de.sf666.bayrol.domain;

/** Actual home-screen filter-pump indicator (55.17106.status), not operating mode. */
public enum BayrolFilterPumpState {
    ON("1"), OFF("0"), UNKNOWN("NaN");

    private final String metricValue;

    BayrolFilterPumpState(String metricValue) {
        this.metricValue = metricValue;
    }

    public String metricValue() {
        return metricValue;
    }

    public static BayrolFilterPumpState fromStatus(String raw) {
        if ("0".equals(raw)) {
            return ON;
        }
        if ("1".equals(raw)) {
            return OFF;
        }
        return UNKNOWN;
    }
}
