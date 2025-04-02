package com.pragmatix.achieve;

import com.pragmatix.achieve.common.AchieveUtils;
import com.pragmatix.achieve.controllers.AchieveController;
import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.achieve.messages.client.GetAchievements;
import com.pragmatix.achieve.messages.client.IncreaseAchievements;
import com.pragmatix.achieve.messages.server.GetAchievementsResult;
import org.junit.Test;

import javax.annotation.Resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.06.13 14:44
 */
public class BoolAchievsTest extends AchieveTest {

    @Resource
    private AchieveController achieveController;

    @Test
    public void test() throws Exception {
        int boolAchieveIndex = 1;

        loginAchieve();

        assertFalse(achievementsResult.boolAchievements.contains(boolAchieveIndex));

        IncreaseAchievements increaseAchievements = new IncreaseAchievements();
        increaseAchievements.sessionKey = sessionId;
        increaseAchievements.timeSequence = timeSequence + 1;
        increaseAchievements.boolAchievements = new int[]{boolAchieveIndex};

        sendAchieve(increaseAchievements);

        Thread.sleep(1000);

        GetAchievements getAchievements = new GetAchievements();
        getAchievements.profileId = "" + testerProfileId;

        sendAchieve(getAchievements);
        GetAchievementsResult getAchievementsResult = receiveAchieve(GetAchievementsResult.class, 100);

        assertFalse(getAchievementsResult.boolAchievements.contains(0));
        assertTrue(getAchievementsResult.boolAchievements.contains(boolAchieveIndex));

        disconnectMain();

        Thread.sleep(1000);
    }

}
