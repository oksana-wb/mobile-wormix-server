package com.pragmatix.testcase;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AbstractTest {

    protected static long testerProfileId = 58027749L;

    protected void printHeapInfo() {
        // Get current size of heap in bytes
        long heapSize = Runtime.getRuntime().totalMemory();
        // Get maximum size of heap in bytes. The heap cannot grow beyond this size.
        // Any attempt will result in an OutOfMemoryException.
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        // Get amount of free memory within the heap in bytes. This size will increase
        // after garbage collection and decrease as new objects are created.
        long heapFreeSize = Runtime.getRuntime().freeMemory();

        System.out.printf("heapSize = %sM, heapMaxSize = %sM, heapFreeSize = %sM\n", heapSize / 1024 / 1024, heapMaxSize / 1024 / 1024, heapFreeSize / 1024 / 1024);
    }

    protected void println(Object o) {
        System.out.println(o);
    }

    public static void main(String[] args) throws Exception {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("EVENTS_CDR_LOGGER");
        RollingFileAppender eventsCdrLogAppender = (RollingFileAppender)log.getAppender("eventsCdrLogAppender");
        TimeBasedRollingPolicy rollingPolicy = (TimeBasedRollingPolicy)eventsCdrLogAppender.getRollingPolicy();
        rollingPolicy.setMaxHistory(65);
        System.out.println(rollingPolicy.getMaxHistory());
    }

}
