package com.pragmatix.pvp.jskills;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.pvp.Wager2x2Test;
import org.junit.Test;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 10.04.13 10:57
 */
public class TeamByTeamSkillTest extends Wager2x2Test {
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

//        Thread.sleep(Integer.MAX_VALUE);

        sender1.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        sender2.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        reciver1.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
        reciver2.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, true);
    }
}
