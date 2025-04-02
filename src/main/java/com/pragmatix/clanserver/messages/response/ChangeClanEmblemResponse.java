package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.ChangeClanEmblemRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 08.05.13 14:55
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#changeClanEmblem(com.pragmatix.clanserver.messages.request.ChangeClanEmblemRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#changeClanEmblem(com.pragmatix.clanserver.messages.request.ChangeClanEmblemRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.request.ChangeClanEmblemRequest
 */
@Command(Messages.CHANGE_CLAN_EMBLEM_RESPONSE)
public class ChangeClanEmblemResponse extends CommonResponse<ChangeClanEmblemRequest> {

    public ChangeClanEmblemResponse() {
    }

    public ChangeClanEmblemResponse(ChangeClanEmblemRequest request) {
        super(request);
    }
}
