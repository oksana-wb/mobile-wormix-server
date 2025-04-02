package com.pragmatix.app.init;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.11.11 17:33
 */
public class UserProfileCreatorTest extends AbstractSpringTest {

    @Resource
    private ProfileService profileService;

    @Resource
    private UserProfileCreator userProfileCreator;

    @Test
    public void wipeTest() throws Exception {
        UserProfile profile = profileService.getUserProfile(16210154L);
        userProfileCreator.wipeUserProfile(profile);
        System.out.println("done!");
    }

}
