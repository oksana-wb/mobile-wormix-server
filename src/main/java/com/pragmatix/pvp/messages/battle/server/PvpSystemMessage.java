package com.pragmatix.pvp.messages.battle.server;

import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;

/**
 * Команда отправляеться сервером когда необходимо отправить сообщение в чат
 *
 */
@Command(2021)
public class PvpSystemMessage implements PvpCommandI {

    public enum TypeEnum implements TypeableEnum{
        PlayerDroppedByReconnectionTimeout(1),
        PlayerDroppedByCommandTimeout(2),
        PlayerDroppedByResponceTimeout(3),
        PlayerSurrendered(4),
        PlayerCheater(5),

        PlayerDisconnected(6),
        PlayerReconnected(7),
        PlayerLongTimeInTurn(8),
        BattleNotExist(9),
        PlayerNotFoundInBattle(10),
        AllJoinedToBattle(11),
        PlayerRepeatYourself(12),
        PlayerSkeepCommand(14),
        ;
        int type;

        TypeEnum(int type) {
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

    /**
	 * сообщение в чат
	 */
	public TypeEnum type;

    public byte playerNum;

    @Ignore
    public long battleId;

    public PvpSystemMessage() {
    }

    public PvpSystemMessage(TypeEnum typeEnum, int playerNum, long battleId) {
        this.type = typeEnum;
        this.playerNum = (byte) playerNum;
        this.battleId = battleId;
    }

    @Override
    public String toString() {
        return "PvpSystemMessage{" +
                "type=" + type +
                ", playerNum=" + playerNum +
                ", battleId=" + battleId +
                '}';
    }

    @Override
    public long getBattleId() {
        return battleId;
    }

}
