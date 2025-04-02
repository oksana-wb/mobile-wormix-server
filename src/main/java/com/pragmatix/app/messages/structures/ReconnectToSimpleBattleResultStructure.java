package com.pragmatix.app.messages.structures;

import com.pragmatix.pvp.messages.handshake.client.ReconnectToBattle;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Resize;
import com.pragmatix.serialization.annotations.Structure;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 23.10.2017 12:51
 */
@Structure(nullable = true)
public class ReconnectToSimpleBattleResultStructure {

    @Resize(TypeSize.UINT32)
    public long battleId;

    @Resize(TypeSize.UINT32)
    public long originalBattleId;

    public short missionId;

    public short lastTurnNum;

    public ReconnectToSimpleBattleResultStructure() {
    }

    public ReconnectToSimpleBattleResultStructure(long battleId, long originalBattleId, short missionId, short lastTurnNum) {
        this.battleId = battleId;
        this.originalBattleId = originalBattleId;
        this.missionId = missionId;
        this.lastTurnNum = lastTurnNum;
    }

    @Override
    public String toString() {
        return "{" +
                "battleId=" + battleId +
                ", originalBattleId=" + originalBattleId +
                ", missionId=" + missionId +
                ", lastTurnNum=" + lastTurnNum +
                '}';
    }

}
