package com.y5neko.burpext.detector;

public interface Detector {
    String fullDomain = "";
    String mainDomain = "";
    String subDomain = "";
    String token = "";
    String result = "";

    String getFullDomain();

    String getResult() throws Exception;
}
