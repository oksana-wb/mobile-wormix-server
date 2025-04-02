package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * Ответ на команду RemoveFromGroup  
 *
 * @author denis
 *         Date: 03.01.2010
 *         Time: 22:32:22
 */
@Command(10014)
public class RemoveFromGroupResult {

    public SimpleResultEnum result;

    public RemoveFromGroupResult(SimpleResultEnum result) {
        this.result = result;
    }

    public RemoveFromGroupResult() {
    }

    @Override
    public String toString() {
        return "RemoveFromGroupResult{" +
                "result=" + result +
                '}';
    }

}
