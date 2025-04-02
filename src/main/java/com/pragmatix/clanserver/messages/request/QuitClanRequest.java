package com.pragmatix.clanserver.messages.request;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 16.04.13 10:23
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#quitClan(QuitClanRequest, UserProfile) 
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#quitClan(com.pragmatix.clanserver.messages.request.QuitClanRequest, com.pragmatix.clanserver.domain.ClanMember)
 */
@Command(Messages.QUIT_CLAN_REQUEST)
public class QuitClanRequest extends AbstractRequest {
    @Override
    public int getCommandId() {
        return Messages.QUIT_CLAN_REQUEST;
    }
}
