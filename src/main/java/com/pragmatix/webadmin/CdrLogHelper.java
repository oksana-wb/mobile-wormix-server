package com.pragmatix.webadmin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.pragmatix.app.common.BattleResultEnum;
import com.pragmatix.app.common.BossBattleResultType;
import com.pragmatix.webadmin.model.PvpBattleStats;
import com.pragmatix.webadmin.model.SimpleBattleLogRecord;
import com.pragmatix.webadmin.model.SimpleMissionLog;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.BiPredicate;

public class CdrLogHelper {

    private static ObjectMapper mapper = new ObjectMapper();

    static {
        var module = new SimpleModule();

        module.addDeserializer(Duration.class, new DurationDeserializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());

        mapper.registerModule(module);
    }

    public static LocalDateTime parseLocalDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime.replace(" ", "T"));
    }

    public static Duration parseTime(String time) {
        try {
            if (StringUtils.isBlank(time) || "0".equals(time) || ".0".equals(time)) {
                return Duration.ZERO;
            }
            if (time.contains(".")) {
                var ss = time.split(":");
                if (ss.length == 1) {
                    return Duration.parse("PT%sS".formatted(ss[0].startsWith(".") ? "0" + ss[0] : ss[0]));
                } else if (ss.length == 2) {
                    return Duration.parse("PT%dM%sS".formatted(Integer.parseInt(ss[0]), ss[1]));
                } else if (ss.length == 3) {
                    return Duration.parse("PT%dH%dM%sS".formatted(Integer.parseInt(ss[0]), Integer.parseInt(ss[1]), ss[2]));
                } else {
                    return Duration.ZERO;
                }

            }
            return Duration.parse(time.replaceAll("(\\d+):(\\d+):(\\d+)", "PT$1H$2M$3S"));
        } catch (Exception e) {
            throw new RuntimeException("source: " + time, e);
        }
    }

    public static PvpBattleStats.PvpBattleLogRecord toPvpBattleLogRecord(String line) {
        try {
            return mapper.readValue(line, PvpBattleStats.PvpBattleLogRecord.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SimpleBattleLogRecord toSimpleBattleLogRecord(LocalDate date, String line, BiPredicate<BattleResultEnum, Short> predicate) {
        var ss = line.split("\t");
        int i = 0;
        LocalDateTime finishBattleTime = LocalDateTime.of(date, LocalTime.parse(ss[i++]));
        i++;
        long profileId = Long.parseLong(ss[i++]);
        Duration battleTime = parseTime(ss[i++]);
        BattleResultEnum result = BattleResultEnum.valueOf(ss[i++]);
        short missionId = Short.parseShort(ss[i++]);
        if (!predicate.test(result, missionId)) {
            return null;
        }
        short assignedMissionId = Short.parseShort(ss[i++]);
        long battleId = Long.parseLong(ss[i++]);
        long assignedBattleId = Long.parseLong(ss[i++]);
        String clientVersion = ss[i++];
        String serverVersion = ss[i++];
        short banType = Short.parseShort(ss[i++]);
        String banNote = ss[i++];
        banNote = banNote.substring(0, banNote.length() - 2);
        SimpleMissionLog missionLog = null;
        try {
            missionLog = mapper.readValue(ss[i++], SimpleMissionLog.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        var sx = ss[i++].split("/");
        short currentMission = Short.parseShort(sx[0]);
        int bossWinAwardToken = Integer.parseInt(sx[1]);
        BossBattleResultType bossBattleResultType = BossBattleResultType.valueOf(ss[i]);

        return new SimpleBattleLogRecord(
                finishBattleTime,
                profileId,
                battleTime,
                result,
                missionId,
                assignedMissionId,
                battleId,
                assignedBattleId,
                clientVersion,
                serverVersion,
                banType,
                banNote,
                currentMission,
                bossWinAwardToken,
                bossBattleResultType,
                missionLog
        );
    }

}
