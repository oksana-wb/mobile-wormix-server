package com.pragmatix.app.messages.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;

/**
 * Команда запроса у сервера списка профайлов
 * В данный момент используется для загрузки профилей членов клана
 *
 * @see com.pragmatix.app.controllers.ProfileController#onGetProfiles(GetProfiles, com.pragmatix.app.model.UserProfile)
 */
@Command(5)
public class GetProfiles extends SecuredCommand {

    public static final int MAX_PROFILES = 50;

    /**
     * ключ текущей сессии
     */
    public String sessionKey;

    /**
     * id профайлов которые нобходимо прислать
     */
    public String[] ids;

    public GetProfiles() {
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "GetProfiles{" +
                "ids=" + Arrays.toString(ids) +
                ", sessionKey='" + sessionKey + '\'' +
                ", secureResult=" + secureResult +
                '}';
    }
}
