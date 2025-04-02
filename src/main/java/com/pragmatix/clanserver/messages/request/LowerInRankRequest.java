package com.pragmatix.clanserver.messages.request;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;

/**
 * Author: Vladimir
 * Date: 12.04.13 11:52
 *
 * @see com.pragmatix.clanserver.services.ClanService#lowerInRank(com.pragmatix.clanserver.messages.request.LowerInRankRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.controllers.ClanController#lowerInRank(LowerInRankRequest, UserProfile)
 */
@Command(Messages.LOWER_IN_RANK_REQUEST)
public class LowerInRankRequest extends AbstractRequest {
    /**
     * идентификатор социальной сети
     */
    @Ignore
    public short socialId;

    /**
     * Идентификатор игрока, понижаемого в звании
     */
    public int profileId;

    /**
     * до какого звания понизить
     */
    public Rank targetRank;

    @Ignore
    public int adminUserId;

    @Override
    public int getCommandId() {
        return Messages.LOWER_IN_RANK_REQUEST;
    }

    public LowerInRankRequest() {
    }

    public LowerInRankRequest(short socialId, int profileId, Rank targetRank) {
        this.socialId = socialId;
        this.profileId = profileId;
        this.targetRank = targetRank;
    }

    public LowerInRankRequest(short socialId, int profileId, Rank targetRank, int adminUserId) {
        this.socialId = socialId;
        this.profileId = profileId;
        this.targetRank = targetRank;
        this.adminUserId = adminUserId;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("profileId=").append(profileId)
                .append(", targetRank=").append(targetRank)
                ;
    }
}
