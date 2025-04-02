package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.09.12 13:14
 */
@Command(10092)
public class RewardCallbackerResult {

    public SimpleResultEnum result;

    public RewardCallbackerResult() {
    }

    public RewardCallbackerResult(SimpleResultEnum result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "RewardCallbackerResult{" +
                "result=" + result +
                '}';
    }

}
