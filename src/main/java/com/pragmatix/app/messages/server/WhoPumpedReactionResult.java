package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.structures.ProfileDoubleKeyStructure;
import com.pragmatix.serialization.annotations.Command;

/**
 * Вернет массивы игроков которые прокачивали реакцию игроку сегодня, -2 дня
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.05.11 16:36
 * @see com.pragmatix.app.controllers.PumpReactionRateController#onGetWhoPumpedReaction(com.pragmatix.app.messages.client.GetWhoPumpedReaction, com.pragmatix.app.model.UserProfile)
 */
@Command(10034)
public class WhoPumpedReactionResult {

    public ProfileDoubleKeyStructure[] todayPumped;

    public ProfileDoubleKeyStructure[] yesterdayPumped;

    public ProfileDoubleKeyStructure[] twoDaysAgoPumped;

    public WhoPumpedReactionResult() {
    }

    public WhoPumpedReactionResult(ProfileDoubleKeyStructure[] todayPumped, ProfileDoubleKeyStructure[] yesterdayPumped, ProfileDoubleKeyStructure[] twoDaysAgoPumped) {
        this.todayPumped = todayPumped;
        this.yesterdayPumped = yesterdayPumped;
        this.twoDaysAgoPumped = twoDaysAgoPumped;
    }

    @Override
    public String toString() {
        return "WhoPumpedReactionResult{" +
                "todayPumped=" + (todayPumped == null ? 0 : todayPumped.length) +
                ", yesterdayPumped=" + (yesterdayPumped == null ? 0 : yesterdayPumped.length) +
                ", twoDaysAgoPumped=" + (twoDaysAgoPumped == null ? 0 : twoDaysAgoPumped.length) +
                '}';
    }
}
