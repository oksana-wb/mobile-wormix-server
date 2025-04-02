package com.pragmatix.app.services.rating;

import com.pragmatix.app.common.RatingType;
import com.pragmatix.app.messages.server.GetRatingResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.intercom.messages.EndPvpBattleRequest;
import com.pragmatix.pvp.BattleWager;

import java.util.function.Function;

public interface RatingService {

    void init();

    // начисляем очки рейтинга
    void onEndPvpBattle(UserProfile profile, EndPvpBattleRequest msg);

    void onCloseClanSeason();

    void onRename(UserProfile profile);

    void onWipe(UserProfile profile);

    void dailyTask();

    void longRunDailyTask();

    void onBan(Long profileId);

    void persistToDisk();

    GetRatingResult getTop(RatingType ratingType, BattleWager battleWager, UserProfile profile);

    // -- для админки --

    /**
     * Вызывается после внешнего (из админки) изменения рейтинга игрока, чтобы обновить топ (если нужно)
     *
     * @param profile  профиль игрока
     * @param maybeAdd флаг: может ли быть, что от этого изменения ранее не входивший в топ игрок теперь поднимется в него
     */
    void onUpdateRating(UserProfile profile, boolean maybeAdd);

    /**
     * Обновить суточный рейтинг игроку (из админки)
     *
     * @param profile     профиль, которому добавляется суточный рейтинг
     * @param incRating   сколько рейтинга добавляется
     * @param battleWager по какой ставке
     */
    void updateDailyRating(UserProfile profile, int incRating, BattleWager battleWager);

    default Function<BattleWager, BattleWager> wagerAggregator() {
        return battleWager -> battleWager;
    }

}
