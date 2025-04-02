package com.pragmatix.app.controllers;

import com.pragmatix.app.common.BattleResultEnum;
import com.pragmatix.app.common.CheatTypeEnum;
import com.pragmatix.app.messages.client.EndBattle;
import com.pragmatix.app.messages.client.EndTurn;
import com.pragmatix.app.messages.client.Ping;
import com.pragmatix.app.messages.client.StartBattle;
import com.pragmatix.app.messages.server.ArenaLocked;
import com.pragmatix.app.messages.server.EndBattleResult;
import com.pragmatix.app.messages.server.StartBattleResult;
import com.pragmatix.app.messages.structures.*;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.BanService;
import com.pragmatix.app.services.BattleService;
import com.pragmatix.app.services.DailyRegistry;
import com.pragmatix.app.services.WeaponService;
import com.pragmatix.app.settings.BossBattleSettings;
import com.pragmatix.craft.domain.ReagentsEntity;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.pvp.services.matchmaking.GroupHpService;
import com.pragmatix.testcase.AbstractSpringTest;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Test;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.pragmatix.app.common.BattleResultEnum.NOT_WINNER;
import static com.pragmatix.app.common.BattleResultEnum.WINNER;
import static com.pragmatix.app.common.WhichLevelEnum.MY_LEVEL;
import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.01.12 13:17
 */
public class BattleControllerTest extends AbstractSpringTest {

    @Resource
    private BattleController battleController;

    @Resource
    private SoftCache softCache;

    // миссия по умолчанию во всех тестах
    private int bossMissionId = 1;
    @Resource(name = "bossMission_01")
    private BossBattleSettings bossBattleSettings;

    private int difficultBossMissionId = 25;
    @Resource(name = "bossMission_25")
    private BossBattleSettings difficultBossBattleSettings;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private WeaponService weaponService;

    @Resource
    private BattleService battleService;

    @Resource
    private GroupHpService groupHpService;

    @Resource
    private BanService banService;

    @Test
    public void testRareAward() throws Exception {
        userProfileCreator.wipeUserProfile(getProfile(testerProfileId));
        UserProfile profile = getProfile(testerProfileId);
        profile.setLevel(difficultBossBattleSettings.getMinLevel());
        profile.setTemporalStuff(ArrayUtils.EMPTY_BYTE_ARRAY);
        profile.setHat((short)0);
        profile.setKit((short)0);

//        loginMain();

        //прошли босса
        int oldMoney = profile.getMoney();
        int oldExp = profile.getExperience();

        profile.setStuff(new short[]{(short)5001});

        dailyRegistry.clearFor(testerProfileId);
        profile.setCurrentMission((short) (difficultBossMissionId - 1));
        profile.setBattlesCount(5);

        winBattle(profile);
        winBattle(profile);

        dailyRegistry.clearFor(testerProfileId);
        winBattle(profile);

        dailyRegistry.clearFor(testerProfileId);
        profile.setStuff(new short[0]);
        winBattle(profile);

        dailyRegistry.clearFor(testerProfileId);
        profile.setStuff(new short[]{(short)1606});
        winBattle(profile);
    }

    private void winBattle(UserProfile profile) {
        Object resp = battleController.onStartBattle(new StartBattle(difficultBossMissionId), profile);
        assertTrue(resp instanceof StartBattleResult);
        StartBattleResult startBattleResult = (StartBattleResult) resp;
        long battleId = startBattleResult.battleId;
        assertTrue(startBattleResult.reagentsForBattle.length == 0);

        EndBattle msg = newEndBattle(profile, (int) battleId, difficultBossMissionId, 1, WINNER);
        List<GenericAwardStructure> award = new ArrayList<>();
        EndBattleResult.EndBattleValidateResult endBattleValidateResult = battleService.endBattle(profile, msg, award);
        assertEquals(EndBattleResult.EndBattleValidateResult.OK, endBattleValidateResult);
        println(award);
    }

