package com.pragmatix.achieve;

import com.pragmatix.achieve.messages.client.GetAchievements;
import com.pragmatix.achieve.messages.server.GetAchievementsResult;
import com.pragmatix.achieve.services.AchieveCommandService;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import com.pragmatix.testcase.HttpClientConnection;
import com.pragmatix.testcase.SimpleTest;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.02.12 12:01
 */
public class AchieveTest extends AbstractSpringTest {

    protected long timeSequence;
    protected GetAchievementsResult achievementsResult;

    @Resource
    protected AchieveCommandService achieveService;

    public void loginAchieve() throws Exception {
        loginMain(testerProfileId);
        achievementsResult = requestAchieve(new GetAchievements("" + testerProfileId), GetAchievementsResult.class);
        timeSequence = achievementsResult.timeSequence;
    }

    protected <T> T receiveAchieve(Class<T> cmdClass, int delay) {
        return mainConnection.receive(cmdClass, delay);
    }

    protected void sendAchieve(Object cmd) {
        mainConnection.send(cmd);
    }

    public <T> T requestAchieve(Object message, Class<T> cmdClass) {
        sendAchieve(message);
        return receiveAchieve(cmdClass, Integer.MAX_VALUE);
    }

}
