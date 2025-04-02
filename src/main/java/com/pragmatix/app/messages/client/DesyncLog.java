package com.pragmatix.app.messages.client;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.06.2016 11:16
 * @see com.pragmatix.app.controllers.BattleController#onDesyncLog(DesyncLog, UserProfile)
 */
@Command(134)
public class DesyncLog {

    @Resize(TypeSize.UINT32)
    public long battleId;

    public PvpBattleType battleType;

    public BattleWager wager;

    public int mapId;

    public short[] missionIds;

    public byte[] battleLog;

    @Override
    public String toString() {
        return "DesyncLog{" +
                "battleId=" + battleId +
                ", battleType=" + battleType +
                ", wager=" + wager +
                ", mapId=" + mapId +
                ", missionIds=" + Arrays.toString(missionIds) +
                ", battleLog(" + battleLog.length + ")" +
                '}';
    }

}
