package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.structures.ClanTO;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 26.04.13 9:22
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#getClanSummary(com.pragmatix.clanserver.messages.request.ClanSummaryRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanService#getClanSummary(ClanSummaryRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.response.ClanSummaryResponse
 * @see com.pragmatix.clanserver.messages.structures.ClanTO
 */
@Command(Messages.CLAN_SUMMARY_REQUEST)
public class ClanSummaryRequest extends AbstractRequest {
    /**
     * Идентификатор клана
     */
    public int clanId;

    /**
     * Количество данных
     */
    public int scope = ClanTO.SCOPE_HEADER;

    public ClanSummaryRequest() {
    }

    public ClanSummaryRequest(int clanId) {
        this.clanId = clanId;
    }

    public ClanSummaryRequest(int clanId, int scope) {
        this.clanId = clanId;
        this.scope = scope;
    }

    @Override
    public int getCommandId() {
        return Messages.CLAN_SUMMARY_REQUEST;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("clanId=").append(clanId)
                .append(", scope=").append(Integer.toHexString(scope))
                ;
    }
}