    @Test
    public void testOnStartSimpleBattle() throws Exception {
        loginMain();
        StartBattle startBattle = new StartBattle();
        startBattle.missionId = 0;
        sendMain(startBattle);
        StartBattleResult startBattleResult = receiveMain(StartBattleResult.class);
        Thread.sleep(1000 * 6);

        EndBattle endBattle = new EndBattle();
        endBattle.type = MY_LEVEL;
        endBattle.battleId = (int) startBattleResult.battleId;
        endBattle.resultRaw = endBattle.battleId + WINNER.getType();
        endBattle.banNote = "";
        endBattle.missionId = 0;
        endBattle.items = new BackpackItemStructure[0];
        endBattle.collectedReagents = new byte[0];
        sendMain(endBattle);
    }

//    @Test
//    public void testOnStartBattle() throws Exception {
//        userProfileCreator.wipeUserProfile(getProfile(testerProfileId));
//        UserProfile profile = getProfile(testerProfileId);
//        profile.setCurrentMission((short) 0);
//        profile.setBattlesCount(5);
//
//        Object resp = battleController.onStartBattle(new StartBattle(Short.MAX_VALUE), profile);
//        assertTrue(resp instanceof ArenaLocked);
//        assertEquals(ArenaLocked.CAUSE_WRONG_MISSION, ((ArenaLocked) resp).causeCode);
//
//        profile.setCurrentMission((short) 0);
//        profile.setBattlesCount(5);
//        resp = battleController.onStartBattle(new StartBattle(-1), profile);
//        assertTrue(resp instanceof StartBattleResult);
//
//        profile.setCurrentMission((short) -1);
//        profile.setBattlesCount(5);
//        resp = battleController.onStartBattle(new StartBattle(-2), profile);
//        assertTrue(resp instanceof StartBattleResult);
//
//        profile.setCurrentMission((short) -2);
//        profile.setBattlesCount(5);
//        resp = battleController.onStartBattle(new StartBattle(-2), profile);
//        assertTrue(resp instanceof ArenaLocked);
//        assertEquals(ArenaLocked.CAUSE_MISSION_LOCKED, ((ArenaLocked) resp).causeCode);
//
//        profile.setCurrentMission((short) -2);
//        profile.setBattlesCount(5);
//        profile.setLevel(1);
//        resp = battleController.onStartBattle(new StartBattle(bossMissionId), profile);
//        assertEquals(ArenaLocked.CAUSE_MISSION_LOCKED, ((ArenaLocked) resp).causeCode);
//
//        profile.setLevel(bossBattleSettings.getMinLevel());
//        resp = battleController.onStartBattle(new StartBattle(1), profile);
//        assertTrue(resp instanceof StartBattleResult);
//
//        profile.setCurrentMission((short) 1);
//        profile.setBattlesCount(5);
//        resp = battleController.onStartBattle(new StartBattle(2), profile);
//        assertTrue(resp instanceof StartBattleResult);
//
//        profile.setCurrentMission((short) 0);
//        profile.setBattlesCount(5);
//        resp = battleController.onStartBattle(new StartBattle(2), profile);
//        assertTrue(resp instanceof ArenaLocked);
//        assertEquals(ArenaLocked.CAUSE_MISSION_LOCKED, ((ArenaLocked) resp).causeCode);
//
//        profile.setCurrentMission((short) 1);
//        profile.setBattlesCount(5);
//        resp = battleController.onStartBattle(new StartBattle(3), profile);
//        assertTrue(resp instanceof ArenaLocked);
//        assertEquals(ArenaLocked.CAUSE_MISSION_LOCKED, ((ArenaLocked) resp).causeCode);
//
//        profile.setCurrentMission((short) 1);
//        profile.setBattlesCount(5);
//        resp = battleController.onStartBattle(new StartBattle(-1), profile);
//        assertTrue(resp instanceof ArenaLocked);
//        assertEquals(ArenaLocked.CAUSE_MISSION_LOCKED, ((ArenaLocked) resp).causeCode);
//
//    }

