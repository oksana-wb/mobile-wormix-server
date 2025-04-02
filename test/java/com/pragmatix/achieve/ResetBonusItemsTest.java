package com.pragmatix.achieve;

import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.achieve.messages.client.BuyResetBonusItems;
import com.pragmatix.achieve.messages.server.BuyResetBonusItemsResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.StuffService;
import com.pragmatix.app.services.WeaponService;
import com.pragmatix.app.common.ShopResultEnum;
import org.junit.Test;

import javax.annotation.Resource;

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.11.12 17:03
 */
public class ResetBonusItemsTest extends AchieveTest {

    @Resource
    private WeaponService weaponService;

    @Resource
    private StuffService stuffService;

    @Test
    public void resetBonusTest() throws Exception {
        loginAchieve();
        UserProfile profile = getProfile(testerProfileId);
        int realMoney = profile.getRealMoney();

        WormixAchievements wormixAchievements = softCache.get(WormixAchievements.class, "" + testerProfileId);
        wormixAchievements.setInvestedAwardPoints((byte) 2);

        int bonusWeaponId = 62;
        weaponService.addOrUpdateWeapon(profile, bonusWeaponId, -1);
        short bonusHatId = (short) 1027;
        stuffService.addStuff(profile, bonusHatId);
        assertTrue(weaponService.isPresentInfinitely(profile, bonusWeaponId));
        assertTrue(stuffService.isExist(profile, bonusHatId));

        BuyResetBonusItemsResult buyResetBonusItemsResult = requestAchieve(new BuyResetBonusItems(), BuyResetBonusItemsResult.class);
        assertEquals(ShopResultEnum.SUCCESS, buyResetBonusItemsResult.result);
        assertEquals(0, wormixAchievements.getInvestedAwardPoints());
        assertEquals(realMoney - 10, profile.getRealMoney());

        assertFalse(weaponService.isPresentInfinitely(profile, bonusWeaponId));
        assertFalse(stuffService.isExist(profile, bonusHatId));

        Thread.sleep(500);


    }


}
