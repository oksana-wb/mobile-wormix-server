package com.pragmatix.pvp.messages.handshake.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Отмена боя
 *
 * @see com.pragmatix.pvp.controllers.PvpController#onCancelBattle(CancelBattle, com.pragmatix.pvp.model.PvpUser)
 */
@Command(1004)
public class CancelBattle extends SecuredCommand implements PvpCommandI {

    /**
     * если бой отменяется клиентом на этапе загрузки карты
     */
    @Resize(TypeSize.UINT32)
    public long battleId;

    /**
     * код ошибки: присылается клиентом, отражается в логах
     */
    public short error;


    public CancelBattle() {
    }

    public CancelBattle(long battleId) {
        this.battleId = battleId;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "CancelBattle{" +
                "battleId=" + battleId +
                ", error=" + error +
                ", secureResult=" + secureResult +
                '}';
    }

    @Override
    public long getBattleId() {
        return battleId;
    }
}
