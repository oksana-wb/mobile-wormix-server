package com.pragmatix.intercom.messages;

import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.annotations.Command;

/**
 * Запрос профиля игрока с целью подбора соперника
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.05.12 11:45
 * @see com.pragmatix.pvp.controllers.PvpIntercomController#onGetProfileError(GetProfileError, com.pragmatix.sessions.IAppServer)
 */
@Command(4001)
public class GetProfileError extends IntercomResponse implements PvpCommandI {

    public enum GetProfileErrorEnum implements TypeableEnum {
        PROFILE_NOT_FOUND(1),
        INSUFFICIENT_LEVEL(2),
        NO_ENOUGH_MONEY(3),
        BATTLE_STATE_MISMATCH(4),
        CONNECTION_STATE_MISMATCH(5),
        PROFILE_IS_BANNED(6),
        ERROR(7),
        EXCEED_BATTLES(8),
        MISSION_LOCKED(9),
        ARENA_IS_LOCKED(10),
        MERCENARIES_BATTLE_NOT_ACCESSIBLE(11),
        INVALID_VERSION(12),
        TEAM_IS_SMALL(14),
        ;
        private int type;

        GetProfileErrorEnum(int type) {
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

    public GetProfileErrorEnum error;

    public long battleId;

    public GetProfileError() {
    }

    public GetProfileError(GetProfileRequest request, GetProfileErrorEnum error) {
        super(request);
        this.battleId = request.battleId;
        this.error = error;
    }

    @Override
    public long getBattleId() {
        return battleId;
    }

    @Override
    public String toString() {
        return "GetProfileError{" +
                "userId=" + socialNetId + ":" + profileId +
                ", battleId=" + battleId +
                ", error=" + error +
                ", requestId=" + requestId +
                '}';
    }

}
