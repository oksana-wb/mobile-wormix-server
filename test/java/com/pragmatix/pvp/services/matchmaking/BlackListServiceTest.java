package com.pragmatix.pvp.services.matchmaking;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf;
import com.pragmatix.testcase.AbstractTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.10.13 17:50
 */
public class BlackListServiceTest extends AbstractTest {

    private BlackListService blackListService;

    @Before
    public void init() {
        blackListService = new BlackListService();
        blackListService.setLobbyConf(new LobbyConf());
        PersistenceService persistenceService = new PersistenceService();
        persistenceService.init();
        blackListService.setPersistenceService(persistenceService);
    }

    @Test
    public void testRegisterBattleResult() throws Exception {
        long userId = 1l;
        long candidatId = 2l;
        long candidat2Id = 3l;
        long candidat3Id = 4l;

        blackListService.registerBattleResult(userId, candidatId, (byte) PvpBattleResult.NOT_WINNER.getType());
        blackListService.registerBattleResult(userId, candidatId, (byte) PvpBattleResult.NOT_WINNER.getType());
        blackListService.registerBattleResult(userId, candidatId, (byte) PvpBattleResult.NOT_WINNER.getType());

        blackListService.registerBattleResult(userId, candidat3Id, (byte) PvpBattleResult.NOT_WINNER.getType());
        blackListService.registerBattleResult(userId, candidat3Id, (byte) PvpBattleResult.NOT_WINNER.getType());
        blackListService.registerBattleResult(userId, candidat3Id, (byte) PvpBattleResult.NOT_WINNER.getType());

        blackListService.registerBattleResult(userId, candidatId, (byte) PvpBattleResult.WINNER.getType());

        blackListService.registerBattleResult(userId, candidat2Id, (byte) PvpBattleResult.DRAW_GAME.getType());
        blackListService.registerBattleResult(userId, candidat2Id, (byte) PvpBattleResult.NOT_WINNER.getType());
        blackListService.registerBattleResult(userId, candidat2Id, (byte) PvpBattleResult.WINNER.getType());

        assertFalse(blackListService.isInBlackList(userId, candidatId));
        assertFalse(blackListService.isInBlackList(candidatId, userId));

        blackListService.getDailyTask().runServiceTask();

        assertTrue(blackListService.isInBlackList(userId, candidatId));
        assertFalse(blackListService.isInBlackList(userId, candidat2Id));
        assertFalse(blackListService.isInBlackList(candidatId, userId));

        // сохранение/восстановление

        blackListService.persistToDisk();
        blackListService.getBlackListsForUsers().clear();

        assertFalse(blackListService.isInBlackList(userId, candidatId));

        blackListService.init();
        assertTrue(blackListService.isInBlackList(userId, candidatId));
        assertTrue(blackListService.isInBlackList(userId, candidat3Id));

    }

}
