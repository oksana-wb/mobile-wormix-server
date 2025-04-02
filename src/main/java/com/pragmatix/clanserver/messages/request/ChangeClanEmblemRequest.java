package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.common.utils.ArrayUtils;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 08.05.13 14:55
 *
 * @see com.pragmatix.clanserver.controllers.ClanController#changeClanEmblem(ChangeClanEmblemRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#changeClanEmblem(ChangeClanEmblemRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.response.ChangeClanEmblemResponse
 */
@Command(Messages.CHANGE_CLAN_EMBLEM_REQUEST)
public class ChangeClanEmblemRequest extends AbstractRequest{

    public byte[] emblem;

    public boolean fromTreas;

    @Override
    public int getCommandId() {
        return Messages.CHANGE_CLAN_EMBLEM_REQUEST;
    }

    public ChangeClanEmblemRequest() {
    }

    public ChangeClanEmblemRequest(byte[] emblem) {
        this.emblem = emblem;
    }

    public ChangeClanEmblemRequest(byte[] emblem, boolean fromTreas) {
        this.emblem = emblem;
        this.fromTreas = fromTreas;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("emblem=").append(ArrayUtils.toString(emblem))
                .append("fromTreas=").append(fromTreas)
                ;
    }
}
