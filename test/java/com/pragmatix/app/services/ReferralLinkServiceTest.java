package com.pragmatix.app.services;

import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.client.Login;
import com.pragmatix.app.messages.server.EnterAccount;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import org.junit.Test;
import com.pragmatix.testcase.SimpleTest;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 16.04.2014 15:34
 */
public class ReferralLinkServiceTest extends com.pragmatix.testcase.AbstractSpringTest {

    @Resource
    private ReferralLinkService referralLinkService;

    @Test
    public void testAwardForReferralLink() throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        String newReferralLink = referralLinkService.addNewReferralLink(new Date(), cal.getTime(), 1,
                0, 90, 0, 0, "52 52 52 52 52", "114:5 77:3", 0, 0, 0);

        connectMain();

        Login message = new Login();
        message.socialNet = SocialServiceEnum.vkontakte;
        message.id = testerProfileId;
        message.ids = Collections.emptyList();
        message.authKey = SimpleTest.MASTER_AUTH_KEY();
        message.params = new String[]{ILogin.REFERRAL_LINK_TOKEN, newReferralLink};

        mainConnection.send(message);

        enterAccount = mainConnection.receive(EnterAccount.class, 15000000);
        sessionId = enterAccount.sessionKey;

        System.out.println(enterAccount.loginAwards);

        mainConnection.disconnect();
        Thread.sleep(300);
    }
}
