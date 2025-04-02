package com.pragmatix.pvp;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.pvp.messages.battle.client.PvpActionEx;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.server.PvpStartTurn;
import com.pragmatix.pvp.messages.handshake.client.ReadyForBattle;
import com.pragmatix.pvp.messages.handshake.server.BattleCreated;
import com.pragmatix.pvp.messages.handshake.server.StartPvpBattle;
import com.pragmatix.pvp.services.battletracking.PvpBattleStateEnum;
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf;
import com.pragmatix.serialization.AppBinarySerializer;
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

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.11.12 15:36
 */
public class Wager3ForAllPvpTest extends AbstractSpringTest {

    private static int battleCount = 0;

    protected PvpParticipant sender;
    protected PvpParticipant reciver1;
    protected PvpParticipant reciver2;

    private static Map<PvpParticipant, Channel> mainChannelsMap = new HashMap<>();

    @Resource
    private LobbyConf lobbyConf;

    @Before
    public void startBattle() throws Exception {
        lobbyConf.setBestMatchQuality(0);

        Wager3ForAllBattle wager3ForAllBattle = new Wager3ForAllBattle(binarySerializer).startBattle(testerProfileId - (battleCount * 3), testerProfileId - (battleCount * 3) - 1, testerProfileId - (battleCount * 3) - 2);
        sender = wager3ForAllBattle.getSender();
        reciver1 = wager3ForAllBattle.getReciver1();
        reciver2 = wager3ForAllBattle.getReciver2();
        battleCount++;
    }

    @After
    public void afterTest() throws InterruptedException {
        sender.disconnectFromPvp();
        reciver1.disconnectFromPvp();
        reciver2.disconnectFromPvp();
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
     * 1. reciver1 сдается
     * 2. sender передает ход
     * 3. reciver2 подверждают передачу хода
     * 4. reciver2 "выносит" sender
     * 5. sender и reciver1 подверждают передачу хода
     * 6. reciver2 - победа, остальным поражение
     */
    @Test
    public void test_1() throws InterruptedException {
        Thread.sleep(1000);

        sender.sendActionEx();

        for(PvpParticipant user : Arrays.asList(reciver1, reciver2)) {
            user.reciveFromPvp(PvpActionEx.class, 300);
        }

        Thread.sleep(1000);

        reciver1.surrender();
        reciver1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);

        Thread.sleep(1000);

        sender.sendEndTurn();

        PvpEndTurn pvpEndTurn = reciver2.reciveFromPvp(PvpEndTurn.class, 300);
        reciver2.confirmTurnTransfer(pvpEndTurn);

        for(PvpParticipant user : Arrays.asList(sender, reciver2)) {
            PvpStartTurn startTurn = user.reciveFromPvp(PvpStartTurn.class, 300);
            assertEquals(startTurn.turningPlayerNum, reciver2.getPlayerNum());
            user.onStartTurn(startTurn);
        }

        reciver2.sendEndTurn(sender);

        pvpEndTurn = sender.reciveFromPvp(PvpEndTurn.class, 300);
        sender.confirmTurnTransfer(pvpEndTurn);

//        reciver1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
        sender.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
        reciver2.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
    }

    /**
     * 1. sender "выносит" reciver1
     * 2. sender передает ход
     * 3. reciver1,2 подверждают передачу хода
     * 4. reciver2 "выносит" sender
     * 5. sender и reciver1 подверждают передачу хода
     * 6. reciver2 - победа, остальным поражение
     */
    @Test
    public void test_2() throws InterruptedException {
        sender.sendActionEx();
        for(PvpParticipant user : Arrays.asList(reciver1, reciver2)) {
            user.reciveFromPvp(PvpActionEx.class, 300);
        }

        sender.sendEndTurn(reciver1);
        for(PvpParticipant user : Arrays.asList(reciver1, reciver2)) {
            PvpEndTurn pvpEndTurn = user.reciveFromPvp(PvpEndTurn.class, 300);
            user.confirmTurnTransfer(pvpEndTurn);
        }
        for(PvpParticipant user : Arrays.asList(sender, reciver1, reciver2)) {
            PvpStartTurn startTurn = user.reciveFromPvp(PvpStartTurn.class, 300);
            assertEquals(startTurn.turningPlayerNum, reciver2.getPlayerNum());
            user.onStartTurn(startTurn);
        }

        reciver2.sendActionEx();
        for(PvpParticipant user : Arrays.asList(sender, reciver1)) {
            user.reciveFromPvp(PvpActionEx.class, 300);
        }

        reciver2.sendEndTurn(sender);
        for(PvpParticipant user : Arrays.asList(sender, reciver1)) {
            PvpEndTurn pvpEndTurn = user.reciveFromPvp(PvpEndTurn.class, 300);
            user.confirmTurnTransfer(pvpEndTurn);
        }

        reciver1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
        sender.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
        reciver2.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
    }

