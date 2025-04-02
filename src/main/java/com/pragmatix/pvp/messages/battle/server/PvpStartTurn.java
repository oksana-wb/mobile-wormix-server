package com.pragmatix.pvp.messages.battle.server;

import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.pvp.messages.battle.client.CountedCommandI;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.08.11 16:07
 */
@Command(1019)
public class PvpStartTurn implements CountedCommandI, SecuredResponse {

    @Resize(TypeSize.UINT32)
    public long battleId;

    /**
     * num игрока который должен ходить
     */
    public byte turningPlayerNum;

    /**
     * номер хода
     */
    public short turnNum;

    public short commandNum;

    public byte[] droppedPlayers;

    public PvpStartTurn() {
    }

    public PvpStartTurn(BattleBuffer battleBuffer, byte[] droppedPlayers) {
        this.battleId = battleBuffer.getBattleId();
        this.turningPlayerNum = battleBuffer.getInTurn().getPlayerNum();
        this.turnNum = (short) battleBuffer.getCurrentTurn().get();
        this.commandNum = (short) battleBuffer.getCurrentCommandNum().get();
        this.droppedPlayers = droppedPlayers;
    }


    @Override
    public short getCommandNum() {
        return commandNum;
    }

    @Override
    public short getTurnNum() {
        return turnNum;
    }

    @Override
    public byte getPlayerNum() {
        return -1;
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
        return "PvpStartTurn{" +
                "battleId=" + battleId +
                ", turningPlayerNum=" + turningPlayerNum +
                ", turnNum=" + turnNum +
                ", commandNum=" + commandNum +
                ", droppedPlayers=" + Arrays.toString(droppedPlayers) +
                '}';
    }
}
