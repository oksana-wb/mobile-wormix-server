package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.InviteToClanRequest;
import com.pragmatix.clanserver.messages.structures.InviteTO;
import com.pragmatix.common.utils.ArrayUtils;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 09.04.13 9:50
 */
@Command(Messages.INVITE_TO_CLAN_RESPONSE)
public class InviteToClanResponse extends CommonResponse<InviteToClanRequest> {
    /**
     * Код подтверждения сервера Вормикса
     */
    public int interopCode;

    public InviteTO[] invites = InviteTO.EMPTY_ARR;

    public InviteToClanResponse() {
    }

    public InviteToClanResponse(InviteToClanRequest request) {
        super(request);
    }

    @Override
    protected StringBuilder propertiesString() {
        StringBuilder sb = super.propertiesString();
        sb.append(", interopCode=").append(interopCode);
        ArrayUtils.append(sb.append(", invites="), invites);
        return sb;
    }
}
