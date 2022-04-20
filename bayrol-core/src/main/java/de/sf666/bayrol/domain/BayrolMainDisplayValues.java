package de.sf666.bayrol.domain;

/**
 * Values seen on the main display
 */
public class BayrolMainDisplayValues
{
    public double ph = 0;
    public double temp = 0;
    public double cl = 0;

    @Override
    public String toString()
    {
        return "BayrolMainDisplayValues [ph=" + ph + ", temp=" + temp + ", cl=" + cl + "]";
    }

}
