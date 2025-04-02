package com.pragmatix.app.services.rating;

import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.pvp.BattleWager;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.05.2016 14:42
 */
class YesterdayRatingData {
    public final BattleWager battleWager;

    public Map<Long, Integer> yesterdayRating = new HashMap<>();

    /**
     * топ MAX_TOP играков за вчера
     */
    public NavigableSet<RatingProfileStructure> yesterdayTop = new ConcurrentSkipListSet<>(new UserProfileByRatingPointsComparator());

    /**
     * PROGRESS_MAX позиций в топе за вчера
     */
    public Map<Long, Integer> yesterdayPositions = new HashMap<>();

    YesterdayRatingData(BattleWager battleWager) {
        this.battleWager = battleWager;
    }
}
