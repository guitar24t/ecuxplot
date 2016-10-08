package org.nyet.util;

import java.util.ArrayList;

public class Version extends ArrayList<Object> {
    private static final long serialVersionUID = 20150620L;
    public static final String ECUxPlot="%ECUXPLOT_VER";
    public static final String JFreeChart="%JFREECHART_VER";
    public static final String JCommon="%JCOMMON_VER";
    public static final String OpenCSV="%OPENCSV_VER";
    public static final String JavacMajor = "%JAVAC_MAJOR_VER";
    public static final String CommonsLang3 = "%COMMONS_LANG3_VER";

    public Version () {
	this.add("ECUxPlot " + Version.ECUxPlot);
	this.add("JFreeChart " + Version.JFreeChart);
	this.add("JCommon " + Version.JCommon);
	this.add("OpenCSV " + Version.OpenCSV);
	this.add("commons-lang3 " + Version.CommonsLang3);
	this.add("Java compiler " + Version.JavacMajor);
	this.add("Java runtime "+ System.getProperty("java.version"));
    }

    public String toString() {
	String s = Strings.join("<br>", this.toArray());
	return "<html>"+s+"</html>";
    }
}
