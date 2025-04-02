package com.pragmatix.pvp;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.server.PvpStartTurn;
import com.pragmatix.pvp.messages.handshake.client.ReadyForBattle;
import com.pragmatix.pvp.messages.handshake.server.BattleCreated;
import com.pragmatix.pvp.messages.handshake.server.StartPvpBattle;
import com.pragmatix.pvp.services.battletracking.PvpBattleStateEnum;
import com.pragmatix.pvp.services.battletracking.PvpBattleTrackerTimeoutTask;
import com.pragmatix.serialization.AppBinarySerializer;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.11.12 15:36
 */
public class Wager2x2Test extends PvpAbstractTest {

    protected PvpParticipant sender1;
    protected PvpParticipant sender2;
    protected PvpParticipant reciver1;
    protected PvpParticipant reciver2;

    @Before
    public void startBattle() throws Exception {
        turnOffLobbyRestrict();

        Wager2x2Battle wager2x2Battle = new Wager2x2Battle(binarySerializer).startBattle(user1Id, user2Id, user3Id, user4Id);
        sender1 = wager2x2Battle.getSender1();
        sender2 = wager2x2Battle.getSender2();
        reciver1 = wager2x2Battle.getReciver1();
        reciver2 = wager2x2Battle.getReciver2();
        battleParticipants = wager2x2Battle.getBattleParticipants();
    }

//    @After
//    public void afterTest() throws InterruptedException {
//        sender1.disconnectFromPvp();
//        sender2.disconnectFromPvp();
//        reciver1.disconnectFromPvp();
//        reciver2.disconnectFromPvp();
//        Thread.sleep(1000);
//    }
//
//    @AfterClass
//    public static void after() throws InterruptedException {
//        for(Channel channel : mainChannelsMap.values()) {
//            if(channel.isActive()) {
//                channel.disconnect();
//            }
//        }
//        Thread.sleep(PvpBattleStateEnum.EndBattle.getStateTimeoutInSeconds() * 1000);
//        Thread.sleep(1000);
//    }

//============================================================================================================================================================================================

    /**
     * 1. 1_1 выносит 2_1
     * 2. 2_2 выносит 1_1
     * 3. 1_2 выносит 2_2
     * 4. победа 1_1, 1_2
     * 5. поражение 2_1, 2_2
     */
    @Test
    public void test_1() throws InterruptedException {
        sender1.sendEndTurn(reciver1);

        transferTurn(sender1, reciver2);

        reciver2.sendEndTurn(sender1);

        transferTurn(reciver2, sender2);

        sender2.sendEndTurn(reciver2);

        confirmTurnTransfer(sender2);

        sender1.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        sender2.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        reciver1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
        reciver2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
    }

    /**
     *
     */
    @Test
    public void test_2() throws InterruptedException {
        sender1.sendEndTurn(reciver1);

        transferTurn(sender1, reciver2);

        sender2.surrender();
        sender2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);

        reciver2.sendEndTurn(sender1);

        transferTurn(reciver2, sender1);

        sender1.sendEndTurn(sender2);

        confirmTurnTransfer(sender1);

        sender1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
        reciver1.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        reciver2.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
    }

    @Test
    public void test_3() throws InterruptedException {
        sender1.sendEndTurn(reciver1);

        sender2.surrender();
        sender2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);

        transferTurn(sender1, reciver2);

        reciver2.sendEndTurn(sender1);

        transferTurn(reciver2, sender1);

        sender1.sendEndTurn(sender2);

        confirmTurnTransfer(sender1);

        sender1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
        reciver1.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        reciver2.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
    }

    /**
     * 2_2 сдается
     * 1_1 выносит 2_1
     * 1_2 disconnect
     * 2_1 получает ход и не ходит
     * 1_1 победа, остальные поражение
     */
    @Test
    public void test_4() throws InterruptedException {
        reciver2.surrender();
        reciver2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);

        sender1.sendEndTurn(reciver1);

        sender2.disconnectFromPvp();

        transferTurn(sender1, reciver1);

        sender1.finishBattleWithResult(PvpBattleResult.WINNER, (PvpBattleStateEnum.ReadyToDispatch.getIdleTimeoutInSeconds() + PvpBattleStateEnum.WaitForReplayCommand.getStateTimeoutInSeconds() + 2) * 1000, true);
