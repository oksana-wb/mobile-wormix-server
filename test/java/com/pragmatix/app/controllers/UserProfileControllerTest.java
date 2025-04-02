package com.pragmatix.app.controllers;

import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.achieve.messages.server.ChooseBonusItemResult;
import com.pragmatix.app.achieve.AchieveAwardService;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.common.utils.VarObject;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 31.01.12 17:25
 */
public class UserProfileControllerTest extends AbstractSpringTest {

    @Resource
    private AchieveAwardService achieveAwardService;

    @Test
    public void testCountBonusItemInBackpack() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        profile.setSocialId((byte) SocialServiceEnum.vkontakte.getType());
        WormixAchievements profileAchievements = new WormixAchievements("" + testerProfileId);
        profileAchievements.setInvestedAwardPoints((byte) (0));

        for(Integer achieveBonusItemId : Arrays.asList(1027, 1034, 2022)) {
            stuffService.removeStuff(profile, achieveBonusItemId.shortValue());
        }
        for(Integer achieveBonusItemId : Arrays.asList(62, 63, 64, 67, 68, 70, 77, 78, 115)) {
            weaponService.removeWeapon(profile, achieveBonusItemId);
        }

        //achievePoints=7860

        List<Integer> bonusItems = Arrays.asList(1027, 62, 64, 115, 1034, 77, 78);
        for(Integer itemId : bonusItems) {
            VarObject<ChooseBonusItemResult> error = new VarObject<>();
//            boolean result = achieveService.giveBonusItem(itemId, profileAchievements, error);
            println(error.value);
//            Assert.assertTrue(result);
        }

        int profilesBonusItemsCount = achieveAwardService.countBonusItemInBackpack(profile);
        Assert.assertEquals(bonusItems.size(), profilesBonusItemsCount);
    }

//    @Test
//    public void testOnSyncInvestedAwardPoints() throws Exception {
//        UserProfile profile = getProfile(testerProfileId);
//        profile.setSocialId((byte) SocialServiceEnum.vkontakte.getType());
//
//        int profilesBonusItemsCount = controller.countBonusItemInBackpack(profile);
//
//        WormixAchievements wormixAchievements = softCache.get(WormixAchievements.class, "" + testerProfileId);
//        wormixAchievements.setInvestedAwardPoints((byte) (profilesBonusItemsCount + 10));
//
//        SyncInvestedAwardPointsResult syncInvestedAwardPointsResult = controller.onSyncInvestedAwardPoints(new SyncInvestedAwardPoints(), profile);
//
//        System.out.println(syncInvestedAwardPointsResult);
//
//        assertNotNull(syncInvestedAwardPointsResult);
//        assertEquals(profilesBonusItemsCount, syncInvestedAwardPointsResult.investedAwardPoints);
//
//
//        wormixAchievements.setInvestedAwardPoints((byte) (profilesBonusItemsCount));
//
//        syncInvestedAwardPointsResult = controller.onSyncInvestedAwardPoints(new SyncInvestedAwardPoints(), profile);
//
//        System.out.println(syncInvestedAwardPointsResult);
//
//        assertNull(syncInvestedAwardPointsResult);
//
//    }

}
