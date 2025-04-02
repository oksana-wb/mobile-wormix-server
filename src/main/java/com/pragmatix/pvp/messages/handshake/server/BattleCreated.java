package com.pragmatix.pvp.messages.handshake.server;

import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.PvpProfileStructure;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Serialize;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Arrays;

/**
 * Команда ответа от сервера, что соперник найден и можно грузить карту
 */
@Command(2006)
public class BattleCreated extends BattleCreatedStructure implements PvpCommandI, SecuredResponse  {

    public PreCalculatedPoints[] preCalculatedPoints;

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
        return "BattleCreated{" +
                "battleType=" + battleType +
                ", battleId=" + battleId +
                ", wager=" + wager +
                ", mapId=" + mapId +
                ", missionIds=" + Arrays.toString(missionIds) +
                ", questId=" + questId +
                ", participants=" +  Arrays.toString(participantStructs) +
                ", seed=" + seed +
                ", reagentsForBattle=" + Arrays.toString(reagentsForBattle) +
                ", preCalculatedPoints=" + Arrays.toString(preCalculatedPoints) +
                '}';
    }

}
