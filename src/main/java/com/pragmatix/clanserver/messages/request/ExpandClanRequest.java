package com.pragmatix.clanserver.messages.request;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 12.04.13 12:43
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#expandClan(ExpandClanRequest, UserProfile)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#expandClan(ExpandClanRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.response.ExpandClanResponse
 */
@Command(Messages.EXPAND_CLAN_REQUEST)
public class ExpandClanRequest extends AbstractRequest {

    public int level;

    public boolean fromTreas;

    @Override
    public int getCommandId() {
        return Messages.EXPAND_CLAN_REQUEST;
    }

    public ExpandClanRequest() {
    }

    public ExpandClanRequest(int level) {
        this.level = level;
    }

    public ExpandClanRequest(int level, boolean fromTreas) {
        this.level = level;
        this.fromTreas = fromTreas;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("level=").append(level)
                .append("fromTreas=").append(fromTreas);
    }
}
