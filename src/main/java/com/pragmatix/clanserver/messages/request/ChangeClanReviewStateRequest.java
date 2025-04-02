package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.domain.ReviewState;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 23.05.13 16:29
 *
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#changeClanReviewState(com.pragmatix.clanserver.messages.request.ChangeClanReviewStateRequest)
 * @see com.pragmatix.clanserver.messages.response.ChangeClanReviewStateResponse
 */
@Command(Messages.CHANGE_CLAN_REVIEW_STATE_REQUEST)
public class ChangeClanReviewStateRequest extends AbstractRequest {

    public int clanId;

    public ReviewState reviewState;

    @Override
    public int getCommandId() {
        return Messages.CHANGE_CLAN_REVIEW_STATE_REQUEST;
    }

    public ChangeClanReviewStateRequest() {
    }

    public ChangeClanReviewStateRequest(int clanId, ReviewState reviewState) {
        this.clanId = clanId;
        this.reviewState = reviewState;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("clanId=").append(clanId)
                .append(", reviewState=").append(reviewState)
                ;
    }
}
