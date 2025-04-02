package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 25.04.13 10:37
 *
 * @see com.pragmatix.clanserver.services.ClanService#deleteClan(DeleteClanRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.controllers.ClanController#deleteClan(DeleteClanRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.response.DeleteClanResponse
 */
@Command(Messages.DELETE_CLAN_REQUEST)
public class DeleteClanRequest extends AbstractRequest {

    @Override
    public int getCommandId() {
        return Messages.DELETE_CLAN_REQUEST;
    }

    public DeleteClanRequest() {
    }
}
