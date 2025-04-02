package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.clanserver.controllers.ClanController#changeClanClosedState(ChangeClanClosedStateRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#changeClanClosedState(ChangeClanClosedStateRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.response.ChangeClanClosedStateResponse
 */
@Command(Messages.CHANGE_CLAN_CLOSED_STATE_REQUEST)
public class ChangeClanClosedStateRequest extends AbstractRequest {

    public boolean closed;

    @Override
    public int getCommandId() {
        return Messages.CHANGE_CLAN_CLOSED_STATE_REQUEST;
    }

    public ChangeClanClosedStateRequest() {
    }

    public ChangeClanClosedStateRequest(boolean closed) {
        this.closed = closed;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("closed=").append(closed)
                ;
    }
}
