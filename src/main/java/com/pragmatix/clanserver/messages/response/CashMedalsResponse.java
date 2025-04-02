package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.ServiceResult;
import com.pragmatix.clanserver.messages.request.CashMedalsRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.clanserver.controllers.ClanController#cashMedals(com.pragmatix.clanserver.messages.request.CashMedalsRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#cashMedals(int, com.pragmatix.clanserver.domain.ClanMember)
 */
@Command(Messages.CASH_MEDALS_RESPONSE)
public class CashMedalsResponse extends CommonResponse<CashMedalsRequest> {

    public CashMedalsResponse() {
    }

    public CashMedalsResponse(CashMedalsRequest request) {
        super(request);
    }

    public CashMedalsResponse(ServiceResult serviceResult, CashMedalsRequest request) {
        super(serviceResult, request, "");
    }
}
