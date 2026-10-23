package de.sf666.bayrol.domain;

/** Actual home-screen light indicator (55.17101.status), not operating mode. */
public enum BayrolLightState {
    ON("1"), OFF("0"), UNKNOWN("NaN");

    private final String metricValue;

    BayrolLightState(String metricValue) {
        this.metricValue = metricValue;
    }

    public String metricValue() {
        return metricValue;
    }

    public static BayrolLightState fromStatus(String raw) {
        if ("0".equals(raw)) {
            return ON;
        }
        if ("1".equals(raw)) {
            return OFF;
        }
        return UNKNOWN;
    }
}
