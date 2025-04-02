package com.pragmatix.pvp.messages.handshake.server;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Команда начала боя
 *
 * @see com.pragmatix.pvp.services.battletracking.handlers.HandlerI#handle(com.pragmatix.pvp.model.PvpUser, com.pragmatix.pvp.messages.PvpCommandI, com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum, com.pragmatix.pvp.model.BattleBuffer)
 */
@Command(2001)
public class StartPvpBattle implements PvpCommandI {

    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;

    /**
     * Сессионный ключ
     */
    public String sessionKey;

    /**
     * Номер игрока в бою
     */
    public byte playerNum;

    public StartPvpBattle(long battleId, String sessionKey, int playerNum) {
        this.battleId = battleId;
        this.sessionKey = sessionKey;
        this.playerNum = (byte) playerNum;
    }

    public StartPvpBattle() {
    }

    @Override
    public String toString() {
        return "StartPvpBattle{" +
                "battleId=" + battleId +
                ", sessionKey='" + sessionKey + '\'' +
                ", playerNum=" + playerNum +
                '}';
    }

    @Override
    public long getBattleId() {
        return battleId;
    }

}
