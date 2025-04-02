package com.pragmatix.app.login;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import java.util.Date;
import java.util.Random;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.12.2014 16:09
 */
public class LoginTest extends AbstractSpringTest {

    @Test
    public void siccessLogin() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        profile.setLevel(25);
        profile.setLevelUpTime(new Date());
        profile.getBackpack().clear();
        
        loginMain();
        mainConnection.disconnect();

        Thread.sleep(300);
    }

    @Test
    public void newUserLogin() throws Exception {
        loginMain(new Random().nextInt(1_000_000), testerProfileId);
    }

}
