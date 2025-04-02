package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.ListClansRequest;
import com.pragmatix.clanserver.messages.structures.ClanTO;
import com.pragmatix.common.utils.ArrayUtils;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 07.05.13 14:35
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#listClans(ListClansRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#listClans(ListClansRequest)
 * @see com.pragmatix.clanserver.messages.request.ListClansRequest
 */
@Command(Messages.LIST_CLANS_RESPONSE)
public class ListClansResponse extends CommonResponse<ListClansRequest> {
    /**
     * Список кланов
     */
    public ClanTO[] clans = ClanTO.EMPTY_ARRAY;

    public ListClansResponse() {
    }

    public ListClansResponse(ListClansRequest request) {
        super(request);
    }

    @Override
    protected StringBuilder propertiesString() {
        return super.propertiesString()
                .append("clans=").append(ArrayUtils.toString(clans))
                ;
    }
}
