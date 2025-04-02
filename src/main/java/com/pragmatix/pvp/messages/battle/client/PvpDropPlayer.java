package com.pragmatix.pvp.messages.battle.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Команду присылает игрок когда выходит из боя. Или когда сдаётся или когда клиент говорит о том что игрок читер
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 29.08.11 15:38
 * @see com.pragmatix.pvp.filters.PvpProtocolCheckerFilter#checkMessage(PvpDropPlayer, com.pragmatix.pvp.model.PvpUser)
 * @see com.pragmatix.pvp.controllers.PvpController#onPvpExitFromBattle(PvpDropPlayer, com.pragmatix.pvp.model.PvpUser)
 */
@Command(1006)
public class PvpDropPlayer extends SecuredCommand implements PvpCommandI {

    public byte playerNum;

    @Resize(TypeSize.UINT32)
    public long battleId;

    public DropReasonEnum reason;

    /**
     * не сериализуются: используются только для внутреннего общения с {@link com.pragmatix.pvp.services.battletracking.handlers.UnbindHandler}
     */
    @Ignore
    public int banType;
    @Ignore
    public String banNote;

    public PvpDropPlayer() {
    }

    public PvpDropPlayer(int playerNum, long battleId, DropReasonEnum reason) {
        this.playerNum = (byte)playerNum;
        this.battleId = battleId;
        this.reason = reason;
        this.banType = 0;
        this.banNote = "";
    }

    public PvpDropPlayer(byte playerNum, long battleId, DropReasonEnum reason, int banType, String banNote) {
        this.playerNum = playerNum;
        this.battleId = battleId;
        this.reason = reason;
        this.banType = banType;
        this.banNote = banNote;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public long getBattleId() {
        return battleId;
    }

    @Override
    public String toString() {
        return "PvpDropPlayer{" +
                "playerNum=" + playerNum +
                ", battleId=" + battleId +
                ", reason=" + reason +
                ", secureResult=" + secureResult +
                (banType == 0 ? "" : ", banType=" + banType) +
                (banNote == null || banNote.isEmpty() ? "" : ", banNote=" + banNote) +
                '}';
    }

}
