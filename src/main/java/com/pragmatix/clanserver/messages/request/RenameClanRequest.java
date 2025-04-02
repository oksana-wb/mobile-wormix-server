package com.pragmatix.clanserver.messages.request;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 08.05.13 14:55
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#renameClan(RenameClanRequest, UserProfile)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#renameClan(RenameClanRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.response.RenameClanResponse
 */
@Command(Messages.RENAME_CLAN_REQUEST)
public class RenameClanRequest extends AbstractRequest{

    public String name;

    public boolean fromTreas;

    @Override
    public int getCommandId() {
        return Messages.RENAME_CLAN_REQUEST;
    }

    public RenameClanRequest() {
    }

    public RenameClanRequest(String name) {
        this.name = name;
    }

    public RenameClanRequest(String name, boolean fromTreas) {
        this.name = name;
        this.fromTreas = fromTreas;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("name='").append(name).append('\'')
                .append(", fromTreas=").append(fromTreas)
                ;
    }
}
