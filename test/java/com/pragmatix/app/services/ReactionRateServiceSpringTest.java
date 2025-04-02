package com.pragmatix.app.services;

import com.pragmatix.app.controllers.PumpReactionRateController;
import com.pragmatix.app.messages.client.PumpReactionRate;
import com.pragmatix.app.messages.client.PumpReactionRates;
import com.pragmatix.app.messages.server.PumpReactionRateResult;
import com.pragmatix.app.messages.server.PumpReactionRatesResult;
import com.pragmatix.app.messages.structures.PumpReactionRateStructure;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created: 30.04.11 12:25
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class ReactionRateServiceSpringTest extends AbstractSpringTest {

    @Resource
    PumpReactionRateController controller;

    @Resource
    SoftCache softCache;

    @Resource
    ReactionRateService reactionRateService;

    @Test
    public void reactionLevelValueTest() {
        UserProfile profile = profileService.getUserProfile(172441157L);
        UserProfileStructure userProfileStructure = profileService.getUserProfileStructure(profile);

        int[] reactionLevel = reactionRateService.getReactionLevel(userProfileStructure);
        System.out.println(Arrays.toString(reactionLevel));
    }

    @Test
    public void buyReactionLevelTest() {
        UserProfile profile = getProfile(testerProfileId);

        profile.setReactionRate(0);
        assertEquals(ShopResultEnum.ERROR, reactionRateService.buyReactionRate(1, profile));

        profile.setRealMoney(priceForLevel(2));
        assertEquals(ShopResultEnum.SUCCESS, reactionRateService.buyReactionRate(2, profile));
        assertEquals(0, profile.getRealMoney());
        assertEquals(valueForLevel(2), profile.getReactionRate());

        int level = 6;
        profile.setRealMoney(priceForLevel(level) / 2 + 1);
        profile.setReactionRate(valueForLevel(level - 1) + ((valueForLevel(level) - valueForLevel(level - 1)) / 2) + 1);
        assertEquals(ShopResultEnum.SUCCESS, reactionRateService.buyReactionRate(level, profile));
        assertEquals(0, profile.getRealMoney());
        assertEquals(valueForLevel(level), profile.getReactionRate());

        level = 20;
        profile.setReactionRate(4972950 * 2);
        assertEquals(ShopResultEnum.ERROR, reactionRateService.buyReactionRate(level, profile));
    }

    private int priceForLevel(int level) {
        return reactionRateService.getReactionLevelsConf().get(level).price;
    }

    private int valueForLevel(int level) {
        return reactionRateService.getReactionLevelsConf().get(level).value;
    }

    @Test
    public void testLimitPumpReactionRate() throws InterruptedException {
        UserProfile profile = getProfile(testerProfileId);
        for(int friendId = 1001; friendId <= 1100; friendId++) {
            getProfile(friendId);
            PumpReactionRateResult.ResultEnum result = reactionRateService.pumpReactionRate(profile, friendId);
            assertEquals(PumpReactionRateResult.ResultEnum.OK, result);
        }
        int friendId = 1101;
        PumpReactionRateResult.ResultEnum result = reactionRateService.pumpReactionRate(profile, friendId);
        assertEquals(PumpReactionRateResult.ResultEnum.lIMIT_EXEEDED, result);
    }

    @Test
    public void testLimitPumpReactionRates() throws InterruptedException {
        UserProfile profile = getProfile(testerProfileId);
        List<Long> friendIds = new ArrayList<>();
        for(int friendId = 2001; friendId <= 2101; friendId++) {
//            if(new Random().nextBoolean())
            getProfile(friendId);
            friendIds.add((long) friendId);
        }
        PumpReactionRateStructure[] result = reactionRateService.pumpReactionRates(profile, ArrayUtils.toPrimitive(friendIds.toArray(new Long[0])));
        for(int i = 0; i < result.length - 1; i++) {
            PumpReactionRateStructure pumpReactionRateStructure = result[i];
            assertEquals(PumpReactionRateResult.ResultEnum.OK.getType(), pumpReactionRateStructure.result);
        }
        PumpReactionRateStructure pumpReactionRateStructure = result[result.length - 1];
        assertEquals(PumpReactionRateResult.ResultEnum.lIMIT_EXEEDED.getType(), pumpReactionRateStructure.result);
    }

    @Test
    public void testPumpReactionRateCommand() {
        UserProfile profile = softCache.get(UserProfile.class, 58027749L);
        long friendId = 58027748L;
        UserProfile friendProfile = softCache.get(UserProfile.class, friendId);

        PumpReactionRate msg = new PumpReactionRate();
        PumpReactionRateResult result;
        int oldReactionRate = friendProfile.getReactionRate();

        // прокачиваем другу
        msg.friendId = friendProfile.getId();
        result = (PumpReactionRateResult) controller.onPumpReactionRate(msg, profile);
        assertEquals(PumpReactionRateResult.ResultEnum.OK.getType(), result.result);

        // пауза чтобы обновилась запись в БД
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        softCache.remove(UserProfile.class, friendId);
        friendProfile = softCache.get(UserProfile.class, friendId);
        assertEquals(oldReactionRate + 1, friendProfile.getReactionRate());

    }

    @Test
    public void testPumpReactionRatesCommand() {

        UserProfile profile = softCache.get(UserProfile.class, 58027749L);
        softCache.get(UserProfile.class, 1L).setOnline(true);
        softCache.get(UserProfile.class, 58027741L);


        long[] friendsIds = {1L, 58027741L, 58027742L, 58027743L, 58027744L, 58027745L, 58027746L, 58027747L, 58027748L};

        PumpReactionRates msg = new PumpReactionRates();
        msg.friendIds = friendsIds;

        PumpReactionRatesResult result = controller.onPumpReactionRates(msg, profile);
        PumpReactionRateStructure[] pumpedFriends = result.pumpedFriends;

        System.out.println(Arrays.toString(pumpedFriends));

        assertEquals(friendsIds.length, pumpedFriends.length);


        // пауза чтобы обновилась запись в БД
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void secureCheckTest() throws Exception {
        loginMain(testerProfileId);
        PumpReactionRate msg = new PumpReactionRate();
        msg.sessionKey = enterAccount.sessionKey;
        msg.friendId = testerProfileId;
        sendMain(msg);
    }

}
