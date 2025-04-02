package com.pragmatix.pvp.messages.handshake.client;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.11.12 12:55
 * @see com.pragmatix.pvp.filters.PvpAuthFilter#authenticate(RejectBattleOffer)
 * @see com.pragmatix.pvp.controllers.PvpIntercomController#onRejectBattleOffer(RejectBattleOffer, com.pragmatix.sessions.IAppServer)
 */
@Command(1003)
public class RejectBattleOffer extends PvpLogin implements PvpCommandI {

    /**
     * id боя приглашение в который игрок отклонил
     */
    @Resize(TypeSize.UINT32)
    public long battleId;

    /**
     * признак того что игрок явно отклонил бой
     * за него это мог сделать клиент, зная что игрок в бою
     */
    public boolean manual;

    public RejectBattleOffer() {
    }

    public RejectBattleOffer(byte socialNetId, long profileId, long battleId, boolean manual) {
        this.socialNetId = socialNetId;
        this.profileId = profileId;
        this.battleId = battleId;
        this.manual = manual;
        this.secureResult = true;
    }

    @Override
    public long getBattleId() {
        return battleId;
    }

    @Override
    public String toString() {
        return "RejectBattleOffer{" +
                super.toString() +
                ", battleId=" + battleId +
                ", manual=" + manual +
                ", secureResult=" + secureResult +
                '}';
    }

}
