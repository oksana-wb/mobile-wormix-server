package com.pragmatix.chat.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.10.2016 17:38
 * @see com.pragmatix.chat.GlobalChatController#onPostToChat(PostToChat, UserProfile)
 */
@Command(10302)
public class ChatMessageEvent {

    public enum ChatMessageEventType implements TypeableEnum {

        SUCCESS(0),
        ILLEGAL_STATE(1),
        NOT_ALLOWED(2),
        TOO_FAST(3),
        RESTRICTED(4),
        ILLEGAL_ACTION(5),
        ;

        public final int type;

        ChatMessageEventType(int type) {
            this.type = type;
        }

        @Override
        public int getType() {
            return type;
        }
    }

    public ChatMessageEventType state = ChatMessageEventType.SUCCESS;

    public ChatMessage chatMessage;

    public ChatMessageEvent() {
    }

    public ChatMessageEvent(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
    }

    public ChatMessageEvent(ChatMessageEventType errorState) {
        this.state = errorState;
    }

    @Override
    public String toString() {
        return "ChatMessageEvent{" +
                (state == ChatMessageEventType.SUCCESS ? "chatMessage=" + chatMessage : "error=" + state) +
                '}';
    }

    public boolean isSuccess(){
        return state == ChatMessageEventType.SUCCESS;
    }

}
