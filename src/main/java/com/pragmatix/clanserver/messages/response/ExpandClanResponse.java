package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.ExpandClanRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 12.04.13 12:46
 */
@Command(Messages.EXPAND_CLAN_RESPONSE)
public class ExpandClanResponse extends CommonResponse<ExpandClanRequest> {
    /**
     * Новый уровень клана
     */
    public int level;

    public ExpandClanResponse() {
    }

    public ExpandClanResponse(ExpandClanRequest request) {
        super(request);
    }

    @Override
    protected StringBuilder propertiesString() {
        return super.propertiesString()
                .append(", level=").append(level);
    }
}
