package com.pragmatix.app.messages.client;

import com.pragmatix.app.common.RatingType;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.serialization.annotations.Command;

/**
 * Коменда для взятия рейтинга игрока
 * User: denis
 * Date: 21.04.2010
 * Time: 1:15:36
 *
 * @see com.pragmatix.app.controllers.RatingController#onGetRating(GetRating, com.pragmatix.app.model.UserProfile)
 */
@Command(19)
public class GetRating {

    public RatingType ratingType;

    public BattleWager battleWager;

    public GetRating() {
    }

    public GetRating(RatingType ratingType, BattleWager battleWager) {
        this.ratingType = ratingType;
        this.battleWager = battleWager;
    }

    @Override
    public String toString() {
        return "GetRating{" +
                "ratingType=" + ratingType +
                ", battleWager=" + battleWager +
                '}';
    }
}
