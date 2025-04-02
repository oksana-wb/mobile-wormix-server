package com.pragmatix.chat;

import com.pragmatix.app.common.Locale;
import com.pragmatix.chat.messages.ChatMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 12.03.2018 11:12
 */
public class ChatRoom {

    final Locale locale;

    final List<ChatMessage> chat = new LinkedList<>();

    final AtomicReference<ChatMessage[]> chatHistory = new AtomicReference<>(new ChatMessage[0]);

    final AtomicLong messageCounter = new AtomicLong();

    final Map<Integer, ChatState> inChatProfiles = new ConcurrentHashMap<>();

    public ChatRoom(Locale locale) {
        this.locale = locale;
    }

}
