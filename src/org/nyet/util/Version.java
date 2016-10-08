package org.nyet.util;

import java.util.ArrayList;

public class Version extends ArrayList<Object> {
    private static final long serialVersionUID = 20150620L;
    public static final String ECUxPlot = "v0.10r0.0";
    public static final String JFreeChart = "1.0.19";
    public static final String JCommon = "1.0.23";
    public static final String OpenCSV = "3.8";
    public static final String CommonsLang3 = "3.4";

    public Version() {
        this.add("ECUxPlot " + Version.ECUxPlot);
        this.add("JFreeChart " + Version.JFreeChart);
        this.add("JCommon " + Version.JCommon);
        this.add("OpenCSV " + Version.OpenCSV);
        this.add("commons-lang3 " + Version.CommonsLang3);
        this.add("Java runtime " + System.getProperty("java.version"));
    }

    public String toString() {
        String s = Strings.join("<br>", this.toArray());
        return "<html>" + s + "</html>";
    }
}
