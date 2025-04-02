package com.pragmatix.app.services.rating;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.common.RatingType;
import com.pragmatix.app.messages.server.GetRatingResult;
import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.app.messages.structures.SimpleProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.clanserver.messages.request.UpdateRatingRequest;
import com.pragmatix.clanserver.services.ClanService;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.intercom.messages.EndPvpBattleRequest;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.server.Server;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.pragmatix.pvp.BattleWager.WAGER_15_DUEL;
import static com.pragmatix.pvp.BattleWager.WAGER_50_2x2;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.05.2016 14:17
 */
public class RatingServiceImpl implements RatingService {

    public static final int PROGRESS_DEEP = 6;
    public static final int PROGRESS_MAX = 300;
    public static final int MAX_TOP = 100;

    @Resource
    private RankService rankService;

    @Resource
    private RatingDAO ratingDAO;

    @Resource
    private ProfileService profileService;

    @Resource
    private ClanService clanService;

    @Resource
    private DailyRatingService dailyRatingService;

    @Resource
    private PersistenceService persistenceService;

    private Division rubyDivision;

    private Division seasonRubyDivision;

    private final ProgressData seasonProgressData = new ProgressData(PROGRESS_DEEP, "RatingService.seasonProgress");

    private Function<BattleWager, BattleWager> wagerAggregator;

    private boolean initialized = false;

    @Override
    public void init() {
        rubyDivision = new Division(0, MAX_TOP);
        loadTopProfiles(rubyDivision);

        seasonProgressData.restoreFromDisk(persistenceService);
        seasonRubyDivision = new Division(0, PROGRESS_MAX, new UserProfileByRankAndRatingPointsComparator(), (profile, lastProfile) -> {
            byte profileRank = rankService.getPlayerRealRank(profile);
            return profileRank < lastProfile.rank || (profileRank == lastProfile.rank && profile.getRankPoints() > lastProfile.ratingPoints);
        });
        loadSeasonTopProfiles(seasonRubyDivision);

        /*
          делаем два дневных топа:
            1) Дуэльный топ (объединяет ставки на 15 и 300)
            2) Топ 2х2 (объединяет 2х2 с другом и со случайным союзником)
         */
        wagerAggregator = battleWager -> {
            switch (battleWager) {
                case WAGER_15_DUEL:
                case WAGER_20_DUEL:
                case WAGER_300_DUEL:
                    return WAGER_15_DUEL;
                case WAGER_50_2x2:
                case WAGER_50_2x2_FRIENDS:
                    return WAGER_50_2x2;
                default:
                    return null;
            }
        };
        dailyRatingService.init(new BattleWager[]{WAGER_15_DUEL, BattleWager.WAGER_50_2x2});

        initialized = true;
    }

    public void cleanSeasonAndDailyTop() {
        rubyDivision = new Division(0, MAX_TOP);
        seasonProgressData.clean();
        seasonProgressData.persistToDisk(persistenceService);

        dailyRatingService.wipeDailyTop();
        dailyRatingService.persistToDisk();
    }

    @Override
    public void persistToDisk() {
        dailyRatingService.persistToDisk();
        seasonProgressData.persistToDisk(persistenceService);
    }

    @Override
    public void onEndPvpBattle(UserProfile profile, EndPvpBattleRequest msg) {
        if(msg.result == PvpBattleResult.WINNER || msg.result == PvpBattleResult.NOT_WINNER || msg.result == PvpBattleResult.DRAW_GAME) {
            addRating(profile, msg.ratingPoints);
            addSeasonRating(profile, msg.rankPoints);

            dailyRatingService.updateDailyTop(profile, msg.ratingPoints, wagerAggregator.apply(msg.wager));
        }
    }

    public void addRating(UserProfile profile, int ratingPoints) {
        addRatingFor(profile, ratingPoints);
        boolean maybeAdd = ratingPoints > 0;
        rubyDivision.onChangeRating(profile, maybeAdd, profileService.clanMember_rank_skin(profile));

        seasonRubyDivision.consume(profile, ((ratingProfileStructure, userProfile) -> ratingProfileStructure.init(profile, profileService.clanMember_rank_skin(profile))));
    }

