package com.pragmatix.pvp.dsl;

import com.pragmatix.app.settings.BattleAwardSettings;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.PvpParticipant;
import com.pragmatix.pvp.messages.handshake.client.CreateBattleRequest;
import com.pragmatix.serialization.AppBinarySerializer;
import io.netty.channel.Channel;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 19.10.2017 13:42
 */
public class PvePartnerBattle extends BossBattle {

    private short[] missionIds;

    public PvePartnerBattle(AppBinarySerializer binarySerializer, short[] missionIds) {
        super(binarySerializer);
        this.missionIds = missionIds;
    }

    public PvePartnerBattle(AppBinarySerializer binarySerializer, Map<PvpParticipant, Channel> mainChannelsMap, short[] missionIds) {
        super(binarySerializer, mainChannelsMap);
        this.missionIds = missionIds;
    }

    @Override
    public PvpBattleType getBattleType() {
        return getBattleWager().battleType;
    }

    @Override
    public BattleWager getBattleWager() {
        return BattleWager.PvE_PARTNER;
    }

    @Override
    protected CreateBattleRequest createBattleRequest(PvpParticipant user) {
        CreateBattleRequest battleRequest = super.createBattleRequest(user);
        battleRequest.missionIds = missionIds;
        return battleRequest;
    }

}
