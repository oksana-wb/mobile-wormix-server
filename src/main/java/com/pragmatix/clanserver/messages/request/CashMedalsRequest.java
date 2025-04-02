package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.clanserver.controllers.ClanController#cashMedals(CashMedalsRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#cashMedals(int, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.response.CashMedalsResponse
 */
@Command(Messages.CASH_MEDALS_REQUEST)
public class CashMedalsRequest extends AbstractRequest{

    public int medals;

    @Override
    public int getCommandId() {
        return Messages.CASH_MEDALS_REQUEST;
    }

    public CashMedalsRequest() {
    }

    public CashMedalsRequest(int medals) {
        this.medals = medals;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("medals=").append(medals)
                ;
    }
}
