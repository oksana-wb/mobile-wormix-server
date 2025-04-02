package com.pragmatix.pvp.messages.battle.server;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.pvp.messages.battle.client.CountedCommandI;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.List;

/**
 * команда отсылается участникам в конце боя
 *
 * @see com.pragmatix.app.common.PvpBattleResult
 */
@Command(1021)
public class PvpEndBattle implements CountedCommandI, SecuredResponse {
    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;

    public short turnNum;

    public short commandNum;

    public PvpBattleResult battleResult;

    public int ratingPoints;

    public int rankPoints;

    public List<GenericAwardStructure> award;

    public boolean confirmed;

    public PvpEndBattle() {
    }

    public PvpEndBattle(BattleBuffer battleBuffer, BattleParticipant battleParticipant) {
        this.battleId = battleBuffer.getBattleId();
        this.turnNum = battleBuffer.getCurrentTurn().shortValue();
        this.commandNum = battleBuffer.getCurrentCommandNum().shortValue();

        this.battleResult = battleParticipant.battleResult;
        EndPvpBattleResultStructure battleResultStructure = battleParticipant.battleResultStructure();
        this.ratingPoints = battleResultStructure.ratingPoints;
        this.rankPoints = battleResultStructure.rankPoints;
    }

    public PvpEndBattle setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
        return this;
    }

    public PvpEndBattle fillAward(List<GenericAwardStructure> award) {
        this.award = award;
        return this;
    }

    @Override
    public long getBattleId() {
        return battleId;
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
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "PvpEndBattle{" +
                "battleId=" + battleId +
                ", turnNum=" + turnNum +
                ", commandNum=" + commandNum +
                ", battleResult=" + battleResult +
                ", ratingPoints=" + ratingPoints +
                ", rankPoints=" + rankPoints +
                ", award=" + award +
                ", confirmed=" + confirmed +
                '}';
    }
}
