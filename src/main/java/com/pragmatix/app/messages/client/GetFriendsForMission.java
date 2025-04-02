package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * Запросить списак друзей для совместного прохождения босса
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.02.13 11:21
 *
 * @see com.pragmatix.app.controllers.FriendListController#onGetFriendsForMission(GetFriendsForMission, com.pragmatix.app.model.UserProfile)
 */
@Command(93)
public class GetFriendsForMission {

    public short[] missionIds;

    @Override
    public String toString() {
        return "GetFriendsForMission{" +
                "missionIds=" + Arrays.toString(missionIds) +
                '}';
    }

}
