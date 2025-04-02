package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 21.05.13 16:03
 *
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#topClans(com.pragmatix.clanserver.messages.request.TopClansRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.controllers.ClanController#topClans(com.pragmatix.clanserver.messages.request.TopClansRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.response.TopClansResponse
 */
@Command(Messages.TOP_CLANS_REQUEST)
public class TopClansRequest extends AbstractRequest {

    public boolean season;

    public TopClansRequest() {
    }

    public TopClansRequest(boolean season) {
        this.season = season;
    }

    @Override
    public int getCommandId() {
        return Messages.TOP_CLANS_REQUEST;
    }

    @Override
    public String toString() {
        return "TopClansRequest{" +
                "season=" + season +
                '}';
    }

}
