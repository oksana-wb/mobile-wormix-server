package com.pragmatix.pvp.dsl;

import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.PvpParticipant;
import com.pragmatix.pvp.messages.handshake.client.CreateBattleRequest;
import com.pragmatix.serialization.AppBinarySerializer;
import io.netty.channel.Channel;

import java.util.Arrays;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.12.2015 18:29
 */
public class QuestBossBattle extends BossBattle {

    protected BattleWager battleWager;

    public QuestBossBattle(int questId, AppBinarySerializer binarySerializer, Map<PvpParticipant, Channel> mainChannelsMap) {
        super(binarySerializer, mainChannelsMap);
        this.battleWager = Arrays.stream(BattleWager.values()).filter(w -> w.questId == questId && w.battleType == PvpBattleType.PvE_PARTNER).findFirst().get();
    }

    @Override
    public PvpBattleType getBattleType() {
        return battleWager.battleType;
    }

    @Override
    public BattleWager getBattleWager() {
        return battleWager;
    }

}
