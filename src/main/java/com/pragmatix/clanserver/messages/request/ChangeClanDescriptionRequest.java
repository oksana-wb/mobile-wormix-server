package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 08.05.13 14:55
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#changeClanDescriptions(ChangeClanDescriptionRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#changeClanDescription(ChangeClanDescriptionRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.response.ChangeClanDescriptionResponse
 */
@Command(Messages.CHANGE_CLAN_DESCRIPTION_REQUEST)
public class ChangeClanDescriptionRequest extends AbstractRequest{

    public String description;

    public boolean fromTreas;

    @Override
    public int getCommandId() {
        return Messages.CHANGE_CLAN_DESCRIPTION_REQUEST;
    }

    public ChangeClanDescriptionRequest() {
    }

    public ChangeClanDescriptionRequest(String description) {
        this.description = description;
    }

    public ChangeClanDescriptionRequest(String description, boolean fromTreas) {
        this.description = description;
        this.fromTreas = fromTreas;
    }

    @Override
    protected StringBuilder propertiesString() {
        return super.propertiesString()
                .append("description=").append(description)
                .append("fromTreas=").append(fromTreas)
                ;
    }
}
