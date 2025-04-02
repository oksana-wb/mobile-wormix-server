package com.pragmatix.pvp.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.threads.Execution;
import com.pragmatix.sessions.IClient;

import java.util.Date;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.12.12 16:31
 */
public class BattleLogEvent {

    public String threadName;
    public long timestamp;
    public String message;
    public String execution;

    public BattleLogEvent(String threadName, long timestamp, String message) {
        this.threadName = threadName;
        this.timestamp = timestamp;
        this.message = message;
        this.execution = formatExecution();
    }

    @Override
    public String toString() {
        Date date = new Date(timestamp);
        return String.format("%tH:%tM:%tS.%tL [%s] [%s] %s", date, date, date, date, threadName, execution, message);
    }

    public String formatExecution() {
        Execution execution = Execution.EXECUTION.get();
        if (execution == null) {
            return "NO EXECUTION";
        }
        Session session = execution.getSession();
        if (session == null) {
            return "NO SESSION";
        }
        IClient user = session.getUser();
        if (user == null) {
            return "NO USER";
        }
        return user.toString();
    }

}
