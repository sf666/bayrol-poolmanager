package de.sf666.bayrol.domain;

/**
 * Values seen on the main display
 */
public class BayrolMainDisplayValues
{
    public String date = "";
    public double ph = 0;
    public double temp = 0;
    public double cl = 0;
    public String lightState = "";

    @Override
	public String toString() {
		return "BayrolMainDisplayValues [date=" + date + ", ph=" + ph + ", temp=" + temp + ", cl=" + cl + ", lightState=" + lightState +
			"]";
	}

}
