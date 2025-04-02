package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 04.06.13 11:12
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#changeClanJoinRating(ChangeClanJoinRatingRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#changeClanJoinRating(ChangeClanJoinRatingRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.response.ChangeClanJoinRatingResponse
 */
@Command(Messages.CHANGE_CLAN_JOIN_RATING_REQUEST)
public class ChangeClanJoinRatingRequest extends AbstractRequest{
    public int joinRating;

    @Override
    public int getCommandId() {
        return Messages.CHANGE_CLAN_DESCRIPTION_REQUEST;
    }

    public ChangeClanJoinRatingRequest() {
    }

    public ChangeClanJoinRatingRequest(int joinRating) {
        this.joinRating = joinRating;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("joinRating=").append(joinRating)
                ;
    }
}
