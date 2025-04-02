package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;

/**
 * Author: Vladimir
 * Date: 10.04.13 17:22
 * @see com.pragmatix.clanserver.controllers.ClanController#promoteInRank(com.pragmatix.clanserver.messages.request.PromoteInRankRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#promoteInRank(com.pragmatix.clanserver.messages.request.PromoteInRankRequest, com.pragmatix.clanserver.domain.ClanMember)
 */
@Command(Messages.PROMOTE_IN_RANK_REQUEST)
public class PromoteInRankRequest extends AbstractRequest {
    /**
     * идентификатор социальной сети
     */
    @Ignore
    public short socialId;

    /**
     * Идентификатор игрока, повышаемого в звании
     */
    public int profileId;

    /**
     * до какого звания повысить
     */
    public Rank targetRank;

    @Ignore
    public int adminUserId;

    @Override
    public int getCommandId() {
        return Messages.PROMOTE_IN_RANK_REQUEST;
    }

    public PromoteInRankRequest() {
    }

    public PromoteInRankRequest(short socialId, int profileId, Rank targetRank) {
        this.socialId = socialId;
        this.profileId = profileId;
        this.targetRank = targetRank;
    }

    public PromoteInRankRequest(short socialId, int profileId, Rank targetRank, int adminUserId) {
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
