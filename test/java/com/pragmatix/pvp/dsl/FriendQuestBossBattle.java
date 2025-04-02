package com.pragmatix.pvp.dsl;

import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.PvpParticipant;
import com.pragmatix.pvp.messages.handshake.client.CreateBattleRequest;
import com.pragmatix.pvp.messages.handshake.client.ReadyForBattle;
import com.pragmatix.pvp.messages.handshake.server.BattleCreated;
import com.pragmatix.pvp.messages.handshake.server.CallToBattle;
import com.pragmatix.pvp.messages.handshake.server.StartPvpBattle;
import com.pragmatix.serialization.AppBinarySerializer;
import io.netty.channel.Channel;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 07.04.2016 16:36
 */
public class FriendQuestBossBattle extends QuestBossBattle {

    public FriendQuestBossBattle(int questId, AppBinarySerializer binarySerializer, Map<PvpParticipant, Channel> mainChannelsMap) {
        super(questId, binarySerializer, mainChannelsMap);
        this.battleWager = Arrays.stream(BattleWager.values()).filter(w -> w.questId == questId && w.battleType == PvpBattleType.PvE_FRIEND).findFirst().get();
    }

    @Override
    public PvpBattleType getBattleType() {
        return PvpBattleType.PvE_FRIEND;
    }

    @Override
    public BossBattle startBattle() throws Exception {
        for(PvpParticipant user : battleParticipants) {
            user.connectToPvp();
        }

        CreateBattleRequest createBattleRequest = createBattleRequest(battleParticipants[0]);
        battleParticipants[0].sendToPvp(createBattleRequest);
        Thread.sleep(100);

        CallToBattle callToBattle = battleParticipants[1].reciveFromMain(CallToBattle.class, 300);
        battleParticipants[1].sendJoinToBattle(callToBattle);

        for(PvpParticipant user : battleParticipants) {
            BattleCreated battleCreated = user.reciveFromPvp(BattleCreated.class, 300);
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

    @Override
    protected CreateBattleRequest createBattleRequest(PvpParticipant user) {
        CreateBattleRequest request = super.createBattleRequest(user);
        request.participants = new long[]{battleParticipants[0].getUserId(), battleParticipants[1].getUserId()};
        return request;
    }

}
