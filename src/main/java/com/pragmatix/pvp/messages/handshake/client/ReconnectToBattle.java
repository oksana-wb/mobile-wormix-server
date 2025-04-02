package com.pragmatix.pvp.messages.handshake.client;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Команда ре-логина на сервер боя
 *
 * @see com.pragmatix.pvp.filters.PvpAuthFilter#reconnect(ReconnectToBattle)
 * @see com.pragmatix.pvp.controllers.PvpLoginController#onLogin(Object, com.pragmatix.pvp.model.PvpUser)
 */
@Command(1015)
public class ReconnectToBattle extends PvpLogin implements PvpCommandI {

    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;
    /**
     * номер хода
     */
    public short turnNum;

    public byte playerNum;
    /**
     * номер последней полученной или отправленной команды
     */
    public short lastCommandNum;

    public ReconnectToBattle() {
    }

    @Override
    public String toString() {
        return "ReconnectToBattle{" +
                super.toString() +
                ", battleId=" + battleId +
                ", turnNum=" + turnNum +
                ", playerNum=" + playerNum +
                ", lastCommandNum=" + lastCommandNum +
                ", secureResult=" + secureResult +
                '}';
    }

    @Override
    public long getBattleId() {
        return battleId;
    }
}
