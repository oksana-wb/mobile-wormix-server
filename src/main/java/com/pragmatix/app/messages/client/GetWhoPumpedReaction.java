package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;

/**
 * Запросить список друзей которые прокачивали реакцию игроку в течении 3-х дней.
 * Сегодня, -2 дня.
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.05.11 15:43
 *
 * @see com.pragmatix.app.controllers.PumpReactionRateController#onGetWhoPumpedReaction(GetWhoPumpedReaction, com.pragmatix.app.model.UserProfile)
 */
@Command(46)
public class GetWhoPumpedReaction {

    public boolean todayOnly = false;

    @Override
    public String toString() {
        return "GetWhoPumpedReaction{" +
                "toodayOnly=" + todayOnly +
                '}';
    }
}
