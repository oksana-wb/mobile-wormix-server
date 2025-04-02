package com.pragmatix.achieve.messages.client;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;

/**
 * Получить достижения по id игрока
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.07.11 15:55
 * @see com.pragmatix.achieve.controllers.AchieveController#onGetAchievements(GetAchievements, UserProfile)
 */
@Command(3002)
public class GetAchievements extends SecuredCommand {

    public String profileId;

    public GetAchievements() {
    }

    public GetAchievements(String profileId) {
        this.profileId = profileId;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "GetAchievements{" +
                "profileId='" + profileId + '\'' +
                '}';
    }
}
