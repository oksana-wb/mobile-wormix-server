package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.structures.SelectStuffResultStructure;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * Ответ от сервера на команду выбора предмета для одевания на себя
 *
 * User: denis
 * Date: 04.10.2010
 * Time: 23:18:48
 */
@Command(10022)
public class SelectStuffResults implements SecuredResponse {

    public SelectStuffResultStructure[] selectStuffResults;

    public SelectStuffResults() {
    }

    public SelectStuffResults(SelectStuffResultStructure[] selectStuffResults) {
        this.selectStuffResults = selectStuffResults;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "SelectStuffResults{" +
                Arrays.toString(selectStuffResults) +
                '}';
    }
}
