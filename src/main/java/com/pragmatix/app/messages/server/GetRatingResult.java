package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.RatingType;
import com.pragmatix.app.messages.client.GetRating;
import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;
import java.util.List;

/**
 * Возвращает список анкет игроков из рейтинга
 * User: denis
 * Date: 21.04.2010
 * Time: 1:19:58
 */
@Command(10019)
public class GetRatingResult {

   /**
     * ТОП 50 игроков
     */
    public List<RatingProfileStructure> profileStructures;

    /**
     * собственный рейтинг игрока
     */
    public int rating;

    /**
     * позиция в ТОП 1000 30 мин. назад
     * равна 0 если не попал в ТОП 1000
     */
    public int oldPlace;

    /**
     * текущий рейтинг: позиция в ТОП 1000 5 мин. назад (использовать если игрок не вошел в ТОП 50)
     * вчерашний рейтинг: позиция в ТОП 1000 за вчера
     * равна 0 если не попал в ТОП 1000
     *
     * если игорок входит в ТОП 50 позицию брать из profileStructures
     */
    public int place;

    public RatingType ratingType;

    public BattleWager battleWager;

    public GetRatingResult() {
    }

    public GetRatingResult(RatingType ratingType, BattleWager battleWager, List<RatingProfileStructure> ratingProfileStructures) {
        this.ratingType = ratingType;
        this.battleWager = battleWager;
        this.profileStructures = ratingProfileStructures;
    }

    public GetRatingResult(RatingType ratingType, BattleWager battleWager, List<RatingProfileStructure> profileStructures, int rating, int oldPlace, int place) {
        this(ratingType, battleWager, profileStructures);
        this.rating = rating;
        this.oldPlace = oldPlace;
        this.place = place;
    }

    @Override
    public String toString() {
        return "GetRatingResult{" +
                "ratingType=" + ratingType +
                ", battleWager=" + battleWager +
                ", rating=" + rating +
                ", oldPlace=" + oldPlace +
                ", place=" + place +
                ", profileStructures=" + profileStructures +
                '}';
    }
}
