package com.pragmatix.app.services;

import com.pragmatix.app.messages.server.ArenaResult;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.08.2016 12:47
 */
public class ArenaServiceTest extends AbstractSpringTest {

    @Resource
    ArenaService arenaService;

    @Test
    public void newArenaResult() throws Exception {
        HashMap<BattleWager, String[]> arenaWorkTime = new HashMap<>();
        arenaWorkTime.put(BattleWager.WAGER_300_DUEL, new String[] {"09:00", "10:00"});
        arenaWorkTime.put(BattleWager.WAGER_50_3_FOR_ALL, new String[] {"10:00", "11:00"});
        arenaService.setArenaWorkTime(arenaWorkTime);

        assertTrue(arenaService.isArenaLocked(BattleWager.WAGER_300_DUEL));
        assertTrue(arenaService.isArenaLocked(BattleWager.WAGER_50_3_FOR_ALL));

        ArenaResult arenaResult = arenaService.newArenaResult(getProfile(testerProfileId));
        println(arenaResult);
    }

}