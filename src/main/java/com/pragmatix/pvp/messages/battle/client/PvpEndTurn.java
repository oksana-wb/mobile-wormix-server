package com.pragmatix.pvp.messages.battle.client;

import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.*;

import java.util.Arrays;

/**
 * Команда присылается в конце хода.
 * Говорит клиенту, что комманд больше не будет
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.06.11 10:57
 * @see com.pragmatix.pvp.controllers.PvpController#onPvpEndTurn(PvpEndTurn, com.pragmatix.pvp.model.PvpUser)
 *
 * @see PvpEndTurnResponse
 */
@Command(1014)
public class PvpEndTurn extends SecuredCommand implements CountedCommandI {

    public short turnNum;

    public short commandNum;
    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;

    public byte playerNum;
    /**
     * игроки выбывшие (перестали бегать, отошли с миром) во время хода
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
     * признак того что передача хода была инициировано сервером
     */
    public boolean forced = false;
    /**
     * во время боя была обнаружена попытка взлома
     */
    public short banType;
    /**
     * доп. информация по бану
     */
    public String banNote;
    /**
     * выбывшие юниты
     * структура: первым находится playerNum, следом количество выбитых этим игроком юнитов на этом ходу
     */
    public byte[] droppedUnits;
    /**
     * HP участников боя в % от первоночального
     */
    public short[] participantsHealthInPercent;

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "PvpEndTurn{" +
                "turnNum=" + turnNum +
                ", commandNum=" + commandNum +
                ", battleId=" + battleId +
                ", playerNum=" + playerNum +
                ", droppedPlayers=" + Arrays.toString(droppedPlayers) +
                ", collectedReagentsByUser=" + Arrays.toString(collectedReagents) +
                ", droppedUnitsByUser=" + Arrays.toString(droppedUnits) +
                ", participantsHealthInPercent=" + Arrays.toString(participantsHealthInPercent) +
                ", items=" + (items == null ? null : Arrays.asList(items)) +
                ", battleState=" + battleState +
                ", secureResult=" + secureResult +
                ", forced=" + forced +
                ", banType=" + banType +
                ", banNote='" + banNote + '\'' +
                '}';
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
        return playerNum;
    }

    @Override
    public long getBattleId() {
        return battleId;
    }

    public boolean isForced() {
        return forced;
    }

    public void setForced(boolean forced) {
        this.forced = forced;
    }
}
