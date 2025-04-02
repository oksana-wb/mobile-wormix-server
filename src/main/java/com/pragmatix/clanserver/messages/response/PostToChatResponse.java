package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.PostToChatRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 15.04.13 12:28
 *
 * @see com.pragmatix.clanserver.services.ChatService#postToChat(com.pragmatix.clanserver.messages.request.PostToChatRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.controllers.ChatController#postToChat(com.pragmatix.clanserver.messages.request.PostToChatRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.request.PostToChatRequest
 */
@Command(Messages.POST_TO_CHAT_RESPONSE)
public class PostToChatResponse extends CommonResponse<PostToChatRequest> {
    public PostToChatResponse() {
    }

    public PostToChatResponse(PostToChatRequest request) {
        super(request);
    }
}
