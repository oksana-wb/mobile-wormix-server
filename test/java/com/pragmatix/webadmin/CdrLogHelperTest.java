package com.pragmatix.webadmin;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class CdrLogHelperTest {

    @Test
    public void parseBattlesStatLogTest() throws IOException {
        var date = LocalDate.parse("2024-10-05");
        var lines = FileUtils.readLines(new File("logs/cdr/battles_stat-2024-10-05.log"), StandardCharsets.UTF_8);
        var endBattleLogs = lines.stream()
                //.limit(1)
                .map(line -> CdrLogHelper.toSimpleBattleLogRecord(date, line, (battleResult, missionId) -> true))
                .toList();

        var line = endBattleLogs.get(endBattleLogs.size() - 1);
        System.out.println(line);
    }

    @Test
    public void parsePvpDetailsLogTest() throws IOException {
        var start = System.currentTimeMillis();
        var lines = FileUtils.readLines(new File("logs/cdr/pvp-details-2024-10-05.log"), StandardCharsets.UTF_8);
        System.out.println("read lines in time: " + (System.currentTimeMillis() - start)+" ms.");
        start = System.currentTimeMillis();
        var endBattleLogs = lines.stream()
//                .limit(1)
                .map(CdrLogHelper::toPvpBattleLogRecord)
                .toList();

        var line = endBattleLogs.get(endBattleLogs.size() - 1);
        System.out.println("parsed in time: " + (System.currentTimeMillis() - start)+" ms.");
        System.out.println(line);
    }

}