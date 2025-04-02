package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.LowerInRankRequest;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;

/**
 * Author: Vladimir
 * Date: 12.04.13 11:55
 */
@Command(Messages.LOWER_IN_RANK_RESPONSE)
public class LowerInRankResponse extends CommonResponse<LowerInRankRequest> {
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

    public LowerInRankResponse() {
    }

    public LowerInRankResponse(LowerInRankRequest request) {
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