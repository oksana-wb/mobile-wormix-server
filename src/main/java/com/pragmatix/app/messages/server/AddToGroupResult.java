package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * Ответ на команду AddToGroup
 *
 * @author denis
 *         Date: 03.01.2010
 *         Time: 22:28:44
 */
@Command(10013)
public class AddToGroupResult {

    public ShopResultEnum result;

    public AddToGroupResult() {
    }

    public AddToGroupResult(ShopResultEnum result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "AddToGroupResult{" +
                "result=" + result +
                '}';
    }
}
