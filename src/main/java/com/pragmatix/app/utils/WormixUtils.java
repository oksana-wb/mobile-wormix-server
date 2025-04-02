package com.pragmatix.app.utils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class WormixUtils {

    public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static DateTimeFormatter instantFormatter = dateTimeFormatter.withZone(ZoneOffset.UTC);
    public static DateTimeFormatter instantLocalFormatter = dateTimeFormatter.withZone(ZoneOffset.systemDefault());
    
}
