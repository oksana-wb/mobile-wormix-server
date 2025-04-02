package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.app.controllers.ProfileController#onGetDailyBonus(com.pragmatix.app.messages.client.GetDailyBonus, com.pragmatix.app.model.UserProfile)
 */
@Command(107)
public class GetDailyBonus {

    @Override
    public String toString() {
        return "GetDailyBonus{}";
    }

}
