package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.UpdateRatingRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 23.04.13 16:27
 */
@Command(Messages.UPDATE_RATING_RESPONSE)
public class UpdateRatingResponse extends CommonResponse<UpdateRatingRequest> {
    public UpdateRatingResponse() {
    }

    public UpdateRatingResponse(UpdateRatingRequest request) {
        super(request);
    }
}
