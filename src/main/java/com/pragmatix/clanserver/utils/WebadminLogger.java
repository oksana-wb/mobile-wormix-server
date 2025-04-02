package com.pragmatix.clanserver.utils;

import ch.qos.logback.classic.spi.ThrowableProxy;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 14.10.13 16:42
 */
public class WebadminLogger implements Logger {

    private final Logger log;
    private final StringBuilder sb;

    public WebadminLogger(Logger logger, StringBuilder sb) {
        log = logger;
        this.sb = sb;
    }

    protected void append(final String level, String s, Object ... params) {
        FormattingTuple formattingTuple = MessageFormatter.arrayFormat(s, params);
        sb.append("[").append(level).append("] ").append(formattingTuple.getMessage()).append("\n");
    }

    protected void appendThrowable(final String level, String s, Throwable throwable) {
        sb.append("[").append(level).append("] ").append(s).append("\n");
    }

    public static void main(String[] args) {
        ThrowableProxy throwableProxy = new ThrowableProxy(new Exception("some!"));
        System.out.println(Arrays.toString(throwableProxy.getStackTraceElementProxyArray()));
    }


    @Override
    public String getName() {
        return "WebadminLogger";
    }

    @Override
    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        log.trace(s);
        append("TRACE", s);
    }

    @Override
    public void trace(String s, Object o) {
        log.trace(s, o);
        append("TRACE", s, o);
    }

    @Override
    public void trace(String s, Object o, Object o2) {
        log.trace(s, o, o2);
        append("TRACE", s, o, o2);
    }

    @Override
    public void trace(String s, Object[] objects) {
        log.trace(s, objects);
        append("TRACE", s, objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        log.trace(s, throwable);
        appendThrowable("TRACE", s, throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return log.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String s) {
        log.trace(marker, s);
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        log.trace(marker, s, o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o2) {
        log.trace(marker, s, o, o2);
    }

    @Override
    public void trace(Marker marker, String s, Object[] objects) {
        log.trace(marker, s, objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        log.trace(marker, s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        log.debug(s);
        append("DEBUG", s);
    }

    @Override
    public void debug(String s, Object o) {
        log.debug(s, o);
        append("DEBUG", s, o);
    }

    @Override
    public void debug(String s, Object o, Object o2) {
        log.debug(s, o, o2);
        append("DEBUG", s, o, o2);
    }

    @Override
    public void debug(String s, Object[] objects) {
        log.debug(s, objects);
        append("DEBUG", s, objects);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        log.debug(s, throwable);
        appendThrowable("DEBUG", s, throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return log.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        log.debug(marker, s);
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        log.debug(marker, s, o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o2) {
        log.debug(marker, s, o, o2);
    }

    @Override
    public void debug(Marker marker, String s, Object[] objects) {
        log.debug(marker, s, objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        log.debug(marker, s, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        log.info(s);
        append("INFO", s);
    }

    @Override
    public void info(String s, Object o) {
        log.info(s, o);
        append("INFO", s, o);
    }

    @Override
    public void info(String s, Object o, Object o2) {
        log.info(s, o, o2);
        append("INFO", s, o, o2);
    }

    @Override
    public void info(String s, Object[] objects) {
        log.info(s, objects);
        append("INFO", s, objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        log.info(s, throwable);
        appendThrowable("INFO", s, throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return log.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        log.info(marker, s);
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        log.info(marker, s, o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o2) {
        log.info(marker, s, o, o2);
    }

    @Override
    public void info(Marker marker, String s, Object[] objects) {
        log.info(marker, s, objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        log.info(marker, s, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        log.warn(s);
        append("WARN", s);
    }

    @Override
    public void warn(String s, Object o) {
        log.warn(s, o);
        append("WARN", s, o);
    }

    @Override
    public void warn(String s, Object[] objects) {
        log.warn(s, objects);
        append("WARN", s, objects);
    }

    @Override
    public void warn(String s, Object o, Object o2) {
        log.warn(s, o, o2);
        append("WARN", s, o, o2);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        log.warn(s, throwable);
        appendThrowable("WARN", s, throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return log.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        log.warn(marker, s);
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        log.warn(marker, s, o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o2) {
        log.warn(marker, s, o, o2);
    }

    @Override
    public void warn(Marker marker, String s, Object[] objects) {
        log.warn(marker, s, objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        log.warn(marker, s, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        log.error(s);
        append("ERROR", s);
    }

    @Override
    public void error(String s, Object o) {
        log.error(s, o);
        append("ERROR", s, o);
    }

    @Override
    public void error(String s, Object o, Object o2) {
        log.error(s, o, o2);
        append("ERROR", s, o, o2);
    }

    @Override
    public void error(String s, Object[] objects) {
        log.error(s, objects);
        append("ERROR", s, objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        log.error(s, throwable);
        appendThrowable("ERROR", s, throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return log.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        log.error(marker, s);
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        log.error(marker, s, o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o2) {
        log.error(marker, s, o, o2);
    }

    @Override
    public void error(Marker marker, String s, Object[] objects) {
        log.error(marker, s, objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        log.error(marker, s, throwable);
    }
}
