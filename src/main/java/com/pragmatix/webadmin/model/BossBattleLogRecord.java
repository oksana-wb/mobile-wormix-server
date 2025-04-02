package com.pragmatix.webadmin.model;

import com.pragmatix.app.common.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BossBattleLogRecord {
    public final LocalDateTime finishBattleTime;
    public final long profileId;
    public final short socialNetId;
    public final Duration battleDuration;
    public final String missionIds;
    public final long battleId;
    public final boolean win;
    public final int turnsCount;
    public final BossBattleResultType simpleResultType;
    public final Set<Integer> usedWeapons;
    public final Set<Race> usedRaces = new HashSet<>(4);
    public final Set<Short> usedHats = new HashSet<>(4);
    public final Set<Short> usedKits = new HashSet<>(4);
    public final List<PvpBattleStats.Unit> teamUnits;

    public static BossBattleLogRecord valueOf(SimpleBattleLogRecord logRecord) {
        return new BossBattleLogRecord(logRecord);
    }

    private BossBattleLogRecord(SimpleBattleLogRecord logRecord) {
        this.finishBattleTime = logRecord.finishBattleTime();
        this.profileId = logRecord.profileId();
        this.socialNetId = 0;
        this.battleDuration = logRecord.battleTime();
        this.missionIds = String.valueOf(logRecord.missionId());
        this.battleId = logRecord.battleId();
        this.win = logRecord.result() == BattleResultEnum.WINNER;
        this.simpleResultType = logRecord.bossBattleResultType();

        var simpleMissionLog = logRecord.missionLog();

        this.teamUnits = simpleMissionLog.teamSnapshot.stream().map(unitSnapshot -> {
            var unit = new PvpBattleStats.Unit();
            unit.id = unitSnapshot.id;
            unit.type = TeamMemberType.valueOf(unitSnapshot.type);
            unit.level = unitSnapshot.level;
            unit.armor = unitSnapshot.armor;
            unit.attack = unitSnapshot.attack;
            unit.race = Race.valueOf(unitSnapshot.race);
            unit.hat = unitSnapshot.hat;
            unit.kit = unitSnapshot.kit;
            return unit;
        }).toList();

        usedWeapons = simpleMissionLog.supposedTotalItems.stream().map(it -> it.weaponId).collect(Collectors.toSet());
        for (SimpleMissionLog.TeamUnit teamUnit : simpleMissionLog.teamSnapshot) {
            usedRaces.add(Race.valueOf(teamUnit.race));
            usedHats.add(teamUnit.hat);
            usedKits.add(teamUnit.kit);
        }
        usedHats.remove((short) 0);
        usedKits.remove((short) 0);

        this.turnsCount = simpleMissionLog.turns.getFirst().turnNum == 0 ? simpleMissionLog.turns.size() - 1 : simpleMissionLog.turns.size();
    }

    public static BossBattleLogRecord valueOf(PvpBattleStats.PvpBattleLogRecord logRecord, PvpBattleStats.Participant participant) {
        return new BossBattleLogRecord(logRecord, participant);
    }

    private BossBattleLogRecord(PvpBattleStats.PvpBattleLogRecord logRecord, PvpBattleStats.Participant participant) {
        this.finishBattleTime = logRecord.finish;
        this.battleDuration = logRecord.duration;
//        if (!logRecord.missionIds.isEmpty()) {
        this.missionIds = logRecord.missionIds.size() == 1 ? String.valueOf(logRecord.missionIds.getFirst()) : "%d_%d".formatted(logRecord.missionIds.getFirst(), logRecord.missionIds.getLast());
//        } else {
//            this.missionIds = null;
//        }
        this.battleId = logRecord.battleId;
        this.simpleResultType = null;

        this.profileId = participant.profile.profileId;
        this.socialNetId = participant.profile.socialNetId;
        this.win = participant.battleResult == PvpBattleResult.WINNER;
        this.teamUnits = participant.profile.units;

        this.turnsCount = logRecord.turnCount;

        if (logRecord.battleLog != null) {
            this.usedWeapons = logRecord.battleLog.turns.stream()
                    .flatMap(it -> it.shotsByWeapon.keySet().stream())
                    .collect(Collectors.toSet());
        } else {
            this.usedWeapons = Set.of();
        }
        for (PvpBattleStats.Participant it : logRecord.participants) {
            for (PvpBattleStats.Unit unit : it.profile.units) {
                usedRaces.add(unit.race);
                usedHats.add(unit.hat);
                usedKits.add(unit.kit);
            }
        }
        usedHats.remove((short) 0);
        usedKits.remove((short) 0);
    }

}
