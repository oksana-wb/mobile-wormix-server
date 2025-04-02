package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.structures.SimpleProfileStructure;
import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.serialization.annotations.Command;

import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.02.13 11:25
 * @see com.pragmatix.app.messages.client.GetFriendsForMission
 */
@Command(1093)
public class GetFriendsForMissionResult {

    public enum FriendState implements TypeableEnum {
        SUCCESS(0),
        EXCEED_BATTLES(1),
        MISSION_LOCKED(2),;

        private final int type;

        FriendState(int type) {
            this.type = type;
        }

        @Override
        public int getType() {
            return type;
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", name(), type);
        }
    }

    public List<SimpleProfileStructure> friends;

    public List<FriendState> states;

    @Override
    public String toString() {
        return "GetFriendsForMissionResult{" +
                "friends(" + friends.size() + ")" +
                ", states(" + states + ")" +
                '}';
    }

}
