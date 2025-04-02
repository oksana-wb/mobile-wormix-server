package com.pragmatix.pvp;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.services.HeroicMissionService;
import com.pragmatix.app.settings.HeroicMissionState;
import com.pragmatix.pvp.messages.battle.client.PvpActionEx;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.server.PvpStartTurn;
import com.pragmatix.pvp.messages.handshake.client.ReadyForBattle;
import com.pragmatix.pvp.messages.handshake.server.BattleCreated;
import com.pragmatix.pvp.messages.handshake.server.CallToBattle;
import com.pragmatix.pvp.messages.handshake.server.StartPvpBattle;
import com.pragmatix.pvp.services.battletracking.PvpBattleStateEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import io.netty.channel.Channel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.11.12 15:36
 */
public class PveHeroicMissionTest extends AbstractSpringTest {

    private long user1Id = testerProfileId;
    private long user2Id = testerProfileId - 1;

    private PvpParticipant sender1;
    private PvpParticipant sender2;
    private PvpParticipant reciver1;
    private PvpParticipant reciver2;

    private List<PvpParticipant> battleParticipants;

    private static Map<PvpParticipant, Channel> mainChannelsMap = new HashMap<>();

    @Resource
    private HeroicMissionService heroicMissionService;

    @Before
    public void startBattle() throws Exception {
        StartTeamBattle startTeamBattle = new StartTeamBattle().invoke();
        sender1 = startTeamBattle.getSender1();
        sender2 = startTeamBattle.getSender2();
        reciver1 = startTeamBattle.getReciver1();
        reciver2 = startTeamBattle.getReciver2();
        battleParticipants = startTeamBattle.getBattleParticipants();
    }

    @After
    public void afterTest() throws InterruptedException {
        sender1.disconnectFromPvp();
        sender2.disconnectFromPvp();
        Thread.sleep(1000);
    }

    @AfterClass
    public static void after() throws InterruptedException {
        for(Channel channel : mainChannelsMap.values()) {
            if(channel.isActive()) {
                channel.disconnect();
            }
        }
        Thread.sleep(PvpBattleStateEnum.EndBattle.getStateTimeoutInSeconds() * 1000);
        Thread.sleep(1000);
    }
//============================================================================================================================================================================================

    /**
     * 1. игроки ходят по очереди
     * 2. 2 раза "ходят" боты
     * 3. ботов выносят - игрокам победа
     */
    @Test
    public void test_1() throws InterruptedException {
        sender1.sendActionEx();
        sender2.reciveFromPvp(PvpActionEx.class, 300);

        sender1.sendEndTurn();
        transferTurn(sender1, sender2);

        sender2.sendActionEx();
        sender1.reciveFromPvp(PvpActionEx.class, 300);

        sender2.sendEndTurn();
        transferTurn(sender2, reciver1);

        sender1.sendEndTurnResponse();
        sender2.sendEndTurnResponse();
        startTurn(reciver2);

        sender1.sendEndTurnResponse();
        sender2.sendEndTurnResponse();
        startTurn(sender1);

        sender1.sendEndTurn(reciver1, reciver2);
        confirmTurnTransfer(sender1);

        sender1.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        sender2.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);

        sender1.disconnectFromPvp();
        sender2.disconnectFromPvp();
        Thread.sleep(1000);

        for(Channel channel : mainChannelsMap.values()) {
            if(channel.isActive()) {
                channel.disconnect();
            }
        }
        Thread.sleep(1000);
    }

//============================================================================================================================================================================================

    private void transferTurn(PvpParticipant inTurn, PvpParticipant nextTurn) {
        confirmTurnTransfer(inTurn);

        startTurn(nextTurn);
    }

    private void startTurn(PvpParticipant nextTurn) {
        for(PvpParticipant user : battleParticipants) {
            if(!user.isEndBattle()) {
                PvpStartTurn startTurn = user.reciveFromPvp(PvpStartTurn.class, 300);
                assertEquals(startTurn.turningPlayerNum, nextTurn.getPlayerNum());
                user.onStartTurn(startTurn);
            }
        }
    }

    private void confirmTurnTransfer(PvpParticipant inTurn) {
        for(PvpParticipant user : battleParticipants) {
            if(!user.isEndBattle() && !user.equals(inTurn)) {
                PvpEndTurn pvpEndTurn = user.reciveFromPvp(PvpEndTurn.class, 300);
                user.confirmTurnTransfer(pvpEndTurn);
            }
        }
    }

    private class StartTeamBattle {
        private PvpParticipant sender1;
        private PvpParticipant sender2;
        private List<PvpParticipant> battleParticipants;

        public PvpParticipant getSender1() {
            return sender1;
        }

        public PvpParticipant getSender2() {
            return sender2;
        }

        public PvpParticipant getReciver1() {
            return reciver1;
        }

        public PvpParticipant getReciver2() {
            return reciver2;
        }

        public List<PvpParticipant> getBattleParticipants() {
            return battleParticipants;
        }

        public StartTeamBattle invoke() throws Exception {
            PvpParticipant user1_1 = new PvpParticipant(user1Id, binarySerializer);
            PvpParticipant user1_2 = new PvpParticipant(user2Id, binarySerializer);
            battleParticipants = Arrays.asList(user1_1, user1_2);

            for(PvpParticipant user : battleParticipants) {
                if(mainChannelsMap.containsKey(user) && mainChannelsMap.get(user).isActive()) {
                    user.setMainChannel(mainChannelsMap.get(user));
                } else {
                    user.loginMain();
                    mainChannelsMap.put(user, user.getMainChannel());
                }

                user.connectToPvp();
            }

            HeroicMissionState state = heroicMissionService.getHeroicMissionStates()[0];
            user1_1.sendCreatePveFriendBattleRequest(state.getMissionIds(), state.getMapId(), user1_2.getUserId());

            CallToBattle callToBattle = user1_2.reciveFromMain(CallToBattle.class, 1000);
            assertEquals(2, callToBattle.participantStructs.length);
            user1_2.sendJoinToBattle(callToBattle);

            callToBattle = user1_1.reciveFromPvp(CallToBattle.class, 1000);
            assertEquals(2, callToBattle.participantStructs.length);

            for(PvpParticipant user : battleParticipants) {
                BattleCreated battleCreated = user.reciveFromPvp(BattleCreated.class, 1000);
                user.sendToPvp(new ReadyForBattle(battleCreated.getBattleId()));
            }

            for(PvpParticipant user : battleParticipants) {
                StartPvpBattle startPvpBattle = user.reciveFromPvp(StartPvpBattle.class, 300);
                user.onStartBattle(startPvpBattle);
            }

            sender1 = user1_1;
            sender2 = user1_2;
            reciver1 = new PvpParticipant(2);
            reciver2 = new PvpParticipant(3);
            return this;
        }
    }

}
