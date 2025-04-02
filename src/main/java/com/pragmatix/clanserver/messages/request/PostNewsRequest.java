package com.pragmatix.clanserver.messages.request;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 18.04.13 15:30
 *
 * @see com.pragmatix.clanserver.services.ChatService#postNews(com.pragmatix.clanserver.messages.request.PostNewsRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.controllers.ChatController#postNews(PostNewsRequest, UserProfile)
 * @see com.pragmatix.clanserver.messages.response.PostNewsResponse
 */
@Command(Messages.POST_NEWS_REQUEST)
public class PostNewsRequest extends AbstractRequest {
    /**
     * Текстовое сообщение
     */
    public String text;

    @Override
    public int getCommandId() {
        return Messages.POST_NEWS_REQUEST;
    }

    public PostNewsRequest() {
    }

    public PostNewsRequest(String text) {
        this.text = text;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("text=").append(text)
                ;
    }
}
