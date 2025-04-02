package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.DeleteClanRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 25.04.13 10:42
 * @see com.pragmatix.clanserver.services.ClanService#deleteClan(DeleteClanRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.controllers.ClanController#deleteClan(DeleteClanRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.request.DeleteClanRequest
 */
@Command(Messages.DELETE_CLAN_RESPONSE)
public class DeleteClanResponse extends CommonResponse<DeleteClanRequest> {

    public int comeback;

    public DeleteClanResponse() {
    }

    public DeleteClanResponse(DeleteClanRequest request) {
        super(request);
    }

    @Override
    public String toString() {
        return "DeleteClanResponse{" +
                "serviceResult=" + serviceResult +
                ", comeback=" + comeback +
                '}';
    }

}
