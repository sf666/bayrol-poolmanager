package de.sf666.bayrol.domain;

/** Explicit Bayrol light modes; unknown/automatic modes are not assumed off. */
public enum BayrolLightState {
    ON("1"), OFF("0"), UNKNOWN("NaN");

    private final String metricValue;

    BayrolLightState(String metricValue) {
        this.metricValue = metricValue;
    }

    public String metricValue() {
        return metricValue;
    }

    public static BayrolLightState fromRaw(String raw) {
        if ("1".equals(raw)) {
            return ON;
        }
        if ("4".equals(raw)) {
            return OFF;
        }
        return UNKNOWN;
    }
}
