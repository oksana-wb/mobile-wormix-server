package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.QuitClanRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 16.04.13 10:25
 *
 * @see com.pragmatix.clanserver.messages.request.QuitClanRequest
 */
@Command(Messages.QUIT_FROM_CLAN_RESPONSE)
public class QuitClanResponse extends CommonResponse<QuitClanRequest> {
    public QuitClanResponse() {
    }

    public QuitClanResponse(QuitClanRequest request) {
        super(request);
    }
}
