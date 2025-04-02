package com.pragmatix.pvp;

import com.pragmatix.app.common.PvpBattleResult;
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
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.11.12 15:36
 */
public class PveFriendTest extends AbstractSpringTest {

    public static final int DEFAULT_DELAY = 300;
//    public static final int DEFAULT_DELAY = Integer.MAX_VALUE;

    private long user1Id = testerProfileId;
    private long user2Id = testerProfileId - 1;

    private PvpParticipant sender1;
    private PvpParticipant sender2;
    private PvpParticipant reciver1;
    private PvpParticipant reciver2;

    private List<PvpParticipant> battleParticipants;

    private static Map<PvpParticipant, Channel> mainChannelsMap = new HashMap<>();

    @Before
    public void startBattle() throws Exception {
        StartTeamBattle startTeamBattle = new StartTeamBattle().invoke();
        sender1 = startTeamBattle.getSender1();
        sender2 = startTeamBattle.getSender2();
        reciver1 = startTeamBattle.getReciver1();
        reciver2 = startTeamBattle.getReciver2();
        battleParticipants = startTeamBattle.getBattleParticipants();
    }

//    @After
//    public void afterTest() throws InterruptedException {
//        sender1.disconnectFromPvp();
//        sender2.disconnectFromPvp();
//        Thread.sleep(1000);
//    }
//
//    @AfterClass
//    public static void after() throws InterruptedException {
//        for(Channel channel : mainChannelsMap.values()) {
//            if(channel.isConnected()) {
//                channel.disconnect();
//            }
//        }
//        Thread.sleep(PvpBattleStateEnum.EndBattle.getStateTimeoutInSeconds() * 1000);
//        Thread.sleep(1000);
//    }
//============================================================================================================================================================================================

    /**
     * 1. 1-й игрок сдаётся
     * 2. 2-й топится
     */
    @Test
    public void test_0() throws InterruptedException {
        // 1)
        sender1.sendActionEx();
        sender2.reciveFromPvp(PvpActionEx.class, DEFAULT_DELAY);

        sender1.surrender();
        sender1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, DEFAULT_DELAY, false);

        //2)
        transferTurn(sender1, sender2);

        sender2.sendActionEx();

