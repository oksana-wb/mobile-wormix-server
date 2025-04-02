package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.PostNewsRequest;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 18.04.13 15:30
 *
 * @see com.pragmatix.clanserver.services.ChatService#postNews(com.pragmatix.clanserver.messages.request.PostNewsRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.controllers.ChatController#postNews(com.pragmatix.clanserver.messages.request.PostNewsRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.messages.request.PostNewsRequest
 */
@Command(Messages.POST_NEWS_RESPONSE)
public class PostNewsResponse extends CommonResponse<PostNewsRequest> {
    public PostNewsResponse() {
    }

    public PostNewsResponse(PostNewsRequest request) {
        super(request);
    }
}
