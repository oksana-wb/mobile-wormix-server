package com.pragmatix.pvp.services.matchmaking;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.pvp.PvpAbstractTest;
import com.pragmatix.pvp.dsl.WagerDuelBattle;
import com.pragmatix.pvp.services.PvpService;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.10.13 22:10
 */
public class BlackListIntegrationTest extends PvpAbstractTest {

    @Resource
    private BlackListService blackListService;

    @Test
    public void testDuel() throws Exception {
        turnOffLobbyRestrict();

        WagerDuelBattle wagerDuelBattle = new WagerDuelBattle(binarySerializer);

        wagerDuelBattle.startBattle(user1Id, user2Id);
        wagerDuelBattle.finishBattle();
        Thread.sleep(1000);
        System.out.println("===============================================================");

        wagerDuelBattle.startBattle(user1Id, user3Id);
        wagerDuelBattle.finishBattle();
        Thread.sleep(1000);
        System.out.println("===============================================================");

        wagerDuelBattle.startBattle(user1Id, user4Id);
        wagerDuelBattle.finishBattle();
        Thread.sleep(1000);
        System.out.println("===============================================================");

        System.out.println(blackListService);
    }

    @Test
    public void test2Duel() throws Exception {
        turnOffLobbyRestrict();

        lobbyConf.setCheckLastOpponent(true);

        Long pvpUserId1 = PvpService.getPvpUserId(user1Id, (byte) 1);
        Long pvpUserId2 = PvpService.getPvpUserId(user2Id, (byte) 1);
        blackListService.registerBattleResult(pvpUserId1, pvpUserId2, (byte) PvpBattleResult.NOT_WINNER.getType());
        blackListService.registerBattleResult(pvpUserId1, pvpUserId2, (byte) PvpBattleResult.NOT_WINNER.getType());
        blackListService.registerBattleResult(pvpUserId1, pvpUserId2, (byte) PvpBattleResult.NOT_WINNER.getType());

        blackListService.getDailyTask().runServiceTask();

        WagerDuelBattle wagerDuelBattle = new WagerDuelBattle(binarySerializer);

        wagerDuelBattle.failureStartBattle(user1Id, user2Id);
    }
}
