package com.pragmatix.clanserver.messages.response;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.RenameClanRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 08.05.13 14:55
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#renameClan(RenameClanRequest, UserProfile)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#renameClan(com.pragmatix.clanserver.messages.request.RenameClanRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.request.RenameClanRequest
 */
@Command(Messages.RENAME_CLAN_RESPONSE)
public class RenameClanResponse extends CommonResponse<RenameClanRequest> {

    public RenameClanResponse() {
    }

    public RenameClanResponse(RenameClanRequest request) {
        super(request);
    }
}
