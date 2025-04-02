package com.pragmatix.app.services;

import com.pragmatix.app.common.BattleResultEnum;
import com.pragmatix.app.common.BossBattleResultType;
import com.pragmatix.app.common.WhichLevelEnum;
import com.pragmatix.app.controllers.BattleController;
import com.pragmatix.app.messages.client.EndBattle;
import com.pragmatix.app.messages.client.EndTurn;
import com.pragmatix.app.messages.client.Login;
import com.pragmatix.app.messages.client.StartBattle;
import com.pragmatix.app.messages.server.EnterAccount;
import com.pragmatix.app.messages.server.StartBattleResult;
import com.pragmatix.app.messages.structures.*;
import com.pragmatix.app.model.SimpleBattleStateStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.settings.SimpleBattleSettings;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.pvp.PvpAbstractTest;
import com.pragmatix.pvp.PvpParticipant;
import com.pragmatix.pvp.dsl.PvePartnerBattle;
import com.pragmatix.pvp.dsl.WagerDuelBattle;
import io.netty.channel.Channel;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 07.04.2014 15:28
 */
public class BattleServiceTest extends PvpAbstractTest {

    @Resource
    BattleController battleController;

    @Resource
    CheatersCheckerService cheatersCheckerService;

    @Test
    public void pvpWinAwardTokenTest() throws Exception {
        turnOffLobbyRestrict();
        long[] profiles = new long[]{user1Id, user2Id};
        for(long profileId : profiles) {
            UserProfile profile = getProfile(profileId);
            profile.setLevel(30);
            profile.setMoney(1000);
            dailyRegistry.clearFor(profile.getId());
        }
        Map<PvpParticipant, Channel> mainChannelsMap = new WagerDuelBattle(binarySerializer)
                .startBattle(profiles[0], profiles[1])
                .winBattle(profiles[0]).mainChannelsMap;
        new WagerDuelBattle(binarySerializer, mainChannelsMap)
                .startBattle(profiles[0], profiles[1])
                .winBattle(profiles[0]);
        dailyRegistry.addWagerWinAwardToken(profiles[0], 2);
        new WagerDuelBattle(binarySerializer, mainChannelsMap)
                .startBattle(profiles[0], profiles[1])
                .winBattle(profiles[0]);
    }

    @Test
    public void pveBossWinAwardTokenTest() throws Exception {
        cheatersCheckerService.validateMissionLogTotals = false;

        long[] profiles = new long[]{testerProfileId, testerProfileId - 1};
        for(long profileId : profiles) {
            UserProfile profile = getProfile(profileId);
            profile.setLevel(30);
            profile.setLastBattleTime(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24));
            dailyRegistry.clearFor(profile.getId());

            profile.setCurrentNewMission((short) 101);
        }

