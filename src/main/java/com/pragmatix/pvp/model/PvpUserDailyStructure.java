package com.pragmatix.pvp.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 02.07.13 12:17
 */
public class PvpUserDailyStructure {
    /**
     * подобранные противники(партнеры) в течении дня
     */
    private Map<Long, Integer> opponents = new ConcurrentHashMap<>();
    /**
     * консервативный рейтинг на начало дня
     */
    public int trueSkillRating;

    public Integer getBattlesCountWith(Long opponentId) {
        Integer battles = opponents.get(opponentId);
        return battles != null ? battles : 0;
    }

    public int incBattlesCountWith(Long opponentId) {
        int result;
        Integer battles = opponents.get(opponentId);
        if(battles != null) {
            result = battles + 1;
        } else {
            result = 1;
        }
        opponents.put(opponentId, result);
        return result;
    }

    @Override
    public String toString() {
        return "PvpUserDailyStructure{" +
                "opponents=" + opponents +
                '}';
    }

    public Map<Long, Integer> getOpponents() {
        return opponents;
    }
}
