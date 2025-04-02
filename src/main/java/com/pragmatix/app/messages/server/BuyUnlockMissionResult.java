package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 11.03.12 16:14
 */
@Command(10042)
public class BuyUnlockMissionResult {

    public ShopResultEnum result;

    public BuyUnlockMissionResult() {
    }

    public BuyUnlockMissionResult(ShopResultEnum result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "BuyUnlockMissionResult{" +
                "result=" + result +
                '}';
    }

}
