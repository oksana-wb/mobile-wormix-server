package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 13.07.11 18:42
 */
@Command(value = 10040)
public class SendWipeConfirmCodeResult {

    public enum Result implements TypeableEnum {

        OK(0),
        DAILY_LIMIT_EXCEEDED(1),
        TODAY_ALREADY_WIPED(2),
        SEND_MESSAGE_ERROR(3),
        ERROR(3),
        ;

        int type;

        Result(int type) {
            this.type = type;
        }

        @Override
        public int getType() {
            return type;
        }

        public static Result valueOf(int type) {
            for(Result result : Result.values()) {
                if(result.getType() == type) {
                    return result;
                }
            }
            return null;
        }
    }

    public byte result;

    public SendWipeConfirmCodeResult() {
    }

    public SendWipeConfirmCodeResult(Result typeEnum) {
        this.result = (byte) typeEnum.getType();
    }

    @Override
    public String toString() {
        return "SendWipeConfirmCodeResponse{" +
                "result=" + Result.valueOf(result) +
                '}';
    }
}
