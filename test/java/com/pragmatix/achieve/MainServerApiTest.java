package com.pragmatix.achieve;

import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.messages.client.BuyResetBonusItems;
import com.pragmatix.achieve.messages.server.BuyResetBonusItemsResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.intercom.messages.IntercomAchieveRequest;
import com.pragmatix.sessions.AppServerAddress;
import com.pragmatix.sessions.IAppServer;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 14.08.2016 14:44
 */
public class MainServerApiTest extends AchieveTest {

    @Test
    public void buyResetBonusItemsTest() throws Exception {
        UserProfile profile = profileService.getUserProfile(testerProfileId);
        profile.setRealMoney(10);
        int bonusWeaponId = 62;
        weaponService.addOrUpdateWeapon(profile, bonusWeaponId, -1);

        loginAchieve();

        ProfileAchievements profileAchievements = achieveService.getProfileAchievements("" + testerProfileId);
        profileAchievements.setInvestedAwardPoints((byte)1);

        sendAchieve(new BuyResetBonusItems());
        BuyResetBonusItemsResult result = receiveAchieve(BuyResetBonusItemsResult.class, 300);

        Assert.assertEquals(ShopResultEnum.SUCCESS ,result.result);
    }

    @Test
    public void sendIntercomAchieveRequestTest() throws Exception {
        IAppServer mainAppServerAddress = new AppServerAddress("main");
        IntercomAchieveRequest request = IntercomAchieveRequest.BuyResetBonusItems("" + testerProfileId, 1);
        Messages.toServer(request, mainAppServerAddress, true);

        Thread.sleep(3000);
    }

}
