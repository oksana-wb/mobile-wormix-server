package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.ClanSummaryRequest;
import com.pragmatix.clanserver.messages.structures.ClanTO;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 26.04.13 9:25
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#getClanSummary(com.pragmatix.clanserver.messages.request.ClanSummaryRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#getClanSummary(com.pragmatix.clanserver.messages.request.ClanSummaryRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.request.ClanSummaryRequest
 */
@Command(Messages.CLAN_SUMMARY_RESPONSE)
public class ClanSummaryResponse extends CommonResponse<ClanSummaryRequest> {
    /**
     * Информация о клане
     */
    public ClanTO clan;

    public ClanSummaryResponse() {
    }

    public ClanSummaryResponse(ClanSummaryRequest request) {
        super(request);
    }

    @Override
    protected StringBuilder propertiesString() {
        return super.propertiesString()
                .append(", clan=").append(clan)
                ;
    }
}
