package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 14.04.2016 12:56
 *         <p>
 * Ответ на команду сброса имени
 * @see com.pragmatix.app.messages.client.ClearName
 * @see com.pragmatix.app.controllers.ProfileController#onClearName(com.pragmatix.app.messages.client.ClearName, com.pragmatix.app.model.UserProfile)
 */
@Command(10131)
public class ClearNameResult {

    public SimpleResultEnum result;

    public ClearNameResult() {}

    public ClearNameResult(SimpleResultEnum result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "ClearNameResult{" +
                "result=" + result +
                '}';
    }
}