    /**
     * 1. sender "выносит" reciver1
     * 2. sender передает ход
     * 3. reciver1,2 подверждают передачу хода
     * 4. reciver2 "выносит" sender
     * 5. sender подверждают передачу хода
     * 6. reciver1 не подверждает
     * 7. reciver2 - победа, остальным поражение
     * 8. reciver1 - получает поражение без подтверждения от main
     */
    @Test
    public void test_3() throws InterruptedException {
        sender.sendActionEx();
        for(PvpParticipant user : Arrays.asList(reciver1, reciver2)) {
            user.reciveFromPvp(PvpActionEx.class, 300);
        }

        sender.sendEndTurn(reciver1);
        for(PvpParticipant user : Arrays.asList(reciver1, reciver2)) {
            PvpEndTurn pvpEndTurn = user.reciveFromPvp(PvpEndTurn.class, 300);
            user.confirmTurnTransfer(pvpEndTurn);
        }
        for(PvpParticipant user : Arrays.asList(sender, reciver1, reciver2)) {
            PvpStartTurn startTurn = user.reciveFromPvp(PvpStartTurn.class, 300);
            assertEquals(startTurn.turningPlayerNum, reciver2.getPlayerNum());
            user.onStartTurn(startTurn);
        }

        reciver2.sendActionEx();
        for(PvpParticipant user : Arrays.asList(sender, reciver1)) {
            user.reciveFromPvp(PvpActionEx.class, 300);
        }

        reciver2.sendEndTurn(sender);
        for(PvpParticipant user : Arrays.asList(sender)) {
            PvpEndTurn pvpEndTurn = user.reciveFromPvp(PvpEndTurn.class, 300);
            user.confirmTurnTransfer(pvpEndTurn);
        }

        reciver1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, (PvpBattleStateEnum.WaitForTurnTransfer.getStateTimeoutInSeconds() + 1) * 1000, false);

        sender.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
        reciver2.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
    }

    /**
     * 1. sender "выносит" reciver1 и передает ход
     * 2. reciver1,2 подверждают передачу хода
     * 3. reciver2 получает ход и ходит
     * 4. reciver1 рвет соединение и получает поражение без подтверждения от main
     * 5. reciver2 "выносит" sender  и передает ход
     * 6. sender подверждают передачу хода
     * 7. reciver2 - победа, sender - поражение
     */
    @Test
    public void test_4() throws InterruptedException {
        sender.sendActionEx();
        for(PvpParticipant user : Arrays.asList(reciver1, reciver2)) {
            user.reciveFromPvp(PvpActionEx.class, 300);
        }

        sender.sendEndTurn(reciver1);
        for(PvpParticipant user : Arrays.asList(reciver1, reciver2)) {
            PvpEndTurn pvpEndTurn = user.reciveFromPvp(PvpEndTurn.class, 300);
            user.confirmTurnTransfer(pvpEndTurn);
        }
        for(PvpParticipant user : Arrays.asList(sender, reciver1, reciver2)) {
            PvpStartTurn startTurn = user.reciveFromPvp(PvpStartTurn.class, 300);
            assertEquals(startTurn.turningPlayerNum, reciver2.getPlayerNum());
            user.onStartTurn(startTurn);
        }

        reciver2.sendActionEx();
        for(PvpParticipant user : Arrays.asList(sender, reciver1)) {
            user.reciveFromPvp(PvpActionEx.class, 300);
        }

        reciver1.disconnectFromPvp();

        reciver2.sendEndTurn(sender);
        for(PvpParticipant user : Arrays.asList(sender)) {
            PvpEndTurn pvpEndTurn = user.reciveFromPvp(PvpEndTurn.class, 300);
            user.confirmTurnTransfer(pvpEndTurn);
        }

        sender.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
        reciver2.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
    }

    /**
     * 1. sender завершает ход, и тут же сдается
     * 2. sender получает поражение
     * 3. reciver1,2 подверждают передачу хода
     * 4. reciver1 получает ход и выносит reciver2
     * 5. reciver1,2 подверждают передачу хода
     * 6. reciver1 - победа, reciver2 - поражение
     */
    @Test
    public void test_5() throws InterruptedException {
        sender.sendActionEx();
        for(PvpParticipant user : Arrays.asList(reciver1, reciver2)) {
            user.reciveFromPvp(PvpActionEx.class, 300);
        }

        sender.sendEndTurn();
        sender.surrender();
        sender.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);

        for(PvpParticipant user : Arrays.asList(reciver1, reciver2)) {
            PvpEndTurn pvpEndTurn = user.reciveFromPvp(PvpEndTurn.class, 300);
            user.confirmTurnTransfer(pvpEndTurn);
        }
        for(PvpParticipant user : Arrays.asList(reciver1, reciver2)) {
            PvpStartTurn startTurn = user.reciveFromPvp(PvpStartTurn.class, 300);
            assertEquals(startTurn.turningPlayerNum, reciver1.getPlayerNum());
            user.onStartTurn(startTurn);
        }

        reciver1.sendActionEx();
        for(PvpParticipant user : Arrays.asList(reciver2)) {
            user.reciveFromPvp(PvpActionEx.class, 300);
        }

        reciver1.sendEndTurn(reciver2);
        for(PvpParticipant user : Arrays.asList(reciver2)) {
            PvpEndTurn pvpEndTurn = user.reciveFromPvp(PvpEndTurn.class, 300);
            user.confirmTurnTransfer(pvpEndTurn);
        }

        reciver1.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        reciver2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
    }

    /**
     * 1. sender завершает ход, сразу же сдается и получает немедленное поражение
     * 3. reciver1 получает PvpEndTurn, тоже сдается и тоже получает немедленное поражение
     * 6. reciver2 получает победу
     */
    @Test
    public void test_6() throws InterruptedException {
        sender.sendActionEx();
        for(PvpParticipant user : Arrays.asList(reciver1, reciver2)) {
            user.reciveFromPvp(PvpActionEx.class, 300);
        }

        sender.sendEndTurn();
        sender.surrender();
        sender.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);

        reciver1.reciveFromPvp(PvpEndTurn.class, 300);
        reciver1.surrender();
        reciver1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);

        reciver2.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
    }
