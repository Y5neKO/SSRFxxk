package com.y5neko.burpext.core;

public class Config {
    public static final String VERSION = "0.0.1";
    public static final String EXTENSION_NAME = "SSRFxxk v" + VERSION;

    public static final String LOGO = "  ___   ___   ___   ___               _   \n" +
            " / __| / __| | _ \\ | __| __ __ __ __ | |__\n" +
            " \\__ \\ \\__ \\ |   / | _|  \\ \\ / \\ \\ / | / /\n" +
            " |___/ |___/ |_|_\\ |_|   /_\\_\\ /_\\_\\ |_\\_\\\n" +
            "                                      v" + VERSION;

    public final static String userHome = System.getProperty("user.home");
    public final static String configDirPath = userHome + "/.ssrfxxk/";
    public final static String configFilePath = configDirPath + "config.json";
    public final static String DB_URL = "jdbc:sqlite:" + configDirPath + "data.db";
}