package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.ChangeClanReviewStateRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 23.05.13 16:31
 *
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#changeClanReviewState(com.pragmatix.clanserver.messages.request.ChangeClanReviewStateRequest)
 * @see com.pragmatix.clanserver.messages.request.ChangeClanReviewStateRequest
 */
@Command(Messages.CHANGE_CLAN_REVIEW_STATE_RESPONSE)
public class ChangeClanReviewStateResponse extends CommonResponse<ChangeClanReviewStateRequest> {
    public ChangeClanReviewStateResponse() {
    }

    public ChangeClanReviewStateResponse(ChangeClanReviewStateRequest request) {
        super(request);
    }
}
