package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.domain.ReviewState;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 07.05.13 13:21
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#listClans(ListClansRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#listClans(ListClansRequest)
 * @see com.pragmatix.clanserver.messages.response.ListClansResponse
 */
@Command(Messages.LIST_CLANS_REQUEST)
public class ListClansRequest extends AbstractRequest {
    /**
     * Строка поиска
     */
    public String searchPhrase;

    public ReviewState[] reviewStates = new ReviewState[0];

    public int offset;

    public int limit;

    @Override
    public int getCommandId() {
        return Messages.LIST_CLANS_REQUEST;
    }

    public ListClansRequest() {
    }

    public ListClansRequest(String searchPhrase) {
        this.searchPhrase = searchPhrase;
    }

    public ListClansRequest(String searchPhrase, ReviewState[] reviewStates, int offset, int limit) {
        this.searchPhrase = searchPhrase;
        this.reviewStates = reviewStates;
        this.offset = offset;
        this.limit = limit;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("searchPhrase=").append(searchPhrase)
                ;
    }
}
