package com.pragmatix.pvp.messages.handshake.server;

import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.messages.PvpProfileStructure;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Resize;
import com.pragmatix.serialization.annotations.Serialize;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Arrays;
import java.util.stream.Collectors;

@Structure
public class BattleCreatedStructure {
    /**
     * тип боя
     */
    public PvpBattleType battleType;
    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;
    /**
     * ставка игрока
     */
    public BattleWager wager;
    /**
     * id карты на которой будем играть
     */
    public int mapId;
    /**
     * id боссов для совместного прохождения
     */
    public short[] missionIds;
    /**
     * профайлы участников, хотит тот кто по индексу 0
     */
    public PvpProfileStructure[] participantStructs;
    /**
     * начальный счетчик рендома
     */
    public int seed;
    /**
     * массив id реагентов, которые могут выпасть в бою. длинной от 0-3
     * id могут повторяться
     */
    public byte[] reagentsForBattle;

    public short questId;

    @Override
    public String toString() {
        return "BattleCreatedStructure{" +
                "battleType=" + battleType +
                ", battleId=" + battleId +
                ", wager=" + wager +
                ", mapId=" + mapId +
                ", missionIds=" + Arrays.toString(missionIds) +
                ", questId=" + questId +
                ", seed=" + seed +
                ", reagentsForBattle=" + Arrays.toString(reagentsForBattle) +
                ", participants=\n" + Arrays.stream(participantStructs).map(PvpProfileStructure::mkString).collect(Collectors.joining("\n")) + "\n" +
                '}';
    }

}
