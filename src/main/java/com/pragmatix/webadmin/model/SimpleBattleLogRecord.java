package com.pragmatix.webadmin.model;

import com.pragmatix.app.common.BattleResultEnum;
import com.pragmatix.app.common.BossBattleResultType;

import java.time.Duration;
import java.time.LocalDateTime;

public record SimpleBattleLogRecord(
        LocalDateTime finishBattleTime,
        long profileId,
        Duration battleTime,
        BattleResultEnum result,
        short missionId,
        short assignedMissionId,
        long battleId,
        long assignedBattleId,
        String clientVersion,
        String serverVersion,
        short banType,
        String banNote,
        short currentMission,
        int bossWinAwardToken,
        BossBattleResultType bossBattleResultType,
        SimpleMissionLog missionLog
) {
    public boolean isValid(){
        return missionLog.valid;
    }
    
}
