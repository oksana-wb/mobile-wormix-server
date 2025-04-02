package com.pragmatix.pvp.messages.handshake.server;

import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.PvpProfileStructure;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.11.12 12:12
 */
@Command(2002)
public class CallToBattle implements PvpCommandI {
    /**
     * кого зовем в бой
     */
    @Resize(TypeSize.UINT32)
    public long profileId;
    /**
     * тип боя
     */
    public BattleWager battleWager;
    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;
    /**
     * id карты на которой будем играть
     */
    @Resize(TypeSize.UINT32)
    public long mapId;
    /**
     * id боссов для совместного прохождения
     */
    public short[] missionIds;
    /**
     * профайлы участников боя, participantStructs[0] - создатель боя
     */
    public PvpProfileStructure[] participantStructs;

    public String clientParams;

    @Override
    public long getBattleId() {
        return battleId;
    }

    @Override
    public String toString() {
        return "CallToBattle{" +
                "profileId=" + profileId +
                ", battleWager=" + battleWager +
                ", battleId=" + battleId +
                ", mapId=" + mapId +
                ", missionIds=" + Arrays.toString(missionIds) +
                ", clientParams=" + clientParams +
                ", participantStructs=" + (participantStructs == null ? null : Arrays.asList(participantStructs)) +
                '}';
    }
}
