package com.pragmatix.pvp.dsl;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.PvpParticipant;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.handshake.client.CreateBattleRequest;
import com.pragmatix.pvp.messages.handshake.client.ReadyForBattle;
import com.pragmatix.pvp.messages.handshake.server.BattleCreated;
import com.pragmatix.pvp.messages.handshake.server.StartPvpBattle;
import com.pragmatix.serialization.AppBinarySerializer;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.07.13 12:14
 */
public abstract class BossBattle {

    private final AppBinarySerializer binarySerializer;
    public Map<PvpParticipant, Channel> mainChannelsMap;
    public PvpParticipant participant1;
    public PvpParticipant participant2;
    protected PvpParticipant[] battleParticipants;

    public BossBattle(AppBinarySerializer binarySerializer) {
        this.binarySerializer = binarySerializer;
        this.mainChannelsMap = new HashMap<>();
    }

    public BossBattle(AppBinarySerializer binarySerializer, Map<PvpParticipant, Channel> mainChannelsMap) {
        this.binarySerializer = binarySerializer;
        this.mainChannelsMap = mainChannelsMap;
    }

    public abstract PvpBattleType getBattleType();

    public abstract BattleWager getBattleWager();

    public BossBattle loginMain(long userId1, long userId2) throws Exception {
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

    public BossBattle startBattle() throws Exception {
        for(PvpParticipant user : battleParticipants) {
            user.connectToPvp();
        }

        for(PvpParticipant user : battleParticipants) {
            CreateBattleRequest createBattleRequest = createBattleRequest(user);
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
                participant1 = battleParticipant;
            } else {
                participant2 = battleParticipant;
            }
        }

        assertNotNull(participant1);
        assertNotNull(participant2);
        return this;
    }

    protected CreateBattleRequest createBattleRequest(PvpParticipant user) {
        return user.сreateBattleRequest(getBattleType(), getBattleWager());
    }

    public BossBattle startBattle(long user1Id1, long user2Id1) throws Exception {
        return loginMain(user1Id1, user2Id1).startBattle();
    }

    public BossBattle winBattle() {
        participant1.sendActionEx();

        participant1.sendEndTurn(0, "", new byte[0], new PvpParticipant(2), new PvpParticipant(3));
        participant2.confirmTurnTransfer(participant2.reciveFromPvp(PvpEndTurn.class, 300));

        participant2.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        participant2.disconnectFromPvp();

        participant1.finishBattleWithResult(PvpBattleResult.WINNER, 300, true);
        participant1.disconnectFromPvp();

        return this;
    }

    public void disconnectFromPvp() {
        for(PvpParticipant user : battleParticipants) {
            user.disconnectFromPvp();
        }
    }

    public PvpParticipant getParticipant1() {
        return participant1;
    }

    public PvpParticipant getParticipant2() {
        return participant2;
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
