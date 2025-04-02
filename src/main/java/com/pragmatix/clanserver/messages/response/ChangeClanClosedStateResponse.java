package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.ChangeClanClosedStateRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#changeClanClosedState(com.pragmatix.clanserver.messages.request.ChangeClanClosedStateRequest, com.pragmatix.clanserver.domain.ClanMember)
 */
@Command(Messages.CHANGE_CLAN_CLOSED_STATE_RESPONSE)
public class ChangeClanClosedStateResponse extends CommonResponse<ChangeClanClosedStateRequest> {
    public ChangeClanClosedStateResponse() {
    }

    public ChangeClanClosedStateResponse(ChangeClanClosedStateRequest request) {
        super(request);
    }
}