    public void addSeasonRating(UserProfile profile, int rankPoints) {
        int newRankPoints = profile.getRankPoints() + rankPoints;
        profile.setRankPoints(newRankPoints);
        rankService.onSetRankPoints(profile);
        boolean maybeAdd = rankPoints > 0;
        seasonRubyDivision.onChangeRating(profile, maybeAdd, profileService.clanMember_rank_skin(profile));
        rubyDivision.consume(profile, ((ratingProfileStructure, userProfile) -> ratingProfileStructure.rank = rankService.getPlayerRealRank(userProfile)));
        dailyRatingService.onSetSeasonRating(profile, (userProfile) -> rankService.getPlayerRealRank(userProfile));
    }

    public void addRatingFor(UserProfile profile, int ratingPoints) {
        int newRating = Math.max(0, profile.getRating() + ratingPoints);
        profile.setRating(newRating);
        short socialId = profileService.getSocialIdForClan(profile);
        clanService.updateRating(new UpdateRatingRequest(socialId, profile.getId().intValue(), newRating, ratingPoints));
    }

    @Override
    public void onCloseClanSeason() {
        dailyRatingService.onCloseClanSeason();

        for(RatingProfileStructure ratingProfileStructure : rubyDivision.getTopPlayers().values()) {
            UserProfile profile = profileService.getUserProfile(ratingProfileStructure.id);
            ratingProfileStructure.clanMember = profileService.newClanMemberStructure(profile);
        }
        for(RatingProfileStructure ratingProfileStructure : seasonRubyDivision.getTopPlayers().values()) {
            UserProfile profile = profileService.getUserProfile(ratingProfileStructure.id);
            ratingProfileStructure.clanMember = profileService.newClanMemberStructure(profile);
        }
    }

    @Override
    public void onRename(UserProfile profile) {
        // меняем имя в дневном ТОП-е
        dailyRatingService.onRename(profile);
        // меняем имя в глобальном ТОП-е
        rubyDivision.consume(profile, ((ratingProfileStructure, userProfile) -> ratingProfileStructure.name = userProfile.getName()));
        seasonRubyDivision.consume(profile, ((ratingProfileStructure, userProfile) -> ratingProfileStructure.name = userProfile.getName()));
    }

    @Override
    public void onUpdateRating(UserProfile profile, boolean maybeAdd) {
        rubyDivision.onChangeRating(profile, maybeAdd, profileService.clanMember_rank_skin(profile));
        rankService.onSetRankPoints(profile);
        seasonRubyDivision.onChangeRating(profile, maybeAdd, profileService.clanMember_rank_skin(profile));
    }

    @Override
    public void updateDailyRating(UserProfile profile, int incRating, BattleWager battleWager) {
        dailyRatingService.updateDailyTop(profile, incRating, battleWager);
    }

    @Override
    public void onWipe(UserProfile profile) {
        profile.setRating(0);
        profile.setRankPoints(0);
        profile.setBestRank(RankService.INIT_RANK_VALUE);

        // обнуляем ежедневный рейтинг
        dailyRatingService.wipeRating(profile);
        // обнуляем глобальный рейтинг
        rubyDivision.removeUser(profile);
        // обнуляем сезонный рейтинг
        seasonRubyDivision.removeUser(profile);
        // обнуляем клановый рейтинг
        short socialId = profileService.getSocialIdForClan(profile);
        UpdateRatingRequest request = new UpdateRatingRequest(socialId, profile.getId().intValue(), 0, 0);
        request.wipeRating = true;
        clanService.updateRating(request);
    }

    @Override
    public void dailyTask() {
        dailyRatingService.dailyTask();
    }

    @Override
    public void longRunDailyTask() {
    }

    @Override
    public void onBan(Long profileId) {
        dailyRatingService.wipeDailyRatings(profileId);
        rubyDivision.removeUser(profileId);
        seasonRubyDivision.removeUser(profileId);
    }

