package com.pragmatix.app.services;

import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.rating.RatingService;
import com.pragmatix.clan.structures.ClanMemberStructure;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.testcase.AbstractSpringTest;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created: 25.04.11 15:11
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class DailyRatingServiceTest extends AbstractSpringTest {

    @Resource
    private RatingService ratingService;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private SoftCache softCache;

//    @Test
//    public void testStoreDailyRatings() {
//        UserProfile profile1 = newProfile(1);
//        updateDailyTop(profile1, 1000, BattleWager.WAGER_15_DUEL);
//        updateDailyTop(profile1, 1002, BattleWager.WAGER_50_2x2);
//        updateDailyTop(profile1, 1004, BattleWager.WAGER_50_3_FOR_ALL);
//
//        assertEquals(1, getDailyTop(BattleWager.WAGER_15_DUEL).size());
//        assertEquals(1, getDailyTop(BattleWager.WAGER_50_2x2).size());
//        assertEquals(1, getDailyTop(BattleWager.WAGER_50_3_FOR_ALL).size());
//
//        for(BattleWager battleWager : BattleWager.values()) {
//            if(battleWager == BattleWager.WAGER_50_2x2_FRIENDS) {
//                assertEquals(0, getDailyTop(battleWager).size());
//            } else {
//                assertEquals(profile1.getId().longValue(), getDailyTop(battleWager).first().id);
//            }
//        }
//
//        assertEquals(1000, getDailyTop(BattleWager.WAGER_15_DUEL).first().ratingPoints);
//        assertEquals(1002 + 1003, getDailyTop(BattleWager.WAGER_50_2x2).first().ratingPoints);
//        assertEquals(0, getDailyTop(BattleWager.WAGER_50_2x2_FRIENDS).size());
//        assertEquals(1004, getDailyTop(BattleWager.WAGER_50_3_FOR_ALL).first().ratingPoints);
//
//        assertEquals(1000 + 1001 + 1002 + 1003 + 1004, getDailyTop(BattleWager.NO_WAGER).first().ratingPoints);
//
//        dailyRegistry.getDailyTask().runServiceTask();
//
//        // yesterday ratings
//        for(BattleWager battleWager : BattleWager.values()) {
//            if(battleWager == BattleWager.WAGER_50_2x2_FRIENDS) {
//                assertEquals(0, getYesterdayTop(battleWager).size());
//            } else {
//                assertEquals(profile1.getId().longValue(), getYesterdayTop(battleWager).first().id);
//            }
//
//            assertEquals(0, getDailyTop(battleWager).size());
//        }
//
//        assertEquals(1000, getYesterdayTop(BattleWager.WAGER_15_DUEL).first().ratingPoints);
//        assertEquals(1002 + 1003, getYesterdayTop(BattleWager.WAGER_50_2x2).first().ratingPoints);
//        assertEquals(0, getYesterdayTop(BattleWager.WAGER_50_2x2_FRIENDS).size());
//        assertEquals(1004, getYesterdayTop(BattleWager.WAGER_50_3_FOR_ALL).first().ratingPoints);
//
//        assertEquals(1000 + 1001 + 1002 + 1003 + 1004, getYesterdayTop(BattleWager.NO_WAGER).first().ratingPoints);
//
//        updateDailyTop(profile1, 2000, BattleWager.WAGER_15_DUEL);
//        updateDailyTop(profile1, 2002, BattleWager.WAGER_50_2x2);
//        updateDailyTop(profile1, 2003, BattleWager.WAGER_50_2x2_FRIENDS);
//        updateDailyTop(profile1, 2004, BattleWager.WAGER_50_3_FOR_ALL);
//
//        assertEquals(1, getDailyTop(BattleWager.WAGER_15_DUEL).size());
//        assertEquals(1, getDailyTop(BattleWager.WAGER_50_2x2).size());
//        assertEquals(0, getDailyTop(BattleWager.WAGER_50_2x2_FRIENDS).size());
//        assertEquals(1, getDailyTop(BattleWager.WAGER_50_3_FOR_ALL).size());
//
//        assertEquals(2000, getDailyTop(BattleWager.WAGER_15_DUEL).first().ratingPoints);
//        assertEquals(2002 + 2003, getDailyTop(BattleWager.WAGER_50_2x2).first().ratingPoints);
//        assertEquals(0, getDailyTop(BattleWager.WAGER_50_2x2_FRIENDS).size());
//        assertEquals(2004, getDailyTop(BattleWager.WAGER_50_3_FOR_ALL).first().ratingPoints);
//
//        assertEquals(2000 + 2001 + 2002 + 2003 + 2004, getDailyTop(BattleWager.NO_WAGER).first().ratingPoints);
//
//        ratingService.persistToDisk();
//    }

//    @Test
//    public void testRestoreDailyRatings() {
//        UserProfile profile1 = newProfile(1);
//
//        ratingService.initByRestoreState();
//
//        // yesterday ratings
//        for(BattleWager battleWager : BattleWager.values()) {
//            if(battleWager == BattleWager.WAGER_50_2x2_FRIENDS) {
//                assertEquals(0, getYesterdayTop(battleWager).size());
//                assertEquals(0, getDailyTop(battleWager).size());
//            } else {
//                assertEquals(profile1.getId().longValue(), getYesterdayTop(battleWager).first().id);
//                assertEquals(profile1.getId().longValue(), getDailyTop(battleWager).first().id);
//            }
//        }
//
//        assertEquals(1000, getYesterdayTop(BattleWager.WAGER_15_DUEL).first().ratingPoints);
//        assertEquals(1002 + 1003, getYesterdayTop(BattleWager.WAGER_50_2x2).first().ratingPoints);
//        assertEquals(0, getYesterdayTop(BattleWager.WAGER_50_2x2_FRIENDS).size());
//        assertEquals(1004, getYesterdayTop(BattleWager.WAGER_50_3_FOR_ALL).first().ratingPoints);
//
//        assertEquals(1000 + 1001 + 1002 + 1003 + 1004, getYesterdayTop(BattleWager.NO_WAGER).first().ratingPoints);
//
//        assertEquals(2000, getDailyTop(BattleWager.WAGER_15_DUEL).first().ratingPoints);
//        assertEquals(2002 + 2003, getDailyTop(BattleWager.WAGER_50_2x2).first().ratingPoints);
//        assertEquals(0, getDailyTop(BattleWager.WAGER_50_2x2_FRIENDS).size());
//        assertEquals(2004, getDailyTop(BattleWager.WAGER_50_3_FOR_ALL).first().ratingPoints);
//
//        assertEquals(2000 + 2001 + 2002 + 2003 + 2004, getDailyTop(BattleWager.NO_WAGER).first().ratingPoints);
//    }

//    @Test
//    public void test1() {
//        for(int i = 1; i <= RatingService.MAX_TOP; i++) {
//            int rating = Math.abs(new Random().nextInt(1000));
//            updateDailyTop(newProfile(i), rating);
//        }
//
//        assertEquals(RatingService.MAX_TOP, getDailyTop().size());
//
//        // улучшение своего рейтинга
//        UserProfile profile = newProfile(1);
//        int currentRating = dailyRegistry.getDailyRating(profile.getId(), BattleWager.NO_WAGER);
//        int newRatung = currentRating + 10;
//        updateDailyTop(profile, 10);
//
//        assertTrue(noRepeats(getDailyTop()));
//        assertEquals(newRatung, findProfilesRating(getDailyTop(), profile));
//        // добавление новенького в топ
//        profile = newProfile(RatingService.MAX_TOP + 1);
//        int rating = getDailyTop().last().ratingPoints + 10;
//        updateDailyTop(profile, rating);
//
//        assertTrue(noRepeats(getDailyTop()));
//        assertEquals(rating, findProfilesRating(getDailyTop(), profile));
//
//        // при минимальном рейтнге в топе игрок добавится не должен
//        profile = newProfile(RatingService.MAX_TOP + 2);
//        rating = getDailyTop().last().ratingPoints;
//        updateDailyTop(profile, rating);
//
//        assertTrue(noRepeats(getDailyTop()));
//        assertEquals(-1, findProfilesRating(getDailyTop(), profile));
//
//        // уход лидера
//        RatingProfileStructure first = getDailyTop().first();
//        profile = newProfile((int) first.id);
//        rating = first.ratingPoints;
//        updateDailyTop(profile, -rating);
//
//        assertEquals(RatingService.MAX_TOP, getDailyTop().size());
//        assertTrue(noRepeats(getDailyTop()));
//        assertEquals(0, findProfilesRating(getDailyTop(), profile));
//
//        dailyRegistry.getDailyTask().runServiceTask();
//
//
//        UserProfile userProfile = softCache.get(UserProfile.class, getYesterdayTop().first().id);
//        System.out.println(userProfile);
//    }

//    private void updateDailyTop(UserProfile profile, int rating) {
//        ratingService.updateDailyTop(profile, rating, BattleWager.NO_WAGER);
//        ratingService.updateDailyTop(profile, rating, BattleWager.WAGER_15_DUEL);
//    }
//
//    private void updateDailyTop(UserProfile profile, int rating, BattleWager battleWager) {
//        ratingService.updateDailyTop(profile, rating, BattleWager.NO_WAGER);
//        ratingService.updateDailyTop(profile, rating, battleWager);
//    }

//    private NavigableSet<RatingProfileStructure> getDailyTop() {
//        return ratingService.getDailyRatingByWager().get(BattleWager.NO_WAGER).dailyTop;
//    }
//
//    private NavigableSet<RatingProfileStructure> getDailyTop(BattleWager battleWager) {
//        return ratingService.getDailyRatingByWager().get(battleWager).dailyTop;
//    }
//
//    private NavigableSet<RatingProfileStructure> getYesterdayTop() {
//        return ratingService.getYesterdayRatingByWager().get(BattleWager.NO_WAGER).yesterdayTop;
//    }
//
//    private NavigableSet<RatingProfileStructure> getYesterdayTop(BattleWager battleWager) {
//        return ratingService.getYesterdayRatingByWager().get(battleWager).yesterdayTop;
//    }
//
//    private Map<Long, Integer> getYesterdayRating() {
//        return ratingService.getYesterdayRatingByWager().get(BattleWager.NO_WAGER).yesterdayRating;
//    }

    @Test
    public void testRepeats() {
        NavigableSet<RatingProfileStructure> dailyTop = new ConcurrentSkipListSet<RatingProfileStructure>(new Comparator<RatingProfileStructure>() {
            @Override
            public int compare(RatingProfileStructure o1, RatingProfileStructure o2) {
//                if(o1.id == o2.id) {
//                    return 0;
//                } else {
                if(o1.ratingPoints > o2.ratingPoints) {
                    return -1;
                } else if(o1.ratingPoints < o2.ratingPoints) {
                    return 1;
                } else if(o1.id > o2.id) {
                    return -1;
                } else if(o1.id < o2.id) {
                    return 1;
                } else {
                    return 0;
                }
//                }
            }
        });
        for(int i = 1; i <= 100; i++) {
            dailyTop.add(new RatingProfileStructure(newProfile(Math.abs(new Random().nextInt(30))), clanMember_rank, Math.abs(new Random().nextInt(100)), 0));
        }
        for(RatingProfileStructure ratingProfileStructure : dailyTop) {
            System.out.println(ratingProfileStructure);
        }

        assertTrue(noRepeats(dailyTop));

    }

    private final Callable<Tuple3<ClanMemberStructure, Byte, Byte>> clanMember_rank = () -> Tuple.of(new ClanMemberStructure(null), (byte) 0, (byte) 0);

    private int findProfilesRating(NavigableSet<RatingProfileStructure> dailyTop, UserProfile profile) {
        for(RatingProfileStructure ratingProfileStructure : dailyTop) {
            if(ratingProfileStructure.id == profile.getId()) {
                return ratingProfileStructure.ratingPoints;
            }
        }
        return -1;
    }

    private boolean noRepeats(NavigableSet<RatingProfileStructure> dailyTop) {
        Set<Long> set = new HashSet<Long>();
        for(RatingProfileStructure ratingProfileStructure : dailyTop) {
            set.add(ratingProfileStructure.id);
        }
        return dailyTop.size() == set.size();
    }

    private UserProfile newProfile(int id) {
        UserProfile profile = new UserProfile((long) id);
        profile.setOnline(true);
        profile.setStuff(new short[0]);
        softCache.put(UserProfile.class, profile.getId(), profile);
        return profile;
    }

}
