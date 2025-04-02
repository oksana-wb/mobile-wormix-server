package com.pragmatix.pvp.dsl;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.common.utils.VarObject;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpParticipant;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.handshake.client.CreateBattleRequest;
import com.pragmatix.pvp.messages.handshake.client.ReadyForBattle;
import com.pragmatix.pvp.messages.handshake.server.BattleCreated;
import com.pragmatix.pvp.messages.handshake.server.StartPvpBattle;
import com.pragmatix.serialization.AppBinarySerializer;
import io.netty.channel.Channel;

import javax.validation.constraints.Null;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.07.13 12:14
 */
public class WagerDuelBattle {

    private final AppBinarySerializer binarySerializer;
    public Map<PvpParticipant, Channel> mainChannelsMap;
    private PvpParticipant sender;
    private PvpParticipant receiver;
    public PvpParticipant[] battleParticipants;
    private BattleWager battleWager = BattleWager.WAGER_15_DUEL;

    public WagerDuelBattle(AppBinarySerializer binarySerializer) {
        this.binarySerializer = binarySerializer;
        this.mainChannelsMap = new HashMap<>();
    }

    public WagerDuelBattle(AppBinarySerializer binarySerializer, Map<PvpParticipant, Channel> mainChannelsMap) {
        this.binarySerializer = binarySerializer;
        this.mainChannelsMap = mainChannelsMap;
    }

    public WagerDuelBattle setWager(BattleWager battleWager) {
        this.battleWager = battleWager;
        return this;
    }

    public void failureStartBattle(long user1Id1, long user2Id1) throws Exception {
        loginMain(user1Id1, user2Id1);

        for(PvpParticipant user : battleParticipants) {
            user.connectToPvp();
        }

        for(PvpParticipant user : battleParticipants) {
            user.sendCreateBattleRequest(battleWager);
            Thread.sleep(100);
        }

        for(PvpParticipant user : battleParticipants) {
            BattleCreated battleCreated = user.reciveFromPvpNullable(BattleCreated.class, 1000);
            assertNull(battleCreated);
        }
    }

    public WagerDuelBattle loginMain(long userId1, long userId2) throws Exception {
        PvpParticipant user1 = new PvpParticipant(userId1, binarySerializer);
        PvpParticipant user2 = new PvpParticipant(userId2, binarySerializer);

        battleParticipants = new PvpParticipant[]{user1, user2};

        for(PvpParticipant user : battleParticipants) {
            if(mainChannelsMap.containsKey(user) && mainChannelsMap.get(user).isActive()) {
                user.setMainChannel(mainChannelsMap.get(user));
            } else {
                user.loginMain();
                mainChannelsMap.put(user, user.getMainChannel());
            }
        }
        return this;
    }

    public WagerDuelBattle startBattle() throws Exception {
        for(PvpParticipant user : battleParticipants) {
            user.connectToPvp();
        }

        for(PvpParticipant user : battleParticipants) {
            CreateBattleRequest createBattleRequest = сreateBattleRequest(user, battleWager);
            user.sendToPvp(createBattleRequest);

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
            } else {
                receiver = battleParticipant;
            }
        }
        assertNotNull(sender);
        assertNotNull(receiver);
        return this;
    }

    protected CreateBattleRequest сreateBattleRequest(PvpParticipant user, BattleWager wager) {
        return user.сreateBattleRequest(wager);
    }

    public WagerDuelBattle startBattle(long user1Id1, long user2Id1) throws Exception {
        return loginMain(user1Id1, user2Id1).startBattle();
    }

    public void finishBattle(@Null VarObject<PvpParticipant> winner, @Null VarObject<PvpParticipant> looser, int droppedUnits) {
        if (sender.isEndBattle() && receiver.isEndBattle()) {
            return;
        }

        sender.sendActionEx();

        sender.sendEndTurn(0, "", new byte[]{receiver.getPlayerNum(), (byte) droppedUnits});
        receiver.confirmTurnTransfer(receiver.reciveFromPvp(PvpEndTurn.class, 300));
        for(PvpParticipant battleParticipant : battleParticipants) {
            battleParticipant.consumeStartTurn();
        }

        receiver.sendActionEx();

        receiver.sendEndTurn(0, "", new byte[0]);
        sender.confirmTurnTransfer(sender.reciveFromPvp(PvpEndTurn.class, 300));
        for(PvpParticipant battleParticipant : battleParticipants) {
            battleParticipant.consumeStartTurn();
        }

        receiver.surrender();
        receiver.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);
        receiver.disconnectFromPvp();
        receiver.disconnectFromMain();

        sender.finishBattleWithResult(PvpBattleResult.WINNER, 2000, true);
        sender.disconnectFromPvp();
        sender.disconnectFromMain();

        if(winner != null && looser != null) {
            winner.value = sender;
            looser.value = receiver;
        }
    }

    public PvpParticipant[] finishBattle(int droppedUnits) {
        VarObject<PvpParticipant> winner = new VarObject<>();
        VarObject<PvpParticipant> looser = new VarObject<>();
        finishBattle(winner, looser, droppedUnits);
        return new PvpParticipant[]{winner.value, looser.value};
    }

    public PvpParticipant[] finishBattle() {
        VarObject<PvpParticipant> winner = new VarObject<>();
        VarObject<PvpParticipant> looser = new VarObject<>();
        finishBattle(winner, looser, 0);
        return new PvpParticipant[]{winner.value, looser.value};
    }

    public void drawBattle() {
        sender.sendEndTurn(sender, receiver);
        PvpEndTurn pvpEndTurn = receiver.reciveFromPvp(PvpEndTurn.class, 300);
        receiver.confirmTurnTransfer(pvpEndTurn);

        sender.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
        sender.disconnectFromPvp();

        receiver.finishBattleWithResult(PvpBattleResult.DRAW_GAME, 300, true);
        receiver.disconnectFromPvp();
    }

    public WagerDuelBattle winBattle(long winnerId) {
        PvpParticipant winner;
        PvpParticipant looser;
        if(sender.getUserId() == winnerId) {
            winner = sender;
            looser = receiver;
        } else {
            winner = receiver;
            looser = sender;

        }
        looser.surrender();
        looser.finishBattleWithResult(PvpBattleResult.NOT_WINNER, 300, false);
        looser.disconnectFromPvp();

        winner.finishBattleWithResult(PvpBattleResult.WINNER, 2000, true);
        winner.disconnectFromPvp();
        return  this;
    }

    public void disconnectFromPvp() {
        for(PvpParticipant user : battleParticipants) {
            user.disconnectFromPvp();
        }
    }

    public void disconnectFromMain() {
        for(PvpParticipant user : battleParticipants) {
            user.disconnectFromMain();
        }
    }

    public PvpParticipant getSender() {
        return sender;
    }

    public PvpParticipant getReceiver() {
        return receiver;
    }

    public PvpParticipant[] getBattleParticipants() {
        return battleParticipants;
    }

    public PvpParticipant getBattleParticipant(long profileId) {
        PvpParticipant result = null;
        for(PvpParticipant battleParticipant : battleParticipants) {
            if(battleParticipant.getUserId() == profileId) {
                result = battleParticipant;
                break;
            }
        }
        assertNotNull(result);
        return result;
    }

}
