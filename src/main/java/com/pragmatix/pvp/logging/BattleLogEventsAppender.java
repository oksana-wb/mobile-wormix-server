package com.pragmatix.pvp.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterAttachableImpl;
import ch.qos.logback.core.spi.FilterReply;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.services.battletracking.PvpBattleTrackerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.12.12 16:05
 */
@Component
public class BattleLogEventsAppender extends ContextAwareBase implements Appender<ILoggingEvent> {

    @Resource
    private PvpBattleTrackerService pvpBattleTrackerService;

    @Value("${debug.dumpAllBattles:false}")
    private boolean debugDumpAllBattles = false;

    protected boolean started = false;

    /**
     * Appenders are named.
     */
    protected String name;

    private FilterAttachableImpl<ILoggingEvent> fai = new FilterAttachableImpl<ILoggingEvent>();

    private List<Appender<ILoggingEvent>> pvpLogbackLoggerAppenders;
    private Level pvpLogbackLoggerLevel;

    @Override
    public String getName() {
        return name;
    }

    private final ConcurrentMap<Long, List<BattleLogEvent>> logEventsByBattles = new ConcurrentHashMap<Long, List<BattleLogEvent>>();

    @Override
    public void doAppend(ILoggingEvent event) throws LogbackException {
        if(!started) {
            return;
        }

        if(event.getLevel().isGreaterOrEqual(pvpLogbackLoggerLevel)) {
            if(pvpBattleTrackerService != null) {
                for(Appender<ILoggingEvent> pvpLogbackLoggerAppender : pvpLogbackLoggerAppenders) {
                    pvpLogbackLoggerAppender.doAppend(event);
                }
            }
        }

        String message = event.getFormattedMessage();
        if(message.contains("battleId=") && !message.contains("PvpAction")) {
            int start = message.indexOf("battleId=") + 9;
            int end = start;
            while (Character.isDigit(message.charAt(end))) {
                end++;
            }
            String bid = message.substring(start, end);
            long battleId = 0;
            if(!bid.isEmpty()) {
                battleId = Long.parseLong(message.substring(start, end));
            }
            if(battleId > 0) {
                BattleBuffer battle = pvpBattleTrackerService.getBattle(battleId);
                // логгиркем только ход боя
                if(battle != null && battle.isStarted() && !battle.isFinished()) {
                    PvpBattleType battleType = battle.getBattleType();
                    // логгируем только проблемные бои
                    if(debugDumpAllBattles
                            || battleType == PvpBattleType.WAGER_PvP_3_FOR_ALL
                            ) {
                        List<BattleLogEvent> battleLogEvents = logEventsByBattles.get(battleId);
                        if(battleLogEvents == null) {
                            List<BattleLogEvent> newValue = new CopyOnWriteArrayList<>();
                            List<BattleLogEvent> oldValue = logEventsByBattles.putIfAbsent(battleId, newValue);
                            battleLogEvents = oldValue != null ? oldValue : newValue;
                        }
                        BattleLogEvent battleLogEvent = new BattleLogEvent(event.getThreadName(), event.getTimeStamp(), message);
                        battleLogEvents.add(battleLogEvent);
                    }
                }
            }
        }
    }

    /**
     * Set the name of this appender.
     */
    public void setName(String name) {
        this.name = name;
    }

    public void start() {
        started = true;
    }

    public void stop() {
        started = false;
        logEventsByBattles.clear();
    }

    public boolean isStarted() {
        return started;
    }

    public String toString() {
        return this.getClass().getName() + "[" + name + "]";
    }

    public void addFilter(Filter<ILoggingEvent> newFilter) {
        fai.addFilter(newFilter);
    }

    public void clearAllFilters() {
        fai.clearAllFilters();
    }

    public List<Filter<ILoggingEvent>> getCopyOfAttachedFiltersList() {
        return fai.getCopyOfAttachedFiltersList();
    }

    public FilterReply getFilterChainDecision(ILoggingEvent event) {
        return fai.getFilterChainDecision(event);
    }

    public ConcurrentMap<Long, List<BattleLogEvent>> getLogEventsByBattles() {
        return logEventsByBattles;
    }

    public void setPvpLogbackLoggerAppenders(List<Appender<ILoggingEvent>> pvpLogbackLoggerAppenders) {
        this.pvpLogbackLoggerAppenders = pvpLogbackLoggerAppenders;
    }

    public void setPvpLogbackLoggerLevel(Level pvpLogbackLoggerLevel) {
        this.pvpLogbackLoggerLevel = pvpLogbackLoggerLevel;
    }
}
