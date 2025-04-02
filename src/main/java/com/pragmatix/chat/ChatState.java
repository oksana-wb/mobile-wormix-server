package com.pragmatix.chat;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 23.03.2018 12:53
 */
public class ChatState {

    public final long joinTime = System.currentTimeMillis();

    public long lastMessageTime;

    ChatState updateLastMessageTime() {
        lastMessageTime = System.currentTimeMillis();
        return this;
    }
}
