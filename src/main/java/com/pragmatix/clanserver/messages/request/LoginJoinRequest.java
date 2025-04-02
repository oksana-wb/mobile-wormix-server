package com.pragmatix.clanserver.messages.request;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;

/**
 * Author: Vladimir
 * Date: 08.04.13 12:11
 *
 * @see com.pragmatix.clanserver.controllers.ClanAuthController#onLoginJoinRequest(LoginJoinRequest, UserProfile)
 */
@Command(Messages.LOGIN_JOIN_REQUEST)
public class LoginJoinRequest extends LoginBase {
    /**
     * идентификатор клана
     */
    public int clanId;

    /**
     * идентификатор социальной сети пригласившего игрока
     */
    @Ignore
    public short hostSocialId;

    /**
     * идентификатор пригласившего игрока
     */
    public int hostProfileId;

    @Override
    public int getCommandId() {
        return Messages.LOGIN_JOIN_REQUEST;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("clanId=").append(clanId)
                .append(", hostProfileId=").append(hostProfileId);
    }
}
