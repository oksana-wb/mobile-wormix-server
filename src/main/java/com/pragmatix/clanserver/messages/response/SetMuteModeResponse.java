package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.ServiceResult;
import com.pragmatix.clanserver.messages.request.SetMuteModeRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.clanserver.controllers.ClanController#setMuteMode(SetMuteModeRequest, ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#setMuteMode(int, boolean, ClanMember)
 */
@Command(Messages.SET_MUTE_MODE_RESPONSE)
public class SetMuteModeResponse extends CommonResponse<SetMuteModeRequest> {

    public SetMuteModeResponse() {
    }

    public SetMuteModeResponse(SetMuteModeRequest request) {
        super(request);
    }

    public SetMuteModeResponse(ServiceResult serviceResult, SetMuteModeRequest request) {
        super(serviceResult, request, "");
    }
}
