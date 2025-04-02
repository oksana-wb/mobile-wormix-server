package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.PromoteInRankRequest;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;

/**
 * Author: Vladimir
 * Date: 10.04.13 17:33
 */
@Command(Messages.PROMOTE_IN_RANK_RESPONSE)
public class PromoteInRankResponse extends CommonResponse<PromoteInRankRequest> {
    /**
     * идентификатор социальной сети
     */
    @Ignore
    public short socialId;

    /**
     * Идентификатор игрока
     */
    public int profileId;

    /**
     * Новое звание
     */
    public Rank rank;

    public PromoteInRankResponse() {
    }

    public PromoteInRankResponse(PromoteInRankRequest request) {
        super(request);

        socialId = request.socialId;
        profileId = request.profileId;
    }

    @Override
    protected StringBuilder propertiesString() {
        return super.propertiesString()
                .append(", profileId=").append(profileId)
                .append(", rank=").append(rank)
                ;
    }
}