    @Test
    public void testOnEndBattle() throws Exception {
        userProfileCreator.wipeUserProfile(getProfile(testerProfileId));
        UserProfile profile = getProfile(testerProfileId);
        profile.setLevel(bossBattleSettings.getMinLevel());

        loginMain();

        //прошли босса
        int oldMoney = profile.getMoney();
        int oldExp = profile.getExperience();

        dailyRegistry.clearFor(testerProfileId);
        profile.setCurrentMission((short) 0);
        profile.setBattlesCount(5);
        Object resp = battleController.onStartBattle(new StartBattle(bossMissionId), profile);
        assertTrue(resp instanceof StartBattleResult);
        long battleId = ((StartBattleResult) resp).battleId;

        int bossHp = 300;
        battleController.onEndTurn(newZeroTurn(bossMissionId, battleId, groupHpService.calculateGroupHp(profile.getUserProfileStructure()), bossHp), profile);
        int turnNum = 0;
        battleController.onEndTurn(newTurn(bossMissionId, battleId, ++turnNum, true, bossHp), profile);
        battleController.onEndTurn(newTurn(bossMissionId, battleId, ++turnNum, false, 0), profile);

        EndBattle msg = newEndBattle(profile, (int) battleId, bossMissionId, turnNum, WINNER);
        msg.totalDamageToBoss = bossHp;
        battleController.onEndBattle(msg, profile);

        assertEquals(oldMoney + bossBattleSettings.getFirstWinBattleAward().getMoney(), profile.getMoney());
        assertEquals(oldExp + bossBattleSettings.getFirstWinBattleAward().getExperience(), profile.getExperience());
        assertTrue(dailyRegistry.isSuccessedMission(testerProfileId));

        // не прошли босса
        oldMoney = profile.getMoney();
        oldExp = profile.getExperience();

        dailyRegistry.clearFor(testerProfileId);
        profile.setCurrentMission((short) 0);
        profile.setBattlesCount(5);
        resp = battleController.onStartBattle(new StartBattle(1), profile);
        assertTrue(resp instanceof StartBattleResult);
        battleId = ((StartBattleResult) resp).battleId;

        msg = newEndBattle(profile, (int) battleId, bossMissionId, 1, NOT_WINNER);
        battleController.onEndBattle(msg, profile);

        assertEquals(oldMoney + bossBattleSettings.getNotWinnerMoney(), profile.getMoney());
        assertEquals(oldExp + bossBattleSettings.getNotWinnerExp(), profile.getExperience());
        assertFalse(dailyRegistry.isSuccessedMission(testerProfileId));
    }

    @Test
    public void testBossValidationFail() throws Exception {
        userProfileCreator.wipeUserProfile(getProfile(testerProfileId));
        UserProfile profile = getProfile(testerProfileId);
        profile.setLevel(bossBattleSettings.getMinLevel());
        profile.setHat((short) 1000);
        profile.setKit((short) 2001);

        loginMain();

        profile.setCurrentMission((short) 0);
        profile.setBattlesCount(5);
        Object resp = battleController.onStartBattle(new StartBattle(bossMissionId), profile);
        assertTrue(resp instanceof StartBattleResult);
        long battleId = ((StartBattleResult) resp).battleId;

        int bossHp = 300;
        battleController.onEndTurn(newZeroTurn(bossMissionId, battleId, groupHpService.calculateGroupHp(profile.getUserProfileStructure()), bossHp), profile);
        int turnNum = 0;
        battleController.onEndTurn(newTurn(bossMissionId, battleId, ++turnNum, true, bossHp / 4), profile);
        battleController.onEndTurn(newTurn(bossMissionId, battleId, ++turnNum, true, bossHp / 4), profile);
        battleController.onEndTurn(newTurn(bossMissionId, battleId, ++turnNum, true, bossHp / 4), profile);
        battleController.onEndTurn(newTurn(bossMissionId, battleId, ++turnNum, true, bossHp / 4), profile);
        battleController.onEndTurn(newTurn(bossMissionId, battleId, ++turnNum, false, 0), profile);

        EndBattle msg = newEndBattle(profile, (int) battleId, bossMissionId, turnNum + 10, WINNER);
        msg.totalDamageToBoss = bossHp;
        battleController.onEndBattle(msg, profile);
        // не дал боссу сходить => должен быть забанен
        assertTrue("Should be banned", banService.isBanned(profile.getId()));
        banService.remove(profile.getId());
    }

