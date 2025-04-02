package com.pragmatix.pvp.jskills;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.pvp.PvpParticipant;
import com.pragmatix.pvp.Wager3ForAllPvpTest;
import com.pragmatix.pvp.messages.battle.client.PvpActionEx;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.server.PvpStartTurn;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 09.04.13 16:24
 */
public class Three4AllSkillTest extends Wager3ForAllPvpTest {
    /**
     * 1. sender "выносит" reciver1
     * 2. sender передает ход
     * 3. reciver1,2 подверждают передачу хода
     * 4. reciver2 "выносит" sender
     * 5. sender и reciver1 подверждают передачу хода
     * 6. reciver2 - победа, остальным поражение
     */
    @Test
    public void test_1() throws InterruptedException {

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
     * 1. reciver1 сдается
     * 2. sender передает ход
     * 3. reciver2 подверждают передачу хода
     * 4. reciver2 "выносит" sender
     * 5. sender и reciver1 подверждают передачу хода
     * 6. reciver2 - победа, остальным поражение
     */
    @Test
    public void test_2() throws InterruptedException {
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

        sender.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
        reciver2.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
    }

    /**
     * 1. reciver1 сдается
     * 2. sender передает ход
     * 3. reciver2 подверждают передачу хода
     * 4. reciver2 "выносит" sender и reciver2
     * 5. sender подверждет передачу хода и sender и reciver2 получают ничью
     */
    @Test
    public void test_3() throws InterruptedException {
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

        reciver2.sendEndTurn(sender, reciver2);

        pvpEndTurn = sender.reciveFromPvp(PvpEndTurn.class, 300);
        sender.confirmTurnTransfer(pvpEndTurn);

        sender.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
        reciver2.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
    }
}
