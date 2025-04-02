package com.pragmatix.app.services;

import com.pragmatix.app.messages.server.PumpReactionRateResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.pvp.services.matchmaking.WagerMatchmakingService;
import com.pragmatix.testcase.AbstractTest;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created: 30.04.11 12:25
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class ReactionRateServiceTest extends AbstractTest {

    private final DailyRegistry dailyRegistry = new DailyRegistry();
    private WagerMatchmakingService battleService = new WagerMatchmakingService();
    private ReactionRateService reactionRateService = new ReactionRateService();
    private SoftCache softCache = new SoftCache();
    private UserProfile profile;
    private UserProfile friendProfile;
    private UserProfile friendProfile2;
    private PersistenceService persistenceService = new PersistenceService();

    @Before
    public void init() {
        persistenceService.init();
        reactionRateService.setSoftCache(softCache);
        reactionRateService.setPersistenceService(persistenceService);
        reactionRateService.setDailyRegistry(dailyRegistry);
        reactionRateService.initByDefault();

        profile = new UserProfile(1L);
        softCache.put(UserProfile.class, profile.getId(), profile);
        profile.setOnline(true);

        friendProfile = new UserProfile(2L);
        softCache.put(UserProfile.class, friendProfile.getId(), friendProfile);
        friendProfile.setOnline(true);

        friendProfile2 = new UserProfile(3L);
        softCache.put(UserProfile.class, friendProfile2.getId(), friendProfile2);
        friendProfile2.setOnline(true);

    }

    @Test
    public void persistTest() throws InterruptedException {
//        reactionRateService.pumpReactionRate(profile, 2l);
//        reactionRateService.pumpReactionRate(profile, 3l);
//        reactionRateService.pumpReactionRate(friendProfile, 1l);
//        reactionRateService.pumpReactionRate(friendProfile, 3l);
//        reactionRateService.pumpReactionRate(friendProfile2, 1l);
//        reactionRateService.pumpReactionRate(friendProfile2, 2l);
//
//        reactionRateService.getDailyTask().runServiceTask();
//
//        reactionRateService.pumpReactionRate(friendProfile, 1l);
//        reactionRateService.pumpReactionRate(friendProfile, 3l);
//        reactionRateService.pumpReactionRate(friendProfile2, 1l);
//        reactionRateService.pumpReactionRate(friendProfile2, 2l);
//
//        reactionRateService.getDailyTask().runServiceTask();
//
//        reactionRateService.pumpReactionRate(friendProfile2, 1l);
//        reactionRateService.pumpReactionRate(friendProfile2, 2l);
//
//        reactionRateService.persistToDisk();
//        persistenceService.persistObjectToFile(reactionRateService.getWhoPumpedRateByDays(), ReactionRateService.keepFileName, new WhoPumpedReactionByDaysKeeper());

        reactionRateService.init();

        System.out.println(reactionRateService.getWhoPumpedRateByDays());
        System.out.println("");

        reactionRateService.persistToDisk();
    }

    @Test
    public void testDailyTask() throws InterruptedException {
        PersistenceService persistenceService = new PersistenceService();
        persistenceService.setDataDir("D:\\temp\\");
        persistenceService.init();

        reactionRateService.setPersistenceService(persistenceService);
        reactionRateService.init();

        assertTrue(reactionRateService.getWhoPumpedRateByDays().get(0) instanceof ConcurrentMap);
        assertTrue(reactionRateService.getWhoPumpedRateByDays().get(1) instanceof HashMap);
        assertTrue(reactionRateService.getWhoPumpedRateByDays().get(2) instanceof HashMap);

        reactionRateService.getDailyTask().runServiceTask();

//        assertTrue(reactionRateService.getWhoPumpedRateByDays().get(0) instanceof ConcurrentMap);
//        assertTrue(reactionRateService.getWhoPumpedRateByDays().get(1) instanceof HashMap);
//        assertTrue(reactionRateService.getWhoPumpedRateByDays().get(2) instanceof HashMap);


        Thread.sleep(Long.MAX_VALUE);
    }

    @Test
    public void testPumpReactionRateCommand() {

        PumpReactionRateResult.ResultEnum resultEnum;

        // попытка прокачать самому себе
        resultEnum = reactionRateService.pumpReactionRate(profile, profile.getId());
        assertEquals(PumpReactionRateResult.ResultEnum.ERROR, resultEnum);
        assertEquals(0, profile.getReactionRate());

        // прокачиваем другу
        resultEnum = reactionRateService.pumpReactionRate(profile, friendProfile.getId());
        assertEquals(PumpReactionRateResult.ResultEnum.OK, resultEnum);
        assertEquals(1, friendProfile.getReactionRate());

        // прокачиваеш другу еще раз
        resultEnum = reactionRateService.pumpReactionRate(profile, friendProfile.getId());
        assertEquals(PumpReactionRateResult.ResultEnum.TODAY_ALREADY_PUMPED, resultEnum);
        assertEquals(1, friendProfile.getReactionRate());

        // друг прокачивает тебе
        resultEnum = reactionRateService.pumpReactionRate(friendProfile, profile.getId());
        assertEquals(PumpReactionRateResult.ResultEnum.OK, resultEnum);
        assertEquals(1, profile.getReactionRate());

        // 2-ой друг прокачивает тебе
        resultEnum = reactionRateService.pumpReactionRate(friendProfile2, profile.getId());
        assertEquals(PumpReactionRateResult.ResultEnum.OK, resultEnum);
        assertEquals(2, profile.getReactionRate());

        // сбрасываем кэш
        reactionRateService.getDailyTask().runServiceTask();

        // прокачиваем другу, на след день
        resultEnum = reactionRateService.pumpReactionRate(profile, friendProfile.getId());
        assertEquals(PumpReactionRateResult.ResultEnum.OK, resultEnum);
        assertEquals(2, friendProfile.getReactionRate());

    }

    @Test
    public void testLimitPumpedCommand() {
        // прокачиваем другу
        PumpReactionRateResult.ResultEnum resultEnum = reactionRateService.pumpReactionRate(profile, friendProfile.getId());
        assertEquals(PumpReactionRateResult.ResultEnum.OK, resultEnum);
        assertEquals(1, friendProfile.getReactionRate());

        // друг прокачивает тебе
        resultEnum = reactionRateService.pumpReactionRate(friendProfile, profile.getId());
        assertEquals(PumpReactionRateResult.ResultEnum.OK, resultEnum);
        assertEquals(1, profile.getReactionRate());

        // прокачиваем 99-ти друзьям
        for(int i = 1; i < 100; i++) {
            UserProfile friendProfile = new UserProfile(1000L + i);
            softCache.put(UserProfile.class, friendProfile.getId(), friendProfile);

            resultEnum = reactionRateService.pumpReactionRate(profile, friendProfile.getId());
            assertEquals(PumpReactionRateResult.ResultEnum.OK, resultEnum);
            assertEquals(1, friendProfile.getReactionRate());
        }
        resultEnum = reactionRateService.pumpReactionRate(profile, friendProfile.getId());
        assertEquals(PumpReactionRateResult.ResultEnum.lIMIT_EXEEDED, resultEnum);

        // сбрасываем кэш
        reactionRateService.getDailyTask().runServiceTask();
        dailyRegistry.clearFor(profile.getId());

        resultEnum = reactionRateService.pumpReactionRate(profile, friendProfile.getId());
        assertEquals(PumpReactionRateResult.ResultEnum.OK, resultEnum);
        assertEquals(2, friendProfile.getReactionRate());

    }

    @Test
    public void testMyTurn() {
        UserProfile profile = new UserProfile(1L);
        UserProfile enemyProfile = new UserProfile(2L);
        int myTurnInPercent;
        int myTurnInPercentTheory;

        // мой рейтинг больше чем в 4 раза
        profile.setReactionRate(10 * 5);
        enemyProfile.setReactionRate(10);
        myTurnInPercent = getMyTurnInPersent(profile, enemyProfile);
        System.out.println("myTurn=" + myTurnInPercent + "%");
        assertTrue(Math.abs(myTurnInPercent - 80) <= 1);

        // мой рейтинг больше чем в 4 раза
        profile.setReactionRate(10);
        enemyProfile.setReactionRate(10 * 5);
        myTurnInPercent = getMyTurnInPersent(profile, enemyProfile);
        System.out.println("myTurn=" + myTurnInPercent + "%");
        assertTrue(Math.abs(myTurnInPercent - 20) <= 1);


        int reactionRate = AppUtils.generateRandom(10000000);
        profile.setReactionRate(reactionRate);
        int enemyReactionRate = AppUtils.generateRandom(10000000);
        enemyProfile.setReactionRate(enemyReactionRate);

        myTurnInPercent = getMyTurnInPersent(profile, enemyProfile);
        myTurnInPercentTheory = Math.round(reactionRate * 100 / (enemyReactionRate + reactionRate));
        System.out.println(myTurnInPercentTheory + "% = " + myTurnInPercent + "%");

        assertTrue(Math.abs(myTurnInPercent - myTurnInPercentTheory) <= 1);

        reactionRate = AppUtils.generateRandom(1000);
        profile.setReactionRate(reactionRate);
        enemyReactionRate = AppUtils.generateRandom(100);
        enemyProfile.setReactionRate(enemyReactionRate);

        myTurnInPercent = getMyTurnInPersent(profile, enemyProfile);
        myTurnInPercentTheory = Math.round(reactionRate * 100 / (enemyReactionRate + reactionRate));
        System.out.println(myTurnInPercentTheory + "% <> " + myTurnInPercent + "%");

        assertTrue(Math.abs(myTurnInPercent - myTurnInPercentTheory) > 1);
    }


    private int getMyTurnInPersent(UserProfile profile, UserProfile enemyProfile) {
//        float myTurn = 0;
//        float enemyTurn = 0;
//        for(int i = 0; i < 100000; i++) {
//            PvpUser pvpProfile = new PvpUser(profile.getId(), (byte) 1,profile.getId(), BattleWager.FIRST_WAGER, 0,0,"");
//            pvpProfile.setProfileStructure(profile.getUserProfileStructure());
//            PvpUser enemyPvpProfile = new PvpUser(profile.getId(), (byte) 1,profile.getId(),BattleWager.FIRST_WAGER, 0,0,"");
//            enemyPvpProfile.setProfileStructure(enemyProfile.getUserProfileStructure());
//
//            if(battleService.playTurn(pvpProfile, enemyPvpProfile)) {
//                myTurn++;
//            } else {
//                enemyTurn++;
//            }
//
//        }
//        return Math.round(myTurn * 100 / (enemyTurn + myTurn));
        return 0;
    }
}