    @Test
    public void testBossValidationFastWin() throws Exception {
        userProfileCreator.wipeUserProfile(getProfile(testerProfileId));
        UserProfile profile = getProfile(testerProfileId);

        profile.setLevel(difficultBossBattleSettings.getMinLevel());
        profile.setHat((short) 1000);
        profile.setKit((short) 2001);

        loginMain();

        profile.setCurrentMission((short) (difficultBossMissionId-1));
        profile.setBattlesCount(5);

        Object resp = battleController.onStartBattle(new StartBattle(difficultBossMissionId), profile);
        assertTrue(resp.toString(), resp instanceof StartBattleResult);
        long battleId = ((StartBattleResult) resp).battleId;

        int bossHp = 300;
        battleController.onEndTurn(newZeroTurn(difficultBossMissionId, battleId, groupHpService.calculateGroupHp(profile.getUserProfileStructure()), bossHp), profile);
        int turnNum = 0;
        battleController.onEndTurn(newTurn(difficultBossMissionId, battleId, ++turnNum, true, bossHp / 2), profile);
        Thread.sleep(4000);
        battleController.onEndTurn(newTurn(difficultBossMissionId, battleId, ++turnNum, false, 0), profile);
        Thread.sleep(4000);
        battleController.onEndTurn(newTurn(difficultBossMissionId, battleId, ++turnNum, true, bossHp / 2), profile);
        Thread.sleep(3000);
        battleController.onEndTurn(newTurn(difficultBossMissionId, battleId, ++turnNum, false, 0), profile);
        MissionLogStructure mLog = profile.getMissionLog();
        assertNotNull(mLog);
        EndBattle msg = newEndBattle(profile, (int) battleId, difficultBossMissionId, turnNum, WINNER);
        msg.totalDamageToBoss = bossHp;
        battleController.onEndBattle(msg, profile);
        // за прохождение быстрее 3 минут - попадаешь в репорт
        assertEquals(CheatTypeEnum.FAST_WIN, mLog.getReason());

        mainConnection.send(new Ping());
        // NB: но не на простых боссах
        dailyRegistry.clearMission(testerProfileId);
        resp = battleController.onStartBattle(new StartBattle(bossMissionId), profile);
        assertTrue(resp.toString(), resp instanceof StartBattleResult);
        battleId = ((StartBattleResult) resp).battleId;
        battleController.onEndTurn(newZeroTurn(bossMissionId, battleId, groupHpService.calculateGroupHp(profile.getUserProfileStructure()), bossHp), profile);
        turnNum = 0;
        battleController.onEndTurn(newTurn(bossMissionId, battleId, ++turnNum, true, bossHp), profile);
        Thread.sleep(11000);
        battleController.onEndTurn(newTurn(bossMissionId, battleId, ++turnNum, false, 0), profile);
        mLog = profile.getMissionLog();
        assertNotNull(mLog);
        msg = newEndBattle(profile, (int) battleId, bossMissionId, turnNum, WINNER);
        msg.totalDamageToBoss = bossHp;
        battleController.onEndBattle(msg, profile);
        System.out.println(mLog.getReason());
        assertNotEquals(CheatTypeEnum.FAST_WIN, mLog.getReason());
    }

    private EndBattle newEndBattle(UserProfile profile, long battleId, int missionId, int turnsCount, BattleResultEnum result) {
        EndBattle msg = new EndBattle();
        msg.battleId = battleId;
        msg.missionId = (short) missionId;
        msg.type = MY_LEVEL;
        msg.resultRaw = result.getType() + battleId;
        msg.items = new BackpackItemStructure[0];
        msg.totalUsedItems = msg.items;
        msg.collectedReagents = new byte[0];
        msg.banNote = "";
        msg.totalTurnsCount = (short) turnsCount;
        return msg;
    }

