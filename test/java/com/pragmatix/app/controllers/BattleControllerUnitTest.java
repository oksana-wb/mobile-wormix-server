package com.pragmatix.app.controllers;

import com.pragmatix.app.messages.client.DesyncLog;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 08.06.2016 11:37
 */
public class BattleControllerUnitTest {

    @Test
    public void onDesyncLogTest() throws Exception {
        BattleController battleController = new BattleController();
        UserProfile profile = new UserProfile(123L);
        DesyncLog msg = new DesyncLog();
        msg.battleId = 321;
        msg.battleType = PvpBattleType.WAGER_PvP_DUEL;
        msg.wager = BattleWager.WAGER_15_DUEL;
        msg.mapId = 15;
        msg.missionIds = new short[0];
        msg.battleLog = zipBytes("desyncLog", RandomStringUtils.randomAlphanumeric(1_000_000).getBytes("UTF-8"));

        battleController.onDesyncLog(msg, profile);
    }

    public static byte[] zipBytes(String filename, byte[] input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry entry = new ZipEntry(filename);
        entry.setSize(input.length);
        zos.putNextEntry(entry);
        zos.write(input);
        zos.closeEntry();
        zos.close();
        return baos.toByteArray();
    }
}
