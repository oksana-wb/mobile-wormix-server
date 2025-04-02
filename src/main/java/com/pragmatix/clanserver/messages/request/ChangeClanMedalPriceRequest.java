package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.clanserver.controllers.ClanController#changeClanMedalPrice(ChangeClanMedalPriceRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#changeClanMedalPrice(byte, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.response.ChangeClanMedalPriceResponse
 */
@Command(Messages.CHANGE_CLAN_MEDAL_PRICE_REQUEST)
public class ChangeClanMedalPriceRequest extends AbstractRequest {

    public byte medalPrice;

    @Override
    public int getCommandId() {
        return Messages.CHANGE_CLAN_MEDAL_PRICE_REQUEST;
    }

    public ChangeClanMedalPriceRequest() {
    }

    public ChangeClanMedalPriceRequest( byte medalPrice) {
        this.medalPrice = medalPrice;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("medalPrice=").append(medalPrice)
                ;
    }
}
