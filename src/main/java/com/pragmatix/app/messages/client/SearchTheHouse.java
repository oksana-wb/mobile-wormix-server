package com.pragmatix.app.messages.client;

import com.pragmatix.Commands;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Команда с клиента на сервер для обыска домика друга
 *
 * User: denis
 * Date: 01.08.2010
 * Time: 20:32:40
 *
 * @see com.pragmatix.app.controllers.SearchTheHouseController#onSearchTheHouse(SearchTheHouse, com.pragmatix.app.model.UserProfile)
 */
@Command(Commands.SearchTheHouse)
public class SearchTheHouse extends SecuredCommand {

    /**
     * ключ текущей сессии
     */
    public String sessionKey;
    
    /**
     * id друга которого обыскиваем
     */
    @Resize(TypeSize.UINT32)
    public long friendId;

    /**
     * Номер ключа 1-10
     * Передаются в обратном порядке
     */
    public byte keyNum;


    public SearchTheHouse() {
    }

    @Override
    public String toString() {
        return "SearchTheHouse{" +
                "sessionKey='" + sessionKey + '\'' +
                ", friendId=" + friendId +
                ", keyNum=" + keyNum +
                ", secureResult=" + secureResult +
                '}';
    }


    @Override
    public String getSessionKey() {
        return sessionKey;
    }
}
