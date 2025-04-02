package com.pragmatix.app.login;

import com.pragmatix.app.common.BanType;
import com.pragmatix.app.messages.client.Login;
import com.pragmatix.app.messages.server.UserIsBanned;
import com.pragmatix.app.services.BanService;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.serialization.utils.SecurityUtils;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 09.11.11 11:10
 */
public class BotDetectedTest extends AbstractSpringTest {

    @Resource
    private BanService banService;

    @Resource
    private SecurityUtils securityUtils;

    @Test
    public void invalidLoginTest() throws Exception {

        banService.remove(testerProfileId);

        String secret = securityUtils.getSecret();
        securityUtils.setup("123");

        loginMain();
        securityUtils.setup(secret);

        Thread.sleep(11000);

        assertFalse(mainConnection.getChannel().isActive());

        assertNotNull(banService.get(testerProfileId));
        assertEquals(BanType.BAN_FOR_LOGIN_HACK.getType(), banService.get(testerProfileId).getBanReason());


        loginMain();
        Thread.sleep(300);

        receiveMain(UserIsBanned.class);
    }

    @Test
    public void repeatedLoginTest() throws Exception {

        long cheaterProfileId = 758030L;
        banService.remove(testerProfileId);
        banService.remove(cheaterProfileId);

        loginMain();
        Thread.sleep(300);

        Login message = new Login();
        message.socialNet = SocialServiceEnum.vkontakte;
        message.id = testerProfileId;
        message.ids = Collections.singletonList(cheaterProfileId);
        message.params = new String[0];

        sendMain(message, 300);

        assertFalse(mainConnection.getChannel().isActive());

        assertNotNull(banService.get(testerProfileId));
        assertEquals(BanType.BAN_FOR_BOT.getType(), banService.get(testerProfileId).getBanReason());

        loginMain();
        Thread.sleep(300);

        receiveMain(UserIsBanned.class);
    }

}
