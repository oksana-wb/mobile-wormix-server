package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.ServiceResult;
import com.pragmatix.clanserver.messages.request.SetExpelPermitRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.clanserver.controllers.ClanController#setExpelPermit(SetExpelPermitRequest, ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#setExpelPermit(int, boolean, ClanMember)
 */
@Command(Messages.SET_EXPEL_PERMIT_RESPONSE)
public class SetExpelPermitResponse extends CommonResponse<SetExpelPermitRequest> {

    public SetExpelPermitResponse() {
    }

    public SetExpelPermitResponse(SetExpelPermitRequest request) {
        super(request);
    }

    public SetExpelPermitResponse(ServiceResult serviceResult, SetExpelPermitRequest request) {
        super(serviceResult, request, "");
    }
}