//============================================================================================================================================================================================

    static class Wager3ForAllBattle {
        private AppBinarySerializer binarySerializer;
        private PvpParticipant sender;
        private PvpParticipant reciver1;
        private PvpParticipant reciver2;
        private List<PvpParticipant> battleParticipants;

        Wager3ForAllBattle(AppBinarySerializer binarySerializer) {
            this.binarySerializer = binarySerializer;
        }

        public PvpParticipant getSender() {
            return sender;
        }

        public PvpParticipant getReciver1() {
            return reciver1;
        }

        public PvpParticipant getReciver2() {
            return reciver2;
        }

        public void failureStartBattle(long user1Id, long user2Id, long user3Id) throws Exception {
            PvpParticipant user1 = new PvpParticipant(user1Id, binarySerializer);
            PvpParticipant user2 = new PvpParticipant(user2Id, binarySerializer);
            PvpParticipant user3 = new PvpParticipant(user3Id, binarySerializer);

            battleParticipants = Arrays.asList(user1, user2, user3);

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
                user.sendCreateBattleRequest(BattleWager.WAGER_50_3_FOR_ALL);
                Thread.sleep(100);
            }

            for(PvpParticipant user : battleParticipants) {
                BattleCreated battleCreated = user.reciveFromPvpNullable(BattleCreated.class, 100);
                assertNull(battleCreated);
            }
        }

        public Wager3ForAllBattle startBattle(long user1Id, long user2Id, long user3Id) throws Exception {
            PvpParticipant user1 = new PvpParticipant(user1Id, binarySerializer);
            PvpParticipant user2 = new PvpParticipant(user2Id, binarySerializer);
            PvpParticipant user3 = new PvpParticipant(user3Id, binarySerializer);

            battleParticipants = Arrays.asList(user1, user2, user3);

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
                user.sendCreateBattleRequest(BattleWager.WAGER_50_3_FOR_ALL);
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
                    sender = battleParticipant;
                } else if(battleParticipant.getPlayerNum() == 1) {
                    reciver1 = battleParticipant;
                } else if(battleParticipant.getPlayerNum() == 2) {
                    reciver2 = battleParticipant;
                }
            }
            assertNotNull(sender);
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

            sender.finishBattleWithResult(PvpBattleResult.WINNER, 1000, true);
            sender.disconnectFromPvp();
        }

        public void disconnectFromPvp() {
            for(PvpParticipant user : battleParticipants) {
                user.disconnectFromPvp();
            }
        }

    }

}
