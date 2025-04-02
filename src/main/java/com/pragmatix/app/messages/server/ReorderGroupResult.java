package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * ответ на команду изменения очередности ходов членов команды
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 16.08.12 10:55
 *
 *  @see com.pragmatix.app.messages.client.ReorderGroup
 */
@Command(1022)
public class ReorderGroupResult {

    public SimpleResultEnum result;

    public ReorderGroupResult() {
    }

    public ReorderGroupResult(SimpleResultEnum result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "ReorderGroupResult{" +
                "result=" + result +
                '}';
    }

}
