package com.pragmatix.pvp;

import com.pragmatix.app.services.RestrictionService;
import com.pragmatix.pvp.dsl.WagerDuelBattle;
import com.pragmatix.pvp.messages.battle.client.PvpChatMessage;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;

import static com.pragmatix.app.model.RestrictionItem.BlockFlag.CHAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 16.06.2016 16:55
 */
public class ChatRestrictTest extends PvpAbstractTest {

    @Resource
    private RestrictionService restrictionService;


    @Before
    public void setUp() throws InterruptedException {
        turnOffLobbyRestrict();
        restrictionService.remove(user1Id);
        restrictionService.remove(user2Id);
    }

    @Test
    public void testNotRestricted() throws Exception {
        // restrictions cleared in @Before
        assertFalse(restrictionService.isRestricted(user1Id, CHAT));
        assertFalse(restrictionService.isRestricted(user2Id, CHAT));

        WagerDuelBattle battle = new WagerDuelBattle(binarySerializer).setWager(BattleWager.WAGER_15_DUEL);
        battle.startBattle(user1Id, user2Id);
        
        PvpParticipant sender = battle.getSender();
        PvpParticipant receiver = battle.getReceiver();

        // если ограничений нет - то сообщения ходят как в одну...
        PvpChatMessage msg = newMessageFrom(sender, "hello", false);
        sender.sendToPvp(msg);
        assertSame(msg, receiver.reciveFromPvp(PvpChatMessage.class, 1000));

        // ...так и в обратную сторону
        msg = newMessageFrom(receiver, "blabla", false);
        receiver.sendToPvp(msg);
        assertSame(msg, sender.reciveFromPvp(PvpChatMessage.class, 1000));

        battle.finishBattle();
        sender.disconnectFromMain();
        receiver.disconnectFromMain();
    }

    @Test
    public void testChatRestricted() throws Exception {
        long days = 7;
        restrictionService.addRestriction(user1Id, CHAT, days, 100, "testChatRestricted", "pvptest");

        WagerDuelBattle battle = new WagerDuelBattle(binarySerializer).setWager(BattleWager.WAGER_15_DUEL);
        battle.startBattle(user1Id, user2Id);

        PvpParticipant blocked = battle.getBattleParticipant(user1Id);
        PvpParticipant notBlocked = battle.getBattleParticipant(user2Id);

        // тот, кто под запретом, не может отправлять сообщения...
        PvpChatMessage msg = newMessageFrom(blocked, "hello", false);
        blocked.sendToPvp(msg);
        assertNull(notBlocked.reciveFromPvpNullable(PvpChatMessage.class, 1000));

        // ...а ему их отправлять могут
        msg = newMessageFrom(notBlocked, "blabla", false);
        notBlocked.sendToPvp(msg);
        assertSame(msg, blocked.reciveFromPvp(PvpChatMessage.class, 1000));

        battle.finishBattle();
        blocked.disconnectFromMain();
        notBlocked.disconnectFromMain();
    }

    private PvpChatMessage newMessageFrom(PvpParticipant from, String txt, boolean teamsMessage) {
        PvpChatMessage cmd = new PvpChatMessage();
        cmd.playerNum = from.getPlayerNum();
        cmd.message = txt;
        cmd.battleId = from.getBattleId();
        cmd.teamsMessage = teamsMessage;
        return cmd;
    }

    private void assertSame(PvpChatMessage exp, PvpChatMessage act) {
        assertEquals(exp.playerNum, act.playerNum);
        assertEquals(exp.message, act.message);
    }
}
