package com.pragmatix.app.login;

import com.pragmatix.app.domain.BackpackItemEntity;
import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.client.Login;
import com.pragmatix.app.messages.client.LoginByProfileStringId;
import com.pragmatix.app.messages.server.EnterAccount;
import com.pragmatix.app.messages.server.LoginError;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import com.pragmatix.testcase.HttpClientConnection;
import com.pragmatix.testcase.SimpleTest;
import com.pragmatix.testcase.SocketClientConnection;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 09.11.11 11:10
 */
public class SuccessLoginTest extends AbstractSpringTest {

    @Test
    public void loginSuccessTest() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        profile.setLastLoginDateTime(LocalDateTime.now().minusMinutes(1));
        // заполняем оружием выдаваеиым на старте
        List<BackpackItemEntity> backpack = userProfileCreator.createDefaultBackpack(profile.getId());
        profile.setBackpack(userProfileCreator.initBackpack(backpack));
        profile.setLevel(29);
        profile.setUserProfileStructure(null);
        profile.setExperience(1220);

        dailyRegistry.clearFor(testerProfileId);

//        appParams.setAppVersion("0.0.2.2");

        mainConnection = new SocketClientConnection(binarySerializer).connect("127.0.0.1", 6001);

        Login message = new Login();
        message.socialNet = SocialServiceEnum.vkontakte;
        message.id = testerProfileId;
        message.authKey = SimpleTest.MASTER_AUTH_KEY();
        message.version = AppParams.versionFromString("0.0.2.2");
        message.params = new String[]{ILogin.REFERRAL_LINK_TOKEN, "bby0vr28"};

        mainConnection.send(message);

        enterAccount = mainConnection.receive(EnterAccount.class, 1500);
        sessionId = enterAccount.sessionKey;

        println(enterAccount.loginAwards);

        println("level: " + profile.getLevel());
        println("experience: " + profile.getExperience());
        println("bossWinAwardToken: " + dailyRegistry.getBossWinAwardToken(profile.getProfileId()));
        println("wagerWinAwardToken: " + dailyRegistry.getWagerWinAwardToken(profile.getProfileId()));

        Thread.sleep(5000L);
    }

    @Test
    public void mobileLoginSuccessTest() throws Exception {
        //getProfile(testerProfileId).setLastLoginDateTime(LocalDateTime.now().minusMinutes(1));

        appParams.setAppVersion("0.0.0.2");

        HttpClientConnection mainConnection = new HttpClientConnection("http://127.0.0.1:6001/", binarySerializer);

        LoginByProfileStringId message = new LoginByProfileStringId();
        message.socialNet = SocialServiceEnum.android;
        message.id = "" + (10000 + new Random().nextInt(10000));
        message.authKey = SimpleTest.MASTER_AUTH_KEY();
        message.version = AppParams.versionFromString("0.0.0.2");
//        message.params = new String[]{ILogin.DEBUG_LOGIN_AWARDS, "withdrawSeasonWeapon=true"};

        mainConnection.send(message);

        enterAccount = mainConnection.receive(EnterAccount.class, 1500);
        sessionId = enterAccount.sessionKey;

        println(enterAccount.loginAwards);

        Thread.sleep(2000L);
    }

    @Test
    public void loginOk() throws Exception {
        mainConnection = new SocketClientConnection(binarySerializer).connect("aurora.rmart.ru", 6000);

        LoginByProfileStringId message = new LoginByProfileStringId();
        message.socialNet = SocialServiceEnum.odnoklassniki;
        message.id = "405526675054";
        message.authKey = SimpleTest.MASTER_AUTH_KEY();

        mainConnection.send(message);

        enterAccount = mainConnection.receive(EnterAccount.class, 1500);
        sessionId = enterAccount.sessionKey;
    }

    @Test
    public void loginVk() throws Exception {
        mainConnection = new SocketClientConnection(binarySerializer).connect("178.218.211.146", 60101);
//        mainConnection = new SocketClientConnection(binarySerializer).connect("slots.rmart.ru", 61101);

        Login message = new Login();
        message.socialNet = SocialServiceEnum.vkontakte;
        message.id = testerProfileId;
        message.authKey = SimpleTest.MASTER_AUTH_KEY();

        mainConnection.send(message);

        Thread.sleep(1000L);
        List<Object> incomingMessages = mainConnection.getMessageHandler().getIncomingMessages();
        println(incomingMessages);

//        enterAccount = mainConnection.receive(EnterAccount.class, 1500);
//        sessionId = enterAccount.sessionKey;
        LoginError loginError = mainConnection.receive(LoginError.class, 1500);
        println(loginError);
    }

}
