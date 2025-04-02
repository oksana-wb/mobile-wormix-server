package com.pragmatix.intercom.messages;

import com.pragmatix.app.common.BattleState;
import com.pragmatix.app.services.BattleService;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Arrays;

/**
 * Запрос профиля игрока с целью подбора соперника
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.05.12 11:45
 * @see com.pragmatix.app.controllers.AppIntercomController#onGetProfileRequest(GetProfileRequest, com.pragmatix.sessions.IAppServer)
 * @see GetProfileResponse
 * @see GetProfileError
 */
@Command(4000)
public class GetProfileRequest extends IntercomRequest {

    /**
     * условие выборки: минимальтный уровень
     */
    public int hasLevel;

    /**
     * выставить данное состояние при положительном статусе ответа
     */
    public BattleState setBattleState = BattleState.INDEFINITE;

    public long battleId;

    public BattleWager battleWager;

    /**
     * комбинированный id инициатора боя
     */
    public long battleCreatorPvpId;

    public short[] missionIds;

    public long mapId;

    public GetProfileRequest() {
    }

    public GetProfileRequest(long profileId, byte socialNetId) {
        this.requestId = REQUEST_SEQ.incrementAndGet();

        this.profileId = profileId;
        this.socialNetId = socialNetId;
    }

    public GetProfileRequest(long profileId, byte socialNetId, long battleId, BattleWager battleWager) {
        this.requestId = REQUEST_SEQ.incrementAndGet();

        this.profileId = profileId;
        this.socialNetId = socialNetId;
        this.battleId = battleId;
        this.battleWager = battleWager;
    }

    public GetProfileRequest hasLevel(int hasLevel) {
        this.hasLevel = hasLevel;
        return this;
    }

    public GetProfileRequest canGotoMission(short missionId) {
        this.missionIds = new short[]{missionId};
        return this;
    }

    public GetProfileRequest canGotoHeroicMission(short[] missionIds, long mapId) {
        this.missionIds = missionIds;
        this.mapId = mapId;
        return this;
    }

    public GetProfileRequest ifSuccessSet(BattleState setBattleState) {
        this.setBattleState = setBattleState;
        return this;
    }

    @Override
    public String toString() {
        return "GetProfileRequest{" +
                "user=" + socialNetId + ":" + profileId +
                ", battleId=" + battleId +
                ", battleWager=" + battleWager +
                (hasLevel > 0 ? ", hasLevel=" + hasLevel : "") +
                (missionIds != null && missionIds.length > 0 ? ", missionIds=" + Arrays.toString(missionIds) : "") +
                (mapId > 0 ? ", mapId=" + mapId : "") +
                ", setBattleState=" + setBattleState +
                ", battleCreator=" + PvpService.formatPvpUserId(battleCreatorPvpId) +
                ", requestId=" + requestId +
                '}';
    }


    public short getMissionId(){
        return BattleService.isSingleBossBattle(missionIds) ? missionIds[0] : 0;
    }
}
