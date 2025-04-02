package com.pragmatix.pvp.services;

import com.google.gson.Gson;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.server.Server;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

import static com.pragmatix.pvp.BattleWager.*;
import static com.pragmatix.pvp.services.ExtraBattlesTimetableService.BattlesMode.DUEL_20;
import static com.pragmatix.pvp.services.ExtraBattlesTimetableService.BattlesMode.ROPE_RACE;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.04.2017 15:09
 */
@Service
public class ExtraBattlesTimetableService {

    private Map<BattleWager, Tuple2<LocalDate, LocalDate>> timetable = new LinkedHashMap<>();

    enum BattlesMode {
        ROPE_RACE(FRIEND_ROPE_RACE, ROPE_RACE_QUEST, FRIEND_ROPE_RACE_QUEST),
        DUEL_20(FRIEND_20_DUEL, WAGER_20_DUEL),
        MERCENARIES_DUEL(BattleWager.MERCENARIES_DUEL),;

        final Set<BattleWager> wagers;

        BattlesMode(BattleWager... wagers) {
            this.wagers = new HashSet<>(Arrays.asList(wagers));
        }
    }

    @Value("#{extraBattlesTimetable}")
    public void setTimetable(Map<String, String> timetable) {
        for(Map.Entry<String, String> entry : timetable.entrySet()) {
            BattlesMode battlesMode;
            try {
                battlesMode = BattlesMode.valueOf(entry.getKey());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Не зарегистрированный режим боя " + entry.getKey());
            }

            LocalDate from = LocalDate.parse("1970-01-01");
            LocalDate to = LocalDate.parse("1970-01-02");
            String[] period = entry.getValue().split(",");
            if(period.length == 2) {
                from = LocalDate.parse(period[0]);
                to = LocalDate.parse(period[1]);
            }

            if(from.isAfter(to))
                throw new IllegalStateException("Не корректный диапазон " + Tuple.of(from, to) + " для режима боя " + entry.getKey());

            for(BattleWager wager : battlesMode.wagers) {
                this.timetable.put(wager, Tuple.of(from, to));
            }
        }
        Server.sysLog.info("Расписание для дополнительных режимов боя:");
        for(BattlesMode battlesMode : BattlesMode.values()) {
            Tuple2<LocalDate, LocalDate> period = findPeriod(battlesMode);
            Server.sysLog.info("\t" + battlesMode + "=" + period);
        }
    }

    public boolean validateBattle(BattleWager battleWager) {
        Tuple2<LocalDate, LocalDate> period = timetable.get(battleWager);
        return period == null || (LocalDate.now().isAfter(period._1.minusDays(1)) && LocalDate.now().isBefore(period._2.plusDays(1)));
    }

    public void setModePeriod(BattlesMode key, String period) {
        setTimetable(new HashMap<String, String>() {{
            put(key.name(), period);
        }});
    }

    public String getTimetable() {
        String s = "\n";
        for(BattlesMode battlesMode : BattlesMode.values()) {
            s += battlesMode + "=" + printPeriod(battlesMode) + "\n";
        }
        return s;
    }

    private Tuple2<LocalDate, LocalDate> findPeriod(BattlesMode battlesMode) {
        return this.timetable.get(battlesMode.wagers.iterator().next());
    }

    public String printTimetableAsJson() {
        Map<String, String> result = new LinkedHashMap<>();
        for(BattlesMode battlesMode : BattlesMode.values()) {
            Tuple2<LocalDate, LocalDate> period = findPeriod(battlesMode);
            if(period != null) {
                result.put(battlesMode.name(), period._1 + "," + period._2);
            }
        }
        return new Gson().toJson(result);
    }


    private String printPeriod(BattlesMode mode) {
        Tuple2<LocalDate, LocalDate> period = findPeriod(mode);
        return period._1+","+period._2;
    }

    public String getROPE_RACE() {
        return printPeriod(ROPE_RACE);
    }

    public void setROPE_RACE(String period) {
        setModePeriod(ROPE_RACE, period);
    }

    public String getDUEL_20() {
        return printPeriod(DUEL_20);
    }

    public void setDUEL_20(String period) {
        setModePeriod(DUEL_20, period);
    }

    public String getMERCENARIES_DUEL() {
        return printPeriod(BattlesMode.MERCENARIES_DUEL);
    }

    public void setMERCENARIES_DUEL(String period) {
        setModePeriod(BattlesMode.MERCENARIES_DUEL, period);
    }

}
