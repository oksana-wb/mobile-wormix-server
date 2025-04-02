package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.ServiceResult;
import com.pragmatix.clanserver.messages.request.ChangeClanMedalPriceRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#changeClanMedalPrice(byte, com.pragmatix.clanserver.domain.ClanMember)
 */
@Command(Messages.CHANGE_CLAN_MEDAL_PRICE_RESPONSE)
public class ChangeClanMedalPriceResponse extends CommonResponse<ChangeClanMedalPriceRequest> {

    public ChangeClanMedalPriceResponse() {
    }

    public ChangeClanMedalPriceResponse(ServiceResult serviceResult, ChangeClanMedalPriceRequest request) {
        super(serviceResult, request, "");
    }

}