        sender2.sendEndTurn(sender2);
        sender2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, DEFAULT_DELAY, true);
    }

    /**
     * 1. игроки ходят по очереди
     * 2. 2 раза "ходят" боты
     * 3. ботов выносят - игрокам победа
     */
    @Test
    public void test_1() throws InterruptedException {
        sender1.sendActionEx();
        sender2.reciveFromPvp(PvpActionEx.class, DEFAULT_DELAY);

        sender1.sendEndTurn();
        transferTurn(sender1, sender2);

        sender2.sendActionEx();
        sender1.reciveFromPvp(PvpActionEx.class, DEFAULT_DELAY);

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

        sender1.finishBattleWithResult(PvpBattleResult.WINNER, DEFAULT_DELAY, true);
        sender2.finishBattleWithResult(PvpBattleResult.WINNER, DEFAULT_DELAY, true);

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

    /**
     * 1. начинает ходить 1-и игрок и сдается - получает сразу поражение
     * 2. ход получает 2-й игрок
     * 3. 2 раза "ходит" бот
     * 4. 2-й игрок на первом ходе выносит 1-го бота
     * 5. "ходит" 2-й бот и выносит 2-го игрока
     * 6. 2-му игроку поражение
     */
    @Test
    public void test_2() throws InterruptedException {
        sender1.sendActionEx();
        sender2.reciveFromPvp(PvpActionEx.class, DEFAULT_DELAY);

        sender1.surrender();
        sender1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, DEFAULT_DELAY, false);
        // 2)
        transferTurn(sender1, sender2);

        sender2.sendActionEx();

        sender2.sendEndTurn();
        startTurn(reciver1);
        // 3)
        sender2.sendEndTurnResponse();
        startTurn(reciver2);

        sender2.sendEndTurnResponse();
        startTurn(sender2);
        // 4)
        sender2.sendEndTurn(reciver1);
        startTurn(reciver2);
        // 5)
        sender2.sendEndTurnResponse(sender2);
        // 6)
        sender2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, DEFAULT_DELAY, true);
    }

    /**
     * 1. игроки ходят по очереди
     * 2. 2-й игрок не подтверждает "ход" 1-го бота - порожение после таймаута
     * 3. 1-й игрок подтверждает "ход" 2-го бота - порожение после таймаута
     * 4. 1-й игрок выносит обоих ботов - победа
     */
    @Test
    public void test_3() throws InterruptedException {
        sender1.sendEndTurn();
        transferTurn(sender1, sender2);
        sender2.sendEndTurn();
        transferTurn(sender2, reciver1);

        sender1.sendEndTurnResponse();
        sender2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, (PvpBattleStateEnum.EnvironmentInTurn.getStateTimeoutInSeconds() + 1) * 1000, false);

        startTurn(reciver2);

        sender1.sendEndTurnResponse();
        startTurn(sender1);

        sender1.sendEndTurn(reciver1, reciver2);

        sender1.finishBattleWithResult(PvpBattleResult.WINNER, DEFAULT_DELAY, true);
    }

    /**
     * 1. игроки ходят по очереди
     * 2. оба игрока не подтверждает "ход" 1-го бота - порожение обоим после таймаута
     */
    @Test
    public void test_4() throws InterruptedException {
        sender1.sendEndTurn();
        transferTurn(sender1, sender2);
        sender2.sendEndTurn();
        transferTurn(sender2, reciver1);

        sender1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, (PvpBattleStateEnum.EnvironmentInTurn.getStateTimeoutInSeconds() + 1) * 1000, false);
        sender2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, DEFAULT_DELAY, false);
    }

    /**
     * тормозной клиент
     */
    @Test
    public void test_5() throws InterruptedException {
        sender1.sendEndTurn();
        transferTurn(sender1, sender2);
        sender2.sendEndTurn();
        transferTurn(sender2, reciver1);

        sender1.sendEndTurnResponse();
        Thread.sleep((PvpBattleStateEnum.EnvironmentInTurn.getIdleTimeoutInSeconds() + 1) * 1000);
        sender2.sendEndTurnResponse();

        startTurn(reciver2);

        sender1.sendEndTurnResponse();
        Thread.sleep((PvpBattleStateEnum.EnvironmentInTurn.getIdleTimeoutInSeconds() + 1) * 1000);
        sender2.sendEndTurnResponse();

        startTurn(sender1);

        sender1.sendEndTurn(reciver1, reciver2);
        confirmTurnTransfer(sender1);

        sender1.finishBattleWithResult(PvpBattleResult.WINNER, DEFAULT_DELAY, true);
        sender2.finishBattleWithResult(PvpBattleResult.WINNER, DEFAULT_DELAY, true);
    }

//============================================================================================================================================================================================

    private void transferTurn(PvpParticipant inTurn, PvpParticipant nextTurn) {
        confirmTurnTransfer(inTurn);

        startTurn(nextTurn);
    }

    private void startTurn(PvpParticipant nextTurn) {
        for(PvpParticipant user : battleParticipants) {
            if(!user.isEndBattle()) {
                PvpStartTurn startTurn = user.reciveFromPvp(PvpStartTurn.class, DEFAULT_DELAY);
                assertEquals(startTurn.turningPlayerNum, nextTurn.getPlayerNum());
                user.onStartTurn(startTurn);
            }
        }
    }

    private void confirmTurnTransfer(PvpParticipant inTurn) {
        for(PvpParticipant user : battleParticipants) {
            if(!user.isEndBattle() && !user.equals(inTurn)) {
                PvpEndTurn pvpEndTurn = user.reciveFromPvp(PvpEndTurn.class, DEFAULT_DELAY);
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

            user1_1.sendCreatePveFriendBattleRequest(new short[]{(short) 10, (short) 1}, 0, user1_2.getUserId());

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
                StartPvpBattle startPvpBattle = user.reciveFromPvp(StartPvpBattle.class, DEFAULT_DELAY);
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
