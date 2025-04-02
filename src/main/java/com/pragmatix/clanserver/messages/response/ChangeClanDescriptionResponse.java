package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.ChangeClanDescriptionRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 08.05.13 14:55
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#changeClanDescriptions(com.pragmatix.clanserver.messages.request.ChangeClanDescriptionRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#changeClanDescription(com.pragmatix.clanserver.messages.request.ChangeClanDescriptionRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.request.ChangeClanDescriptionRequest
 */
@Command(Messages.CHANGE_CLAN_DESCRIPTION_RESPONSE)
public class ChangeClanDescriptionResponse extends CommonResponse<ChangeClanDescriptionRequest> {

    public ChangeClanDescriptionResponse() {
    }

    public ChangeClanDescriptionResponse(ChangeClanDescriptionRequest request) {
        super(request);
    }
}