        Map<PvpParticipant, Channel> mainChannelsMap = new PvePartnerBattle(binarySerializer, new short[]{102})
                .startBattle(profiles[0], profiles[1])
                .winBattle().mainChannelsMap;
        new PvePartnerBattle(binarySerializer, mainChannelsMap, new short[]{102})
                .startBattle(profiles[0], profiles[1])
                .winBattle();
        for(long profileId : profiles) {
            dailyRegistry.addBossWinAwardToken(profileId, 1);
        }
        new PvePartnerBattle(binarySerializer, mainChannelsMap, new short[]{102})
                .startBattle(profiles[0], profiles[1])
                .winBattle();
    }

    @Test
    public void simpleBossWinAwardTokenTest() throws Exception {
        cheatersCheckerService.validateMissionLogTotals = false;
        UserProfile profile = getProfile(testerProfileId);
        profile.setLevel(30);
        profile.setLastBattleTime(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24));
        dailyRegistry.clearFor(profile.getId());

        profile.setCurrentMission((short) 20);

        playBattle(profile, 21, BattleResultEnum.WINNER, BossBattleResultType.FIRST_WIN);
        playBattle(profile, 21, BattleResultEnum.WINNER, BossBattleResultType.NEXT_WIN_WITHOUT_TOKEN);
        dailyRegistry.addBossWinAwardToken(testerProfileId, 1);
        playBattle(profile, 22, BattleResultEnum.WINNER, BossBattleResultType.FIRST_WIN);
        dailyRegistry.addBossWinAwardToken(testerProfileId, 1);
        playBattle(profile, 22, BattleResultEnum.WINNER, BossBattleResultType.NEXT_WIN_WITH_TOKEN);
    }

    public void playBattle(UserProfile profile, int missionId, BattleResultEnum battleResultEnum, BossBattleResultType battleResultType) {
        Object resp = battleController.onStartBattle(new StartBattle(missionId), profile);
        println(resp);
        StartBattleResult startBattleResult = (StartBattleResult) resp;

        EndBattle endBattle = new EndBattle();
        endBattle.battleId = startBattleResult.battleId;
        endBattle.missionId = (short) missionId;
        endBattle.resultRaw = battleResultEnum.getType() + endBattle.battleId;

        profile.setStartBattleTime(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10));

        battleService.endBattle(profile, endBattle, new ArrayList<>());
        assertEquals(battleResultType, endBattle.battleAward.bossBattleResultType);
    }

    @Resource
    BattleService battleService;

    @Value("#{battleAwardSettings.awardSettingsMap}")
    Map<Short, SimpleBattleSettings> awardSettingsMap;

    @Test
    public void reconnectToMissionSuccessTest() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        profile.setBattlesCount(1);

        SimpleBattleStateStructure battleState = startBattleThenDisconnect(profile);

        Thread.sleep(1000L);

        short turnNum = (short) (battleState.lastTurnNum() + 1);
        ReconnectToSimpleBattleResultStructure reconnectResult = battleService.onLogin(profile, battleState.battleId, battleState.version, battleState.missionId, turnNum);
        println(reconnectResult);

        battleService.onSimpleBattleTurn(profile, newTurn(battleState.missionId, battleState.battleId, turnNum, true, 100));
    }

    @Test
    public void reconnectToMissionFailureTest() throws Exception {
        ReconnectToSimpleBattleResultStructure reconnectResult;
        SimpleBattleStateStructure battleState;
        short turnNum = 0;
        UserProfile profile = getProfile(testerProfileId);
        profile.setBattlesCount(10);

        battleState = startBattleThenDisconnect(profile);
        turnNum = (short) (battleState.lastTurnNum() + 1);
        reconnectResult = battleService.onLogin(profile, battleState.battleId - 1, battleState.version, battleState.missionId, turnNum);
        Assert.assertNull(reconnectResult);

        battleState = startBattleThenDisconnect(profile);
        turnNum = (short) (battleState.lastTurnNum() + 1);
        reconnectResult = battleService.onLogin(profile, battleState.battleId, battleState.version + 1, battleState.missionId, turnNum);
        Assert.assertNull(reconnectResult);

        battleState = startBattleThenDisconnect(profile);
        turnNum = (short) (battleState.lastTurnNum() + 1);
        reconnectResult = battleService.onLogin(profile, battleState.battleId, battleState.version, (short) 0, turnNum);
        Assert.assertNull(reconnectResult);

        battleState = startBattleThenDisconnect(profile);
        reconnectResult = battleService.onLogin(profile, battleState.battleId, battleState.version, battleState.missionId, (short) 0);
        Assert.assertNull(reconnectResult);

        battleState = startBattleThenDisconnect(profile);
        battleService.simpleBattleReconnectTimeoutInSeconds = 0;
        battleService.trackDisconnectedProfiles();

        reconnectResult = battleService.onLogin(profile, battleState.battleId, battleState.version, battleState.missionId, turnNum);
        Assert.assertNull(reconnectResult);

    }

    public SimpleBattleStateStructure startBattleThenDisconnect(UserProfile profile) throws InterruptedException {
        int version = 1;
        short missionId = (short) 21;

        profile.version = version;
        SimpleBattleSettings battleSettings = awardSettingsMap.get(missionId);
        long battleId = battleService.startSimpleBattle(profile, battleSettings, missionId, new ArrayList<>());
        boolean isPlayerTurn = true;
        int damageToBoss = 100;
        short turnNum = 1;

        battleService.onSimpleBattleTurn(profile, newTurn(missionId, battleId, turnNum++, isPlayerTurn, damageToBoss));
        battleService.onSimpleBattleTurn(profile, newTurn(missionId, battleId, turnNum++, isPlayerTurn, damageToBoss));
        battleService.onSimpleBattleTurn(profile, newTurn(missionId, battleId, turnNum++, isPlayerTurn, damageToBoss));

        battleService.onDisconnect(profile);

        SimpleBattleStateStructure battleState = battleService.disconnectedInSimpleBattle.get(profile.getId());
        Assert.assertNotNull(battleState);
        println(battleState);

        Thread.sleep(1000L);

        return battleState;
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
        return msg;
    }


    @Test
    public void testRefundOfflineBattles() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        profile.setBattlesCount(1);
        profile.setLogoutTime((int) (System.currentTimeMillis() / 1000l));
        loginMain(newOfflineBattle());
    }

    private EndBattleStructure newOfflineBattle() {
        EndBattleStructure battle = new EndBattleStructure();
        battle.startBattleTime = (int) (System.currentTimeMillis() / 1000l) - 30;
        battle.finishBattleTime = (int) (System.currentTimeMillis() / 1000l) - 10;
        battle.result = BattleResultEnum.WINNER;
        battle.type = WhichLevelEnum.MY_LEVEL;
        battle.expBonus = 1;
        battle.missionId = 0;
        return battle;
    }

    public void loginMain(EndBattleStructure... offlineBattles) throws Exception {
        connectMain();

        Login message = new Login();
        message.socialNet = SocialServiceEnum.vkontakte;
        message.id = testerProfileId;
//        message.authKey = AuthFilter.MASTER_AUTH_KEY;

        message.offlineBattles = offlineBattles;

        mainConnection.send(message);

        enterAccount = mainConnection.receive(EnterAccount.class, 1500);
        sessionId = enterAccount.sessionKey;
    }
}