    @Override
    public GetRatingResult getTop(RatingType ratingType, BattleWager battleWager, UserProfile profile) {
        if(ratingType == RatingType.Global) {
            List<RatingProfileStructure> profileStructures = rubyDivision.getRatingList();
            return new GetRatingResult(ratingType, battleWager, profileStructures, profile.getRating(), 0, 0);
        } else if(ratingType == RatingType.Seasonal) {
            List<RatingProfileStructure> ratingList = seasonRubyDivision.getRatingList();
            List<RatingProfileStructure> profileStructures = ratingList.size() <= MAX_TOP ? ratingList : ratingList.subList(0, MAX_TOP);
            int oldPlace = seasonProgressData.getOldPlace(profile.getId());
            int currentPlace = seasonProgressData.getCurrentPlace(profile.getId());
            return new GetRatingResult(ratingType, battleWager, profileStructures, profile.getRankPoints(), oldPlace, currentPlace);
        } else {
            return dailyRatingService.getDailySoloRating(ratingType, battleWager, profile);
        }
    }

    @Scheduled(cron = "0 0 */4 * * *")
    public void updateSeasonProgress() {
        if(!initialized)
            return;

        seasonProgressData.updateProgress(seasonRubyDivision.getRatingList().stream().map(SimpleProfileStructure::getProfileId).collect(Collectors.toList()));

        for(RatingProfileStructure ratingProfileStructure : seasonRubyDivision.getTopPlayers().values()) {
            ratingProfileStructure.oldPlace = seasonProgressData.getOldPlace(ratingProfileStructure.getProfileId());
        }
    }

    public void loadTopProfiles(Division division) {
        long start = System.currentTimeMillis();
        Server.sysLog.info("Select top profiles ...");
        List<RatingProfileStructure> topPlayers = ratingDAO.getTopPlayers(division.maxTop);
        // подгружаем профили топеров
        profileService.loadProfiles(topPlayers.stream().map(p -> p.id).collect(Collectors.toList()), false);
        //заполняем строковый ID и клановую информацию
        for(RatingProfileStructure topPlayer : topPlayers) {
            topPlayer.profileStringId = profileService.getProfileStringId(topPlayer.id);
            topPlayer.clanMember = profileService.newClanMemberStructure(topPlayer.id);
            topPlayer.rank = rankService.getPlayerRealRank(profileService.getUserProfile(topPlayer.getProfileId()));
        }
        Server.sysLog.info("Select done in = " + (double) (System.currentTimeMillis() - start) / (double) 1000 + " sec.");
        division.init(topPlayers);
    }

    public void loadSeasonTopProfiles(Division division) {
        long start = System.currentTimeMillis();
        Server.sysLog.info("Select season top profiles ...");
        List<Long> topIds = ratingDAO.getSeasonTopPlayers(division.maxTop);
        // подгружаем профили топеров
        profileService.loadProfiles(topIds, false);
        //заполняем строковый ID и клановую информацию
        Collection<RatingProfileStructure> topPlayers = topIds.stream().map(profileId -> {
            UserProfile profile = profileService.getUserProfile(profileId);
            RatingProfileStructure result = new RatingProfileStructure(profile, profileService.clanMember_rank_skin(profile), profile.getRankPoints(), seasonProgressData.getOldPlace(profile.getProfileId()));
            result.profileStringId = profileService.getProfileStringId(profileId);
            return result;
        }).collect(Collectors.toList());
        Server.sysLog.info("Select done in = " + (double) (System.currentTimeMillis() - start) / (double) 1000 + " sec.");
        division.init(topPlayers);
    }

    @Override
    public Function<BattleWager, BattleWager> wagerAggregator() {
        return wagerAggregator;
    }

    //====================== Getters and Setters =================================================================================================================================================

    public Division getRubyDivision() {
        return rubyDivision;
    }

    public Division getSeasonRubyDivision() {
        return seasonRubyDivision;
    }
}
