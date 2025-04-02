package com.pragmatix.clanserver.utils;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Author: Vladimir
 * Date: 23.12.11 13:34
 */
public class Utils {
    public static final int SECONDS_IN_HOUR = 3600;
    public static final int MILLISECONDS_IN_HOUR = 1000 * SECONDS_IN_HOUR;

    public static final int SECONDS_IN_DAY = 24 * SECONDS_IN_HOUR;
    public static final int MILLISECONDS_IN_DAY = 1000 * SECONDS_IN_DAY;

    public static final Date[] SECONDS_DATE_RANGE = new Date[] {
            new Date(0),
            new Date(1000L * Integer.MAX_VALUE),
    };
    public static final long[] SECONDS_DATE_RANGE_MILLIS = new long[] {
            SECONDS_DATE_RANGE[0].getTime(),
            SECONDS_DATE_RANGE[1].getTime()
    };
    public static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

    public static int currentTimeInSeconds() {
        return toSeconds(System.currentTimeMillis());
    }

    public static int toSeconds(Date date) {
        return date != null ? toSeconds(date.getTime()) : 0;
    }

    public static int toSeconds(long time) {
        if (time < SECONDS_DATE_RANGE_MILLIS[0] || time > SECONDS_DATE_RANGE_MILLIS[1]) {
            throwSecondsRangeException(new Date(time));
        }

        return (int) Math.round(((double) time) / 1000);
    }

    private static void throwSecondsRangeException(Date date) {
        throw new IllegalArgumentException(MessageFormat.format(
                "Дата ''{0,date,yyyy-MM-dd HH:mm:ss z}'' вне допустимых значений между ''{1,date,yyyy-MM-dd HH:mm:ss z}'' и ''{2,date,yyyy-MM-dd HH:mm:ss z}''",
                date, SECONDS_DATE_RANGE[0], SECONDS_DATE_RANGE[1]
        ));
    }

    public static Date toDate(int seconds) {
        return seconds > 0 ? new Date(1000L * seconds) : null;
    }

    public static Date toDate(long time) {
        return time > 0 ? new Date(time) : null;
    }
}
