package com.pragmatix.pvp.messages.handshake.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Команда начала боя
 *
 * @see com.pragmatix.pvp.controllers.PvpController#onReadyForBattle(ReadyForBattle, com.pragmatix.pvp.model.PvpUser)
 */
@Command(1023)
public class ReadyForBattle extends SecuredCommand implements PvpCommandI {
    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;

    public ReadyForBattle() {
    }

    public ReadyForBattle(long battleId) {
        this.battleId = battleId;
    }

    @Override
    public long getBattleId() {
        return battleId;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "ReadyForBattle{" +
                "battleId=" + battleId +
                '}';
    }
}
