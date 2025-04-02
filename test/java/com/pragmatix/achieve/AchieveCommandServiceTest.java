package com.pragmatix.achieve;

import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.messages.client.GetAchievements;
import com.pragmatix.achieve.messages.client.IncreaseAchievements;
import com.pragmatix.achieve.messages.server.GetAchievementsResult;
import com.pragmatix.achieve.messages.server.IncreaseAchievementsResult;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.08.2016 14:06
 */
public class AchieveCommandServiceTest extends AchieveTest {

    @Test
    public void increaseAchievementsTest() throws Exception {
        loginAchieve();

        IncreaseAchievements message = new IncreaseAchievements();
        message.sessionKey = sessionId;
        message.timeSequence = ++timeSequence;
        message.achievementsIndex = new int[] {1, 51};
        message.achievementsRise = new int[] {40_000, 40_000};
        message.boolAchievements = new int[] {1};

        requestAchieve(message, IncreaseAchievementsResult.class);

//        message = new IncreaseAchievements();
//        message.sessionKey = sessionId;
//        message.timeSequence = ++timeSequence;
//        message.achievementsIndex = new int[] {1, 51};
//        message.achievementsRise = new int[] {40_000, 40_000};
//        message.boolAchievements = new int[] {1};
//
//        requestAchieve(message, IncreaseAchievementsResult.class);

        disconnectMain();
        Thread.sleep(300);
    }
}
