package com.pragmatix.chat.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.chat.ChatAction;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.10.2016 18:01
 * @see com.pragmatix.chat.GlobalChatController#onPostToChat(PostToChat, UserProfile)
 */
@Command(302)
public class PostToChat {

    public ChatAction action;

    public String name;

    public String message;

    public PostToChat() {
    }

    public PostToChat(ChatAction action, String name, String message) {
        this.action = action;
        this.name = name;
        this.message = message;
    }

    public PostToChat(String name, String message) {
        this.action = ChatAction.PostToChat;
        this.name = name;
        this.message = message;
    }

    @Override
    public String toString() {
        return "PostToChat{" +
                "action=" + action +
                ", name='" + name + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

}
