package com.pragmatix.app.services.rating;

import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.pvp.BattleWager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.05.2016 14:42
 */
public class DailyRatingData {
    public final BattleWager battleWager;
    /**
     * топ MAX_TOP игроков в мапе по ID шникам
     */
    public Map<Long, RatingProfileStructure> dailyTopMap = new ConcurrentHashMap<>();

    /**
     * история позиции в дневном топе, с шагом 5 мин. в течении 30 мин.
     */
    public Map<Long, short[]> dailyProgressMap = new ConcurrentHashMap<>();

    /**
     * топ MAX_TOP игроков за сегодня упорядоченых по рейтингу в порядке убывания
     */
    public NavigableSet<RatingProfileStructure> dailyTop = new ConcurrentSkipListSet<>(new UserProfileByRatingPointsComparator());

    public int lastDailyRating = 0;

    DailyRatingData(BattleWager battleWager) {
        this.battleWager = battleWager;
    }
}
