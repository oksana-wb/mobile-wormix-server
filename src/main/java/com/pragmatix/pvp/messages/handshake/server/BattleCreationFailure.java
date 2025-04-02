package com.pragmatix.pvp.messages.handshake.server;

import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.11.12 20:45
 */
@Command(2003)
public class BattleCreationFailure implements PvpCommandI {

    public enum CallToBattleResultEnum implements TypeableEnum{
        SERVER_ERROR(0),
        ACCEPTED(1),
        REJECTED(2),
        OFFLINE(3),
        BUSY(4),
        BANNED(5),
        CANCELED(6),
        TIMEOUT(7),
        CLIENT_ERROR(8),
        DISCONNECTED(9),
        NO_ENOUGH_MONEY(10),
        INSUFFICIENT_LEVEL(11),
        EXCEED_BATTLES(12),
        MISSION_LOCKED(14),
        ARENA_IS_LOCKED(15),
        TEAM_IS_SMALL(16),
        ;

        private int type;

        private CallToBattleResultEnum(int type) {
            this.type = type;
        }

        @Override
        public int getType() {
            return type;
        }
    }

    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;

    /**
     * кто сорвал создание боя
     */
    @Resize(TypeSize.UINT32)
    public long participant;


    public byte socialNetId;

    /**
     * причина
     */
    public CallToBattleResultEnum callToBattleResult;

    @Override
    public long getBattleId() {
        return battleId;
    }

    @Override
    public String toString() {
        return "BattleCreationFailure{" +
                "battleId=" + battleId +
                ", participant=" + participant +
                ", socialNetId=" + socialNetId +
                ", callToBattleResult=" + callToBattleResult +
                '}';
    }

}
