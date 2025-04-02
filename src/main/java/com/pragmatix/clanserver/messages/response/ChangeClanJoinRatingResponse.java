package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.ChangeClanJoinRatingRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 04.06.13 11:13
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#changeClanJoinRating(com.pragmatix.clanserver.messages.request.ChangeClanJoinRatingRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#changeClanJoinRating(com.pragmatix.clanserver.messages.request.ChangeClanJoinRatingRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.request.ChangeClanJoinRatingRequest
 */
@Command(Messages.CHANGE_CLAN_JOIN_RATING_RESPONSE)
public class ChangeClanJoinRatingResponse extends CommonResponse<ChangeClanJoinRatingRequest> {

    public ChangeClanJoinRatingResponse() {
    }

    public ChangeClanJoinRatingResponse(ChangeClanJoinRatingRequest request) {
        super(request);
    }
}
