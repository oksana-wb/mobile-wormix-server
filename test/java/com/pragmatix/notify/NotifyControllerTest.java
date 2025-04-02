package com.pragmatix.notify;

import com.pragmatix.app.common.Locale;
import com.pragmatix.app.messages.client.BuyBattle;
import com.pragmatix.app.messages.client.StartBattle;
import com.pragmatix.app.messages.server.BuyBattleResult;
import com.pragmatix.app.messages.server.StartBattleResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.BattleService;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.notify.message.NotifyProfile;
import com.pragmatix.notify.message.RegisterForNotify;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.08.13 17:00
 */
public class NotifyControllerTest extends AbstractSpringTest {

    @Test
    public void testOnRegisterForNotify() throws Exception {
        loginMain();

        RegisterForNotify registerForNotify = new RegisterForNotify();
        registerForNotify.registrationId = "" + testerProfileId;
        registerForNotify.socialNet = SocialServiceEnum.mobile;
        registerForNotify.sessionKey = enterAccount.sessionKey;

        sendMain(registerForNotify);

        Thread.sleep(1000);

        NotifyProfile notifyProfile = new NotifyProfile();
        notifyProfile.recipientProfileId = testerProfileId;
        notifyProfile.socialNet = SocialServiceEnum.mobile;
        notifyProfile.sessionKey = enterAccount.sessionKey;
        notifyProfile.localizedKey = "MESSAGE";

        sendMain(notifyProfile);

        Thread.sleep(1000);
    }

    @Resource
    BattleService battleService;

    @Test
    public void missionsRestoredNotify() throws Exception {
        loginMain();

        UserProfile profile = getProfile(testerProfileId);
        profile.setLevel(2);

        profile.setLocale(Locale.RU);

        profile.setBattlesCount(5);
        profile.setLastBattleTime(System.currentTimeMillis());

        sendMain(new StartBattle());
        receiveMain(StartBattleResult.class, 1000);

        Thread.sleep(battleService.getDelay(2) + 1000);

        profile.setLocale(Locale.EN);

        profile.setLastBattleTime(System.currentTimeMillis());

        sendMain(new StartBattle());
        receiveMain(StartBattleResult.class, 1000);

        Thread.sleep(battleService.getDelay(2) + 1000);
    }

    @Test
    public void missionsRestoredCancelNotify() throws Exception {
        loginMain();

        UserProfile profile = getProfile(testerProfileId);
        profile.setMoney(1000);
        profile.setRealMoney(10);
        profile.setLevel(5);
        profile.setBattlesCount(1);
        profile.setLastBattleTime(System.currentTimeMillis());

        sendMain(new StartBattle());
        StartBattleResult startBattleResult = receiveMain(StartBattleResult.class, 1000);
        Thread.sleep(1000);

        sendMain(new BuyBattle());
        BuyBattleResult buyBattleResult = receiveMain(BuyBattleResult.class, 1000);
        assertEquals(ShopResultEnum.SUCCESS, buyBattleResult.result);

        Thread.sleep(Integer.MAX_VALUE);
    }

}