    private EndTurn newZeroTurn(int missionId, long battleId, int playerHP, int bossHP) {
        TurnStructure turn = new TurnStructure(0, false,
        /*used=*/   new BackpackItemStructure[]{},
        /*deaths=*/ new BossWormStructure[]{},
        /*births=*/ new BossWormStructure[]{
                new BossWormStructure(true, playerHP),
                new BossWormStructure(false, bossHP)
        });
        EndTurn msg = new EndTurn();
        msg.missionId = (short) missionId;
        msg.battleId = battleId;
        msg.turn = turn;
        return msg;
    }

    private EndTurn newTurn(int missionId, long battleId, int turnNum, boolean isPlayerTurn, int damageToBoss) {
        TurnStructure turn = new TurnStructure(turnNum, isPlayerTurn,
        /*used=*/   new BackpackItemStructure[]{},
        /*deaths=*/ new BossWormStructure[]{},
        /*births=*/ new BossWormStructure[]{});
        turn.damageToBoss = damageToBoss;
        EndTurn msg = new EndTurn();
        msg.missionId = (short) missionId;
        msg.battleId = battleId;
        msg.turn = turn;
        System.out.println("Turn #" + turnNum);
        return msg;
    }

    @Test
    public void testLimitShotCountForMission() throws Exception {
        UserProfile profile = softCache.get(UserProfile.class, testerProfileId);
        int weaponId = 70;
        weaponService.removeWeapon(profile, weaponId);
        assertNull(profile.getBackpackItemByWeaponId(weaponId));

        int MISSION_AWARD_MAX_SHOT_COUNT = 10;
        weaponService.addOrUpdateWeapon(profile, weaponId, MISSION_AWARD_MAX_SHOT_COUNT - 1);
        assertNotNull(profile.getBackpackItemByWeaponId(weaponId));
        assertEquals(MISSION_AWARD_MAX_SHOT_COUNT - 1, profile.getBackpackItemByWeaponId(weaponId).getCount());

        weaponService.addOrUpdateWeaponReturnCount(profile, weaponId, 10, MISSION_AWARD_MAX_SHOT_COUNT);
        assertEquals(MISSION_AWARD_MAX_SHOT_COUNT + 9, profile.getBackpackItemByWeaponId(weaponId).getCount());

        weaponService.addOrUpdateWeaponReturnCount(profile, weaponId, 10, MISSION_AWARD_MAX_SHOT_COUNT);
        assertEquals(MISSION_AWARD_MAX_SHOT_COUNT + 9, profile.getBackpackItemByWeaponId(weaponId).getCount());

        weaponService.removeWeapon(profile, weaponId);
        weaponService.addOrUpdateWeapon(profile, weaponId, MISSION_AWARD_MAX_SHOT_COUNT);
        weaponService.addOrUpdateWeaponReturnCount(profile, weaponId, 1, MISSION_AWARD_MAX_SHOT_COUNT);
        assertEquals(MISSION_AWARD_MAX_SHOT_COUNT, profile.getBackpackItemByWeaponId(weaponId).getCount());

    }

    @Test
    public void testCollectedReagents() {
        UserProfile profile = getProfile(testerProfileId);
        profile.setReagents(new ReagentsEntity(testerProfileId));
        profile.setReagentsForBattle(new byte[]{1, 1, -1});
        EndBattle msg = new EndBattle();
        msg.collectedReagents = new byte[]{1, 1, 1, 4};
        battleService.setOffBattleReagents(msg.battleId, msg.collectedReagents, profile, msg.battleAward);

        int[] values = profile.getReagents().getValues();
        assertEquals(2, values[1]);
        assertEquals(0, values[4]);
    }

    @After
    public void finallyDisconnect() throws Exception {
        if(mainConnection != null) {
            disconnectMain();
        }
    }
}
