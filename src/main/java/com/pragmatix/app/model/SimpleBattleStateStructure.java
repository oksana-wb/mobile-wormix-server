package com.pragmatix.app.model;

import com.pragmatix.app.common.BattleState;
import com.pragmatix.app.messages.structures.MissionLogStructure;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Optional;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 23.10.2017 8:41
 */
@Structure
public class SimpleBattleStateStructure {

    public int version;

    public long battleId;

    public long originalBattleId;

    public short missionId;

    public long lastProcessedBattleId;

    public int startBattleTime;

    public int disconnectTime;

    public byte[] reagentsForBattle;

    public MissionLogStructure missionLog;

    public SimpleBattleStateStructure() {
    }

    public SimpleBattleStateStructure(UserProfile profile, int disconnectTime) {
        this.version = profile.version;
        this.battleId = profile.getBattleId();
        this.originalBattleId = profile.getBattleId();
        this.missionId = profile.getMissionId();
        this.lastProcessedBattleId = profile.getLastProcessedBattleId();
        this.startBattleTime = (int) (profile.getStartBattleTime() / 1000L);
        this.disconnectTime = disconnectTime;
        this.reagentsForBattle = profile.getReagentsForBattle();
        this.missionLog = profile.getMissionLog();
    }

    public void mergeTo(UserProfile profile) {
        profile.setBattleState(BattleState.SIMPLE);
        profile.setBattleId(this.battleId);
        profile.setMissionId(this.missionId);
        profile.setLastProcessedBattleId(this.lastProcessedBattleId);
        profile.setStartBattleTime(this.startBattleTime * 1000L);
        profile.setReagentsForBattle(this.reagentsForBattle);
        profile.setMissionLog(this.missionLog);
    }

    @Override
    public String toString() {
        return "{" +
                "version=" + version +
                ", battleId=" + battleId +
//                ", originalBattleId=" + originalBattleId +
                ", missionId=" + missionId +
//                ", lastProcessedBattleId=" + lastProcessedBattleId +
                ", startBattleTime=" + AppUtils.formatDateInSeconds(startBattleTime) +
                ", disconnectTime=" + AppUtils.formatDateInSeconds(disconnectTime) +
//                ", reagentsForBattle=" + Arrays.toString(reagentsForBattle) +
//                ", missionLog=" + missionLog +
                ", lastTurnNum=" + lastTurnNum() +
                '}';
    }

    public short lastTurnNum() {
        return Optional.ofNullable(missionLog).filter(log -> log.turns.size() > 0).map(log -> log.turns.get(log.turns.size() - 1).turnNum).orElse((short) -1);
    }
}
