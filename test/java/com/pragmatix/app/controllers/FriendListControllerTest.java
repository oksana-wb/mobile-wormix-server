package com.pragmatix.app.controllers;

import com.pragmatix.app.messages.client.GetFriendsForMission;
import com.pragmatix.app.messages.server.GetFriendsForMissionResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.09.13 18:46
 */
public class FriendListControllerTest extends AbstractSpringTest {

    @Test
    public void testOnGetFriendsForMission() throws Exception {
        UserProfile friend = getProfile(193232279l);
        friend.setOnline(true);
        friend.setBattlesCount(0);
        friend.setLastBattleTime(System.currentTimeMillis());
        System.out.println("missionId=" + friend.getCurrentMission());
        System.out.println("newMissionId=" + friend.getCurrentNewMission());

        loginMain(testerProfileId, 193232279l);
        GetFriendsForMission message = new GetFriendsForMission();
        message.missionIds = new short[]{104, 7};
        sendMain(message);
        GetFriendsForMissionResult result = receiveMain(GetFriendsForMissionResult.class, Integer.MAX_VALUE);
        System.out.println(result);
    }
}
