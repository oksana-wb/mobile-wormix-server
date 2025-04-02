package com.pragmatix.clanserver.messages.event;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.structures.ChatMessageTO;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 16.04.13 9:29
 */
@Command(Messages.CHAT_MESSAGE_EVENT)
public class ChatMessageEvent extends AbstractEvent {
    public ChatMessageTO message;

    @Override
    public int getCommandId() {
        return Messages.CHAT_MESSAGE_EVENT;
    }

    public ChatMessageEvent() {
    }

    public ChatMessageEvent(ChatMessageTO message) {
        this.message = message;
    }

    @Override
    protected StringBuilder propertiesString() {
        return super.propertiesString()
                .append("message=").append(message);
    }
}
