package com.pragmatix.clanserver.messages.request;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 15.04.13 12:25
 *
 * @see com.pragmatix.clanserver.services.ChatService#postToChat(com.pragmatix.clanserver.messages.request.PostToChatRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.controllers.ChatController#postToChat(PostToChatRequest, UserProfile)
 */
@Command(Messages.POST_TO_CHAT_REQUEST)
public class PostToChatRequest extends AbstractRequest {
    /**
     * Текстовое сообщение
     */
    public String text;

    @Override
    public int getCommandId() {
        return Messages.POST_TO_CHAT_REQUEST;
    }

    public PostToChatRequest() {
    }

    public PostToChatRequest(String text) {
        this.text = text;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("text=").append(text)
                ;
    }
}
