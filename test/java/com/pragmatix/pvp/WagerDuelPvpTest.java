package com.pragmatix.pvp;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.pvp.dsl.WagerDuelBattle;
import com.pragmatix.pvp.messages.battle.client.PvpActionEx;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurnResponse;
import com.pragmatix.pvp.services.battletracking.PvpBattleStateEnum;
import com.pragmatix.pvp.services.battletracking.PvpBattleTrackerTimeoutTask;
import io.netty.channel.Channel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.11.12 15:36
 */
public class WagerDuelPvpTest extends PvpAbstractTest {

    protected PvpParticipant sender;
    protected PvpParticipant receiver;

    @Before
    public void startBattle() throws Exception {
        turnOffLobbyRestrict();

        WagerDuelBattle wagerDuelBattle = new WagerDuelBattle(binarySerializer, mainChannelsMap).startBattle(user1Id, user2Id);
        sender = wagerDuelBattle.getSender();
        receiver = wagerDuelBattle.getReceiver();
    }

//    @After
//    public void afterTest() throws InterruptedException {
//        sender.disconnectFromPvp();
//        receiver.disconnectFromPvp();
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
     * 1. при передаче хода приходит код бана
     * 2. оставшийся игрок получает победу
     */
    @Test
    public void banDuringEndTurn() throws InterruptedException {
        sender.sendCheatEndTurn();

        PvpEndTurn pvpEndTurn = receiver.reciveFromPvp(PvpEndTurn.class, 300);
        assertTrue(pvpEndTurn.forced);
        receiver.sendToPvp(new PvpEndTurnResponse(pvpEndTurn));

        receiver.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        sender.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);

        assertTrue(banService.isBanned(sender.userId));
    }

    /**
     * 1. отваливается активный игрок
     * 2. пассивный игрок победу, активный поражение после reconnect таймаута
     */
    @Test
    public void wagerPvpDuelTest_1() throws InterruptedException {
        sender.disconnectFromPvp();

        receiver.finishBattleWithResult(PvpBattleResult.WINNER, (pvpService.reconnectTimeoutInSeconds + 1) * 1000, true);
    }

    /**
     * 1. отваливается пассивный игрок
     * 2. активный игрок получает победу после reconnect таймаута
     */
    @Test
    public void wagerPvpDuelTest_3() throws InterruptedException {
        receiver.disconnectFromPvp();

        sender.finishBattleWithResult(PvpBattleResult.WINNER, (pvpService.reconnectTimeoutInSeconds + 1) * 1000, true);
    }

    /**
     * 1. сдается пассивный игрок
     * 2. активный игрок сразу получает победу
     */
    @Test
    public void wagerPvpDuelTest_5() throws InterruptedException {
        receiver.surrender();

        sender.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        receiver.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);
    }

    /**
     * 1. сдается активный игрок
     * 2. пассивный игрок сразу получает победу
     */
    @Test
    public void wagerPvpDuelTest_6() throws InterruptedException {
        sender.sendActionEx();
        receiver.reciveFromPvp(PvpActionEx.class, 300);

        sender.surrender();
        sender.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);
        receiver.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
    }

    /**
     * 1. сдается активный игрок
     * 1. следом сдается пассивный игрок
     * 2. пассивный игрок получает победу
     */
    @Test
    public void wagerPvpDuelTest_6_1() throws InterruptedException {
        sender.sendActionEx();
        receiver.reciveFromPvp(PvpActionEx.class, 300);

        sender.surrender();
        Thread.sleep(100);
        receiver.surrender();
        sender.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);
        receiver.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
    }

    @Test
    public void wagerPvpDuelTest_6_2() throws InterruptedException {
        sender.sendActionEx();
        receiver.reciveFromPvp(PvpActionEx.class, 300);

        Thread.sleep(100);
        receiver.surrender();
        sender.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        receiver.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
    }

    /**
     * 1. погибает активный игрок
     * 2. пассивный игрок подтверждает передачу хода
     * 3. и получает победу
     */
    @Test
    public void wagerPvpDuelTest_7() throws InterruptedException {
        sender.sendEndTurn(sender);

        PvpEndTurn pvpEndTurn = receiver.reciveFromPvp(PvpEndTurn.class, 300);
        assertFalse(pvpEndTurn.forced);
        receiver.sendToPvp(new PvpEndTurnResponse(pvpEndTurn));

        receiver.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        sender.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
    }

    /**
     * 1. погибает пассивный игрок
     * 2. пассивный игрок подтверждает передачу хода
     * 3. и получает поражение
     */
    @Test
    public void wagerPvpDuelTest_8() throws InterruptedException {
        sender.sendEndTurn(receiver);

        PvpEndTurn pvpEndTurn = receiver.reciveFromPvp(PvpEndTurn.class, 300);
        assertFalse(pvpEndTurn.forced);
        receiver.sendToPvp(new PvpEndTurnResponse(pvpEndTurn));

        receiver.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
        sender.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
    }

    /**
     * 1. погибают оба
     * 2. пассивный игрок подтверждает передачу хода
     * 3. и оба получают ничью
     */
    @Test
    public void wagerPvpDuelTest_9() throws InterruptedException {
        Thread.sleep(1000);

        sender.sendEndTurn(receiver, sender);

        PvpEndTurn pvpEndTurn = receiver.reciveFromPvp(PvpEndTurn.class, 300);
        assertFalse(pvpEndTurn.forced);
        receiver.sendToPvp(new PvpEndTurnResponse(pvpEndTurn));

        receiver.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
        sender.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
    }

    /**
     * 1. погибают оба
     * 2. пассивный игрок не подтверждает передачу хода
     * 3. и получают поражение
     * 3. активный игрок получает ничью
     */
    @Test
    public void wagerPvpDuelTest_10() throws InterruptedException {
        sender.sendEndTurn(receiver, sender);

        receiver.finishBattleWithResult(PvpBattleResult.NOT_WINNER, (PvpBattleStateEnum.WaitForTurnTransfer.getStateTimeoutInSeconds() + 1) * 1000, false);
        sender.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
    }

    @Test
    public void reconnectTest() throws Exception {
        Thread.sleep(100);
        sender.sendEndTurn();
        PvpEndTurn pvpEndTurn = receiver.reciveFromPvp(PvpEndTurn.class, 300);

        sender.disconnectFromPvp();
        Thread.sleep(100);
        receiver.disconnectFromPvp();
        Thread.sleep(100);

        sender.connectToPvp();
        sender.sendToPvp(sender.createReconnectToBattleRequest());
        Thread.sleep(100);

        receiver.connectToPvp();
        receiver.sendToPvp(receiver.createReconnectToBattleRequest());

        receiver.sendToPvp(new PvpEndTurnResponse(pvpEndTurn));

        receiver.sendEndTurn(receiver, sender);
        pvpEndTurn = sender.reciveFromPvp(PvpEndTurn.class, 300);
        sender.sendToPvp(new PvpEndTurnResponse(pvpEndTurn));

        receiver.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
        sender.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
    }
//============================================================================================================================================================================================

}
