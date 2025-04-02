package com.pragmatix.app.init;

import com.pragmatix.app.messages.client.LoginByProfileStringId;
import com.pragmatix.app.messages.server.EnterAccount;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertTrue;


/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.09.11 17:05
 */
public class ProfileStringIdTest extends AbstractSpringTest {

    @Test
    public void testLogin() throws Exception {
        connectMain();

        LoginByProfileStringId login = new LoginByProfileStringId();
        login.id = "john.boroda";
        login.ids = Collections.singletonList("boroda.friend.1");
        login.socialNet = SocialServiceEnum.vkontakte;

        sendMain(login);

        EnterAccount enterAccount = receiveMain(EnterAccount.class);

        assertTrue(enterAccount.userProfileStructure.profileStringId != null);
        assertTrue(enterAccount.profileStructures.get(0).profileStringId != null);
    }

}
