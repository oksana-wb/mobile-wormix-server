package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.06.11 18:38
 */
@Command(10051)
public class PrivateChatMessage {

    @Resize(TypeSize.UINT32)
    public long fromProfileId;

    public String msg;
    /**
     * код предопределенного сообщения
     */
    public byte predefinedMsgType;

    public enum PredefinedMessages implements TypeableEnum{
        /**
           игрока вызывают на бой
         */
        BattleRequest(1);

        int type;

        PredefinedMessages(int type) {
            this.type = type;
        }

        @Override
        public int getType() {
            return type;
        }
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "fromProfileId=" + fromProfileId +
                ", msg='" + msg + '\'' +
                ", predefinedMsgType='" + predefinedMsgType + '\'' +
                '}';
    }

    public PrivateChatMessage() {
    }

    public PrivateChatMessage(long fromProfileId, PredefinedMessages predefinedMsg) {
        this.fromProfileId = fromProfileId;
        this.predefinedMsgType = (byte) predefinedMsg.getType();
        this.msg="";
    }
}
