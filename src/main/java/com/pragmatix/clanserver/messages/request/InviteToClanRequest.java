package com.pragmatix.clanserver.messages.request;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;

/**
 * Author: Vladimir
 * Date: 09.04.13 9:37
 *
 * @see com.pragmatix.clanserver.services.ClanService#inviteToClan(InviteToClanRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.controllers.ClanController#inviteToClan(InviteToClanRequest, UserProfile)
 */
@Command(Messages.INVITE_TO_CLAN_REQUEST)
public class InviteToClanRequest extends AbstractRequest {

    /**
     * идентификатор клана
     */
    public int clanId;

    /**
     * идентификатор социальной сети
     */
    @Ignore
    public short socialId;

    /**
     * Идентификатор игрока
     */
    public int profileId;

    @Override
    public int getCommandId() {
        return Messages.INVITE_TO_CLAN_REQUEST;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("clanId=").append(clanId)
                .append(", profileId=").append(profileId);
    }
}
