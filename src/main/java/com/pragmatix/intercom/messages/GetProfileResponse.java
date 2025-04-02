package com.pragmatix.intercom.messages;

import com.pragmatix.app.domain.TrueSkillEntity;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Serialize;
import jskills.Rating;

import javax.validation.constraints.Null;
import java.util.Arrays;

/**
 * Запрос профиля игрока с целью подбора соперника
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.05.12 11:45
 * @see com.pragmatix.pvp.controllers.PvpIntercomController#onGetProfileResponse(GetProfileResponse, com.pragmatix.sessions.IAppServer)
 */
@Command(4002)
public class GetProfileResponse implements IntercomResponseI, PvpCommandI {

    public byte socialNetId;

    public long requestId;

    public long battleId;

    public UserProfileStructure profileStructure;

    // текущее значение TrueSkill рейтинга

    /**
     * The statistical mean value of the rating (also known as μ). *
     */
    public double trueSkillMean;

    /**
     * The standard deviation (the spread) of the rating. This is also known as σ. *
     */
    public double trueSkillStandardDeviation;

    public int battlesCount;

    public int dailyRating;

    public int minDailyRating;

    public short[] backpackConf;

    public int heroicBossLevel = -1;
    /**
     * дополнительные параметры учитываемые при подборе. Сериализованные в JSON
     */
    public String[] auxMatchParams;

    public int version;

    public byte[] seasonsBestRank;
    /**
     * Битовая маска наложенных (в данную секунду) запретов
     */
    public short restrictionBlocks;

    public int bossWinAwardToken;

    public String clientAddress;

    public GetProfileResponse() {
    }

    public GetProfileResponse(GetProfileRequest request, UserProfileStructure profileStructure, TrueSkillEntity trueSkillEntity, int dailyRating, int minDailyRating
            , @Null short[] backpackConf, @Null String[] auxMatchParams, int version, short restrictionBlocks, int bossWinAwardToken) {
        this.dailyRating = dailyRating;
        this.requestId = request.getRequestId();
        this.socialNetId = request.getSocialNetId();
        this.profileStructure = profileStructure;
        this.battleId = request.battleId;
        this.trueSkillMean = trueSkillEntity.getMean();
        this.trueSkillStandardDeviation = trueSkillEntity.getStandardDeviation();
        this.battlesCount = trueSkillEntity.getBattles();
        this.minDailyRating = minDailyRating;
        this.backpackConf = backpackConf;
        this.auxMatchParams = auxMatchParams;
        this.version = version;
        this.restrictionBlocks = restrictionBlocks;
        this.bossWinAwardToken = bossWinAwardToken;
    }

    @Override
    public String toString() {
        return "GetProfileResponse{" +
                "socialNetId=" + socialNetId +
                ", battleId=" + battleId +
                ", version=" + AppUtils.versionToString(version) +
                ", dailyRating=" + dailyRating +
                ", minDailyRating=" + minDailyRating +
                ", battlesCount=" + battlesCount +
                ", rating=" + new Rating(trueSkillMean, trueSkillStandardDeviation) +
                ", heroicBossLevel=" + heroicBossLevel +
                ", auxMatchParams=" + Arrays.toString(auxMatchParams) +
                ", seasonsBestRank=" + Arrays.toString(seasonsBestRank) +
                ", bossWinAwardToken=" + bossWinAwardToken +
                ", clientAddress=" + clientAddress +
                ", restrictionBlocks=0x" + Integer.toHexString(restrictionBlocks) +
                ", " + profileStructure +
                '}';
    }

    @Override
    public long getProfileId() {
        return profileStructure.id;
    }

    @Override
    public byte getSocialNetId() {
        return socialNetId;
    }

    @Override
    public long getRequestId() {
        return requestId;
    }

    @Override
    public long getBattleId() {
        return battleId;
    }

}
