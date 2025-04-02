package com.pragmatix.app.services.rating;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.common.RatingType;
import com.pragmatix.app.messages.server.GetRatingResult;
import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.BanService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.intercom.messages.EndPvpBattleRequest;
import com.pragmatix.pvp.BattleWager;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.List;

public class RatingServiceMobileImpl implements RatingService {

    @Resource
    private OldRatingService ratingService;

    @Resource
    private ProfileService profileService;

    @Resource
    private DailyRatingService dailyRatingService;

    @Resource
    private BanService banService;

    public void init() {
        ratingService.init();
        dailyRatingService.init(BattleWager.values());
    }

    public void persistToDisk() {
        ratingService.persistToDisk();
        dailyRatingService.persistToDisk();
    }

    // начисляем очки рейтинга
    public void onEndPvpBattle(UserProfile profile, EndPvpBattleRequest msg) {
        if(banService.isBanned(profile.getProfileId()))
            return;

        if(msg.result == PvpBattleResult.WINNER || msg.result == PvpBattleResult.NOT_WINNER) {
            ratingService.addRating(profile, msg);

            BattleWager battleWager = msg.wager == BattleWager.WAGER_50_2x2_FRIENDS ? BattleWager.WAGER_50_2x2 : msg.wager;
            updateDailyTops(profile, msg.ratingPoints, battleWager);
        }
    }

    protected void updateDailyTops(UserProfile profile, int incRating, BattleWager battleWager) {
        // обновляем топ по типу ставки
        dailyRatingService.updateDailyTop(profile, incRating, battleWager);
        // обновляем общий ТОП
        dailyRatingService.updateDailyTop(profile, incRating, BattleWager.NO_WAGER);
    }

    @Override
    public void onUpdateRating(UserProfile profile, boolean maybeAdd) {
        // вызовет либо division.checkAndAddUser, либо ratingService.updateRating (если уже в топе)
        ratingService.checkAndAddInRating(profile, maybeAdd);
    }

    @Override
    public void updateDailyRating(UserProfile profile, int incRating, BattleWager battleWager) {
        dailyRatingService.updateDailyTop(profile, incRating, battleWager);
    }

    public void onCloseClanSeason() {
        dailyRatingService.onCloseClanSeason();

        for(Division division : ratingService.getDivisions().values()) {
            for(RatingProfileStructure ratingProfileStructure : division.getTopPlayers().values()) {
                UserProfile profile = profileService.getUserProfile(ratingProfileStructure.id);
                ratingProfileStructure.clanMember = profileService.newClanMemberStructure(profile);
            }
        }
    }

    public void onRename(UserProfile profile) {
        // меняем имя в дневном ТОП-е
        dailyRatingService.onRename(profile);
        // меняем имя в глобальном ТОП-е
        ratingService.onRename(profile);
    }

    public void onBan(Long profileId) {
        dailyRatingService.wipeDailyRatings(profileId);
        ratingService.removeUser(profileId);
    }

    public void onWipe(UserProfile profile) {
        ratingService.wipeRating(profile);
        // обнуляем ежедневный рейтинг
        dailyRatingService.wipeRating(profile);
    }

    public void dailyTask() {
        dailyRatingService.dailyTask();
    }

    public void longRunDailyTask() {
        ratingService.longRunDailyTask();
    }

    public GetRatingResult getTop(RatingType ratingType, BattleWager battleWager, UserProfile profile) {
        if(ratingType == RatingType.Global) {
            List<RatingProfileStructure> profileStructures = ratingService.getRatingList(profile);
            return new GetRatingResult(ratingType, battleWager, profileStructures, profile.getRating(), 0, 0);
        } else {
            return dailyRatingService.getDailySoloRating(ratingType, battleWager, profile);
        }
    }


    @Scheduled(cron = "0 */5 * * * *")
    public void cronTask() {
        dailyRatingService.storeTopPositions();
    }

    public OldRatingService getRatingService() {
        return ratingService;
    }

}
