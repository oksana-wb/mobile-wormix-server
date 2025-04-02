package com.pragmatix.pvp.messages.handshake.client;

import com.pragmatix.app.services.BattleService;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Arrays;

/**
 * Команда логина на сервер PVP c запросом боя
 *
 * @see com.pragmatix.pvp.filters.PvpAuthFilter#authenticate(CreateBattleRequest)
 * @see com.pragmatix.pvp.controllers.PvpLoginController#onLogin(Object, com.pragmatix.pvp.model.PvpUser)
 */
@Command(1001)
public class CreateBattleRequest extends PvpLogin {
    /**
     * тип боя
     */
//    public PvpBattleType battleType;
    /**
     * id карты на которой будем играть
     */
    @Resize(TypeSize.UINT32)
    public long mapId;
    /**
     * ставка игрока
     * при "не нулевой" заявке поле battleType игнорируется
     */
    public BattleWager wager;
    /**
     * id боссов для совместного прохождения
     */
    public short[] missionIds;
    /**
     * количество отрядов в миссии с боссом
     */
    @Ignore
    public byte missionTeamSize;
    /**
     * участники;  participants[0] - инициатор боя
     */
    @Resize(TypeSize.UINT32)
    public long[] participants;
    /**
     * распределение по командам
     */
    public byte[] teamIds;

    public String clientParams;

//    public short questId;
    /**
     * id социальной сети вызываемого друга
     * востребовано только на тесте
     */
    public byte friendSocialNetId;

    @Override
    public String toString() {
        return "CreateBattleRequest{" +
                super.toString() +
//                ", battleType=" + battleType +
                ", wager=" + wager +
                ", mapId=" + mapId +
//                ", questId=" + questId +
                ", missionIds=" + Arrays.toString(missionIds) +
                ", missionTeamSize=" + missionTeamSize +
                ", participants=" + Arrays.toString(participants) +
                ", friendSocialNetId=" + friendSocialNetId +
                ", teams=" + Arrays.toString(teamIds) +
                ", clientParams=" + clientParams +
                ", secureResult=" + secureResult +
                '}';
    }

    public boolean isSingleBossBattle(){
        return BattleService.isSingleBossBattle(missionIds);
    }

    public boolean isSuperBossBattle(){
        return BattleService.isSuperBossBattle(missionIds);
    }

    public short getMissionId(){
        return isSingleBossBattle() ? missionIds[0] : 0;
    }

}
