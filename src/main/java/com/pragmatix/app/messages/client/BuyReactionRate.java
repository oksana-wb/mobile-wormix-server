package com.pragmatix.app.messages.client;

import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Serialize;

/**
 * команда на покупку скорости реакции
 *
 * @see com.pragmatix.app.controllers.ShopController#onBuyReactionRate(BuyReactionRate, com.pragmatix.app.model.UserProfile)
 */
@Command(49)
public class BuyReactionRate {
    /**
     * след. уровень реакции для покупки http://jira.pragmatix-corp.com/browse/WORMIX-4127
     */
    @Serialize(ifExpr = "com.pragmatix.app.settings.AppParams.IS_NOT_MOBILE()")
    public int level;
    /**
     * сколько "реакции" хочет купить игрок. 3 рекции - 1 рубин.
     * величина должна быть кратна 3-ём
     */
    @Serialize(ifExpr = "com.pragmatix.app.settings.AppParams.IS_MOBILE()")
    public int reactionRateCount;

    public BuyReactionRate() {
    }

    @Override
    public String toString() {
        return "BuyReactionRate{" +
                "level=" + level +
                ", reactionRateCount=" + reactionRateCount +
                '}';
    }
}
