package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.ExpelFromClanRequest;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;

/**
 * Author: Vladimir
 * Date: 05.04.13 18:30
 */
@Command(Messages.EXPELL_FROM_CLAN_RESPONSE)
public class ExpelFromClanResponse extends CommonResponse<ExpelFromClanRequest> {
    /**
     * идентификатор социальной сети
     */
    @Ignore
    public short socialId;

    /**
     * Идентификатор игрока
     */
    public int profileId;

    public ExpelFromClanResponse() {
    }

    public ExpelFromClanResponse(ExpelFromClanRequest request) {
        super(request);
        socialId = request.socialId;
        profileId = request.profileId;
    }

    @Override
    protected StringBuilder propertiesString() {
        return super.propertiesString()
                .append("profileId=").append(profileId);
    }
}
