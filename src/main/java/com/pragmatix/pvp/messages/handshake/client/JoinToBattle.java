package com.pragmatix.pvp.messages.handshake.client;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;
import com.pragmatix.serialization.annotations.Resize;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.11.12 12:47
 *
 * @see com.pragmatix.pvp.filters.PvpAuthFilter#authenticate(CreateBattleRequest)
 * @see com.pragmatix.pvp.controllers.PvpLoginController#onLogin(Object, com.pragmatix.pvp.model.PvpUser)
 *
 */
@Command(1002)
public class JoinToBattle extends PvpLogin implements PvpCommandI {

    /**
     * id боя приглашение в который игрок принял
     */
    @Resize(TypeSize.UINT32)
    public long battleId;


    @Override
    public long getBattleId() {
        return battleId;
    }

    @Override
    public String toString() {
        return "JoinToBattle{" +
                super.toString() +
                ", battleId=" + battleId +
                ", secureResult=" + secureResult +
                '}';
    }
}
