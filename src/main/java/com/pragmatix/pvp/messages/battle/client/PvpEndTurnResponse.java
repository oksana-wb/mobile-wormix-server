package com.pragmatix.pvp.messages.battle.client;

import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.common.utils.ArrayUtils;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Serialize;
import com.pragmatix.serialization.annotations.SecureResult;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.06.11 12:03
 *
 * @see com.pragmatix.pvp.controllers.PvpController#onPvpEndTurnResponse(PvpEndTurnResponse, com.pragmatix.pvp.model.PvpUser)
 */
@Command(1017)
public class PvpEndTurnResponse extends SecuredCommand implements PvpCommandI {

    public short turnNum;

    public short commandNum;
    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;
    /**
     * игроки убитые во время хода
     */
    public byte[] droppedPlayers;
    /**
     * массив реагентов, которые игрок собрал за ход
     * структура: первым находится playerNum, следом id реагент (пары могут повторяться)
     */
    public byte[] collectedReagents;
    /**
     * массив потраченого оружия за ход, тем чей сейчас ход
     */
    public BackpackItemStructure[] items;
    /**
     * параметр используется для более раннего обнаружения рассинхронизации боя
     */
    public int battleState;
    /**
     * выбывшие юниты
     * структура: первым находится playerNum, следом количество выбитых этим игроком юнитов на этом ходу
     */
    public byte[] droppedUnits;
    /**
     * HP участников боя в % от первоночального
     */
    public short[] participantsHealthInPercent;

    public PvpEndTurnResponse() {
    }

    public PvpEndTurnResponse(PvpEndTurn pvpEndTurn) {
        this.turnNum = pvpEndTurn.turnNum;
        this.commandNum = pvpEndTurn.commandNum;
        this.battleId = pvpEndTurn.battleId;
        this.droppedPlayers = pvpEndTurn.droppedPlayers;
        this.collectedReagents = pvpEndTurn.collectedReagents;
        this.droppedUnits = pvpEndTurn.droppedUnits;
        this.participantsHealthInPercent = pvpEndTurn.participantsHealthInPercent;
        this.items = pvpEndTurn.items;
        this.battleState = pvpEndTurn.battleState;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "PvpEndTurnResponse{" +
                "turnNum=" + turnNum +
                ", commandNum=" + commandNum +
                ", battleId=" + battleId +
                ", droppedPlayers=" + Arrays.toString(droppedPlayers) +
                ", participantsHealthInPercent=" + Arrays.toString(participantsHealthInPercent) +
                ", collectedReagents=" + Arrays.toString(collectedReagents) +
                ", items=" + (items == null ? null : Arrays.asList(items)) +
                ", battleState=" + battleState +
                ", secureResult=" + secureResult +
                '}';
    }

    @Override
    public long getBattleId() {
        return battleId;
    }
}