//        sender1.finishBattleWithResult(PvpBattleResult.WINNER, Integer.MAX_VALUE, true);
        reciver1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);
    }

    /**
     * При игре на ставках 2 на 2 играем с напарником ! меня выкидывает из игры и мой напарник остаётся один ! его персонажей убивают но напарник выигрывает ставку моими персонажами !
     * по окончании игры ему приписывают поражение так как его персонажи убиты а мои персонажи как-бы потеряли смысл и соединение !
     * 1_1 ходит
     * 2_2 сдается (disconnect)
     * 2_1 выносит 1_1
     * 1_2 выносит 2_1
     * 2_1 выносит 1_2
     * 2_1 победа, остальные поражение
     */
    @Test
    public void test_5() throws InterruptedException {
        sender1.sendEndTurn();
        transferTurn(sender1, reciver1);

        reciver1.sendActionEx();
        reciver2.disconnectFromPvp();
        Thread.sleep((pvpService.reconnectTimeoutInSeconds + 1) * 1000);
//        reciver2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, (PvpBattleTrackerTimoutTask.reconnectTimeoutInSeconds + 1) * 1000, false);
        reciver1.sendActionEx();

        reciver1.sendEndTurn(sender1);
        transferTurn(reciver1, sender2);

        sender2.sendEndTurn(reciver1);
        transferTurn(sender2, reciver1);

        reciver1.sendEndTurn(sender2);
        confirmTurnTransfer(reciver1);

        reciver1.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        sender1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
        sender2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
    }

    /**
     * Когда мой напарник теряет соединение, и все мои персы убиты, остались ток его, и мы делаем с противником ничью
     * Ну то есть идут все на край, и когда меня убивают и противник тоже сам себя, противнику ничью, а мне поражение.
     * <p>
     * 1_1 ходит
     * 2_2 теряет соединение
     * 2_1 выносит 1_1
     * 1_2 выносит 2_1
     * погибают оба 2_1 и 1_2
     * 2_1 и 1_2 ничья
     */
    @Test
    public void test_6() throws InterruptedException {
        sender1.sendEndTurn();
        transferTurn(sender1, reciver1);

        reciver1.sendActionEx();
        reciver2.disconnectFromPvp();
        Thread.sleep((pvpService.reconnectTimeoutInSeconds + 1) * 1000);
//        reciver2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, (PvpBattleTrackerTimoutTask.reconnectTimeoutInSeconds + 1) * 1000, false);
        reciver1.sendActionEx();

        reciver1.sendEndTurn(sender1);
        transferTurn(reciver1, sender2);

        sender2.sendEndTurn(reciver1);
        transferTurn(sender2, reciver1);

        reciver1.sendEndTurn(sender2, reciver2);
        confirmTurnTransfer(reciver1);

        sender1.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
        sender2.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
        reciver1.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
        // reciver2 disconnected
    }

    @Test
    public void test_6_2() throws InterruptedException {
        sender1.sendEndTurn();
        transferTurn(sender1, reciver1);

        reciver1.sendActionEx();
        reciver2.disconnectFromPvp();
        sender1.disconnectFromPvp();
        Thread.sleep((pvpService.reconnectTimeoutInSeconds + 1) * 1000);
        reciver1.sendActionEx();

        reciver1.sendEndTurn(sender2);
        transferTurn(reciver1, sender2);

        sender2.sendEndTurn(reciver1);
        transferTurn(sender2, reciver1);

        reciver1.sendEndTurn(sender1, reciver2);
        confirmTurnTransfer(reciver1);

        sender2.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
        reciver1.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
        // reciver2 disconnected
    }

