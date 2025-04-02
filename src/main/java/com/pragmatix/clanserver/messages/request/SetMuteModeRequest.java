package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.clanserver.controllers.ClanController#setMuteMode(SetMuteModeRequest, ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#setMuteMode(int, boolean, ClanMember)
 */
@Command(Messages.SET_MUTE_MODE_REQUEST)
public class SetMuteModeRequest extends AbstractRequest{

    public int profileId;

    public boolean value;

    @Override
    public int getCommandId() {
        return Messages.SET_MUTE_MODE_REQUEST;
    }

    public SetMuteModeRequest() {
    }

    public SetMuteModeRequest(int profileId, boolean value) {
        this.profileId = profileId;
        this.value = value;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("profileId=").append(profileId)
                .append("value=").append(value)
                ;
    }
}
