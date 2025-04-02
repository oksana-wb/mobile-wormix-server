package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.09.12 13:14
 */
@Command(10090)
public class CallbackFriendResult {

    public SimpleResultEnum result;

    public CallbackFriendResult() {
    }

    public CallbackFriendResult(SimpleResultEnum result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "CallBackFriendResult{" +
                "result=" + result +
                '}';
    }

}
