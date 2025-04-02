package com.pragmatix.chat.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.10.2016 18:03
 * @see com.pragmatix.chat.GlobalChatController#onJoinToChat(JoinToChat, UserProfile)
 */
@Command(10301)
public class JoinToChatResult {

    public ChatMessage[] chatHistory;

    public JoinToChatResult() {
    }

    public JoinToChatResult(ChatMessage[] chatHistory) {
        this.chatHistory = chatHistory;
    }

    @Override
    public String toString() {
        return "JoinToChatResult{" +
                "chatHistory=" + Arrays.toString(chatHistory) +
                '}';
    }
}
