package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;

/**
 * Author: Vladimir
 * Date: 05.04.13 18:30
 *
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#expelFromClan(ExpelFromClanRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.controllers.ClanController#expelFromClan(ExpelFromClanRequest, com.pragmatix.clanserver.domain.ClanMember)
 */
@Command(Messages.EXPELL_FROM_CLAN_REQUEST)
public class ExpelFromClanRequest extends AbstractRequest {
    /**
     * идентификатор социальной сети
     */
    @Ignore
    public short socialId;

    /**
     * Идентификатор игрока
     */
    public int profileId;

    @Ignore
    public int adminUserId;

    public ExpelFromClanRequest() {
    }

    public ExpelFromClanRequest(short socialId, int profileId) {
        this.socialId = socialId;
        this.profileId = profileId;
    }

    public ExpelFromClanRequest(short socialId, int profileId, int adminUserId) {
        this.socialId = socialId;
        this.profileId = profileId;
        this.adminUserId = adminUserId;
    }

    @Override
    public int getCommandId() {return Messages.EXPELL_FROM_CLAN_REQUEST;}

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("profileId=").append(profileId);
    }
}