//============================================================================================================================================================================================

    protected void transferTurn(PvpParticipant inTurn, PvpParticipant nextTurn) {
        confirmTurnTransfer(inTurn);

        for(PvpParticipant user : battleParticipants) {
            if(!user.isEndBattle()) {
                PvpStartTurn startTurn = user.reciveFromPvp(PvpStartTurn.class, 300);
                assertEquals(startTurn.turningPlayerNum, nextTurn.getPlayerNum());
                user.onStartTurn(startTurn);
            }
        }
    }

    protected void confirmTurnTransfer(PvpParticipant inTurn) {
        for(PvpParticipant user : battleParticipants) {
            if(!user.isEndBattle() && !user.equals(inTurn)) {
                PvpEndTurn pvpEndTurn = user.reciveFromPvp(PvpEndTurn.class, 300);
                user.confirmTurnTransfer(pvpEndTurn);
            }
        }
    }

    static class Wager2x2Battle {
        private PvpParticipant sender1;
        private PvpParticipant sender2;
        private PvpParticipant reciver1;
        private PvpParticipant reciver2;
        private List<PvpParticipant> battleParticipants;
        AppBinarySerializer binarySerializer;

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

        Wager2x2Battle(AppBinarySerializer binarySerializer) {
            this.binarySerializer = binarySerializer;
        }

        public void failureStartBattle(long user1Id1, long user2Id1, long user3Id1, long user4Id1) throws Exception {
            PvpParticipant user1_1 = new PvpParticipant(user1Id1, binarySerializer);
            PvpParticipant user1_2 = new PvpParticipant(user2Id1, binarySerializer);
            PvpParticipant user2_1 = new PvpParticipant(user3Id1, binarySerializer);
            PvpParticipant user2_2 = new PvpParticipant(user4Id1, binarySerializer);
            battleParticipants = Arrays.asList(user1_1, user1_2, user2_1, user2_2);

            for(PvpParticipant user : battleParticipants) {
                if(mainChannelsMap.containsKey(user) && mainChannelsMap.get(user).isActive()) {
                    user.setMainChannel(mainChannelsMap.get(user));
                } else {
                    user.loginMain();
                    mainChannelsMap.put(user, user.getMainChannel());
                }

                user.connectToPvp();
            }


            for(PvpParticipant user : battleParticipants) {
                user.sendCreateBattleRequest(BattleWager.WAGER_50_2x2);
                Thread.sleep(100);
            }

            for(PvpParticipant user : battleParticipants) {
                BattleCreated battleCreated = user.reciveFromPvpNullable(BattleCreated.class, 100);
                assertNull(battleCreated);
            }
        }

        public Wager2x2Battle startBattle(long user1Id1, long user2Id1, long user3Id1, long user4Id1) throws Exception {
            PvpParticipant user1_1 = new PvpParticipant(user1Id1, SocialServiceEnum.android, binarySerializer);
            PvpParticipant user1_2 = new PvpParticipant(user2Id1, SocialServiceEnum.mobile, binarySerializer);
            PvpParticipant user2_1 = new PvpParticipant(user3Id1, SocialServiceEnum.android, binarySerializer);
            PvpParticipant user2_2 = new PvpParticipant(user4Id1, SocialServiceEnum.mobile, binarySerializer);
            battleParticipants = Arrays.asList(user1_1, user1_2, user2_1, user2_2);

            for(PvpParticipant user : battleParticipants) {
                if(mainChannelsMap.containsKey(user) && mainChannelsMap.get(user).isActive()) {
                    user.setMainChannel(mainChannelsMap.get(user));
                } else {
                    user.loginMain();
                    mainChannelsMap.put(user, user.getMainChannel());
                }

                user.connectToPvp();
            }


            for(PvpParticipant user : battleParticipants) {
                user.sendCreateBattleRequest(BattleWager.WAGER_50_2x2);
                Thread.sleep(100);
            }

            for(PvpParticipant user : battleParticipants) {
                BattleCreated battleCreated = user.reciveFromPvp(BattleCreated.class, 1000);
                user.sendToPvp(new ReadyForBattle(battleCreated.getBattleId()));
            }

            for(PvpParticipant user : battleParticipants) {
                StartPvpBattle startPvpBattle = user.reciveFromPvp(StartPvpBattle.class, 300);
                user.onStartBattle(startPvpBattle);
            }

            Thread.sleep(1000);

            for(PvpParticipant battleParticipant : battleParticipants) {
                if(battleParticipant.getPlayerNum() == 0) {
                    sender1 = battleParticipant;
                } else if(battleParticipant.getPlayerNum() == 1) {
                    sender2 = battleParticipant;
                } else if(battleParticipant.getPlayerNum() == 2) {
                    reciver1 = battleParticipant;
                } else if(battleParticipant.getPlayerNum() == 3) {
                    reciver2 = battleParticipant;
                }
            }
            assertNotNull(sender1);
            assertNotNull(sender2);
            assertNotNull(reciver1);
            assertNotNull(reciver2);
            return this;
        }

        public void finishBattle() {
            reciver1.surrender();
            reciver1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);
            reciver1.disconnectFromPvp();

            reciver2.surrender();
            reciver2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);
            reciver2.disconnectFromPvp();

            sender1.finishBattleWithResult(PvpBattleResult.WINNER, 1000, true);
            sender2.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);

            sender1.disconnectFromPvp();
            sender2.disconnectFromPvp();
        }


        public void disconnectFromPvp() {
            for(PvpParticipant user : battleParticipants) {
                user.disconnectFromPvp();
            }
        }
    }

}
