package com.pragmatix.app.services.rating;

import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.persist.DivisionsKeeper;
import com.pragmatix.clan.structures.ClanMemberStructure;
import com.pragmatix.clanserver.messages.request.UpdateRatingRequest;
import com.pragmatix.clanserver.services.ClanService;
import com.pragmatix.clanserver.services.RatingServiceImpl;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.intercom.messages.EndPvpBattleRequest;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.serialization.BinarySerializer;
import com.pragmatix.server.Server;
import io.vavr.Tuple;
import io.vavr.Tuple3;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class OldRatingService {

    @Resource
    private ProfileService profileService;

    /**
     * Дивизионы
     */
    private Map<Integer, Division> divisions;

    public static int MAX_TOP = 50;

    private List<League> leagues;
    /**
     * множитель больше единицы - с каким запасом выбирать игроков. Влияет на наполняемость лиг
     */
    private double overheadFactor = 1.8;
    /**
     * рейтинг игрока после поражения не упадет ниже порога лиги новичков
     */
    private int minRatingInNewcomersLeague = 1;
    /**
     * учитывать в топе только тех игроков, которые заходили хотя бы lastLoginDays дней назад
     */
    private int lastLoginDays = 365;

    @Resource
    private PersistenceService persistenceService;

    @Resource
    private ClanService clanService;

    @Resource
    private RatingServiceImpl clanRatingService;

    @Resource
    private BinarySerializer binarySerializer;

    @Resource
    private RatingDAO ratingDAO;

    public static final String divisionsKeepFileName = "RatingService.divisions";

    private boolean initialized = false;

    private final Callable<Tuple3<ClanMemberStructure, Byte, Byte>> clanMember_rank_skin = () -> Tuple.of((ClanMemberStructure) null, (byte) 0, (byte) 0);

    public void init() {
        //инициализируем рейтинг игроков
        Map<Integer, Division> divisions = persistenceService.restoreObjectFromFile(Map.class, divisionsKeepFileName, new DivisionsKeeper(() -> createDivisions(), binarySerializer));
        if(divisions != null) {
            Server.sysLog.info("Rating service reinit ...");
            for(Division division : divisions.values()) {
                division.reinit();
            }
            this.divisions = divisions;
        } else {
            loadTopProfiles();
        }

        initialized = true;
    }

    public void loadTopProfiles() {
        Map<Integer, Division> divisions = createDivisions();

        for(League league : leagues) {
            long start = System.currentTimeMillis();
            Server.sysLog.info("Select top profiles in {} ...", league);
            List<RatingProfileStructure> topPlayers = ratingDAO.getTopPlayers(league, getOverheadFactor(), lastLoginDays, MAX_TOP);
            //заполняем строковый ID и кланову информацию
            //todo загрузить в кеш членов клана списком
            for(RatingProfileStructure topPlayer : topPlayers) {
                topPlayer.profileStringId = profileService.getProfileStringId(topPlayer.id);
                topPlayer.clanMember = profileService.newClanMemberStructure(topPlayer.id);
            }

            Server.sysLog.info("Select done in = " + (double) (System.currentTimeMillis() - start) / (double) 1000 + " sec.");
            Server.sysLog.info("Distribute by divisions ...");
            distributeByDivisions(topPlayers, divisions);
            Server.sysLog.info("Distribute done.");
        }

        Server.sysLog.info("Rating service reinit ...");
        for(Division division : divisions.values()) {
            division.reinit();
        }

        List<Division> values = new ArrayList<>(divisions.values());
        Collections.sort(values, (o1, o2) -> o1.getDivision() - o2.getDivision());
        for(Division division : values) {
            if(division.getTopPlayers().size() < MAX_TOP) {
                NavigableSet<RatingProfileStructure> profileNavigableSet = division.getProfileNavigableSet();
                int minRating = profileNavigableSet.size() > 0 ? profileNavigableSet.first().rating : 0;
                int maxRating = profileNavigableSet.size() > 0 ? profileNavigableSet.last().rating : 0;
                Server.sysLog.warn(String.format("Incomplete top in Division: %s  (%s) %s - %s",
                        division.getDivision(), division.getTopPlayers().size(),
                        minRating, maxRating));
            }
        }
        this.divisions = divisions;
    }

    public void longRunDailyTask() {
        if(initialized) {
            loadTopProfiles();
        }
    }

    public void persistToDisk() {
        clanRatingService.persistToDisk();
        persistenceService.persistObjectToFile(divisions, divisionsKeepFileName, new DivisionsKeeper(null, binarySerializer));
    }

    public Map<Integer, Division> createDivisions() {
        Map<Integer, Division> divisions = new ConcurrentHashMap<>();
        // перебираем лиги
        for(int leagueIndex = 0; leagueIndex < leagues.size(); leagueIndex++) {
            League league = leagues.get(leagueIndex);
            league.setIndex(leagueIndex);
            if(leagueIndex == 0)
                minRatingInNewcomersLeague = league.getMin();
            for(int i = 0; i < league.getDivisionCount(); i++) {
                // создаем дивизионы
                int key = getDivisionKey(league, i);
                divisions.put(key, new Division(key, MAX_TOP));
            }
        }
        return divisions;
    }

    /**
     * Разносим профайлы по дивизионам
     *
     * @param topProfiles топ профайлов
     * @param divisions
     */
    public void distributeByDivisions(List<RatingProfileStructure> topProfiles, Map<Integer, Division> divisions) {
        for(RatingProfileStructure profile : topProfiles) {
            Division division = divisions.get(divisionByRating(profile.rating, profile.id));
            division.checkAndAddUser(profile);
        }
    }

    /**
     * проверяет присутствует профайл игрока в топ 10
     *
     * @param profile профайл игрока для проверки
     * @return true если присутствует
     */
    public boolean contains(UserProfile profile) {
        for(League league : leagues) {
            Division division = getDivisionForUserInLeague(profile.getId(), league);
            if(division.contains(profile.getId()))
                return true;
        }
        return false;
    }

    public RatingProfileStructure getRatingProfileStructure(UserProfile profile) {
        for(League league : leagues) {
            Division division = getDivisionForUserInLeague(profile.getId(), league);
            RatingProfileStructure result = division.getTopPlayers().get(profile.getId());
            if(result != null) {
                return result;
            }
        }
        return null;
    }

    public void removeUser(UserProfile profile) {
        if(contains(profile)) {
            getDivisionForUser(profile).removeUser(profile);
        }
    }

    // перегрузка removeUser для случая, когда нам не доступен полный profile (например, при batchBan)
    public void removeUser(Long profileId) {
        for(League league : leagues) {
            Division division = getDivisionForUserInLeague(profileId, league);
            if(division.contains(profileId)) {
                division.removeUser(profileId);
                return;
            }
        }
    }

    /**
     * вернет список рейтинга для игрока в его дивизионе
     *
     * @param profile профиль игрока
     * @return список рейтинга для игрока
     */
    public List<RatingProfileStructure> getRatingList(UserProfile profile) {
        return getDivisionForUser(profile).getRatingList();
    }

    /**
     * @param profile   профайл игрока который нужно проверить на топ
     * @param maybeAdd  говорит о том, что профайл может быть теоретически добавлен в рейтинг
     * @return true если в топ был добавлен новый игрок
     */
    public boolean checkAndAddInRating(UserProfile profile, boolean maybeAdd) {
        if(!contains(profile)) {
            if(maybeAdd) {
                Division division = getDivisionForUser(profile);
                return division.checkAndAddUser(profile, clanMember_rank_skin);
            }
        } else {
            return updateRating(profile);
        }
        return false;
    }

    public boolean updateRating(UserProfile profile) {
        RatingProfileStructure ratingProfileStructure = getRatingProfileStructure(profile);
        if(ratingProfileStructure != null) {
            Division newDivision = getDivisionForUser(profile);
            Division currentDivision = getDivisionForUser(ratingProfileStructure.rating, profile.getId());
            // остался в том же дивизионе
            if(newDivision == currentDivision) {
                ratingProfileStructure.init(profile, clanMember_rank_skin);
                // проверяем
                currentDivision.reinit();
            } else {
                // перешел в новый дивизион
                currentDivision.removeUser(profile);
                return newDivision.checkAndAddUser(profile, clanMember_rank_skin);
            }
        }
        return false;
    }

    /**
     * получить дивизион игрока, в зависимости от его текущего рейтинга
     *
     * @param profile профиль игрока
     * @return Division
     */
    public Division getDivisionForUser(UserProfile profile) {
        return getDivisionForUser(profile.getRating(), profile.getId());
    }

    public Division getDivisionForUser(int rating, long profileId) {
        return divisions.get(divisionByRating(rating, profileId));
    }

    public Division getDivisionForUserInLeague(Long profileId, League league) {
        return divisions.get(getDivisionKey(league, getDivisionNum(profileId, league)));
    }

    private int divisionByRating(int rating, long profileId) {
        rating = Math.max(minRatingInNewcomersLeague, rating);
        for(League league : leagues) {
            if(rating >= league.getMin() && rating < league.getMax()) {
                return getDivisionKey(league, getDivisionNum(profileId, league));
            }
        }
        return -1;
    }

    /**
     * Метод возвращает номер дивизиона игрока в данной лиге, вычисляемого как остаток от деления id игрока на количество дивизионов в лиге
     *
     * @param profileId id игрока
     * @param league    лига в которой нужно узнать дивизион игрока
     * @return номер дивизиона игрока
     */
    private int getDivisionNum(long profileId, League league) {
        return (int) (profileId % league.getDivisionCount());
    }

    /**
     * Метод возвращает ключ дивизиона на основании лиги и номера дивизиона в лиге
     *
     * @param league      лига
     * @param divisionNum номер дивизиона в лиге
     * @return ключ дивизиона
     */
    private int getDivisionKey(League league, int divisionNum) {
        return league.getIndex() * 10000 + divisionNum;
    }

    /**
     * Добавит/отнимет нужное количество рейтинга
     *
     * @param profile профайл игрока
     * @param msg     результат боя
     */
    public void addRating(UserProfile profile, EndPvpBattleRequest msg) {
        int ratingPoints = msg.ratingPoints;

        addRatingFor(profile, ratingPoints);

        //обновляем глобальный ТОП
        checkAndAddInRating(profile, ratingPoints > 0);
    }

    public void addRatingFor(UserProfile profile, int ratingPoints) {
        int newRating = Math.max(0, profile.getRating() + ratingPoints);
        if(profile.getRating() >= minRatingInNewcomersLeague) {
            // рейтинг игрока после поражения не должен падать ниже порога лиги новичков
            if(newRating < minRatingInNewcomersLeague) {
                newRating = minRatingInNewcomersLeague;
            }
        } else if(ratingPoints < 0) {
            // если у игрока рейтинга меньше порога лиги новичков, он ничего не теряет
            newRating = profile.getRating();
        }
        profile.setRating(newRating);
        if(profileService.isClansEnabled()) {
            short socialId = profileService.getSocialIdForClan(profile);
            clanService.updateRating(new UpdateRatingRequest(socialId, profile.getId().intValue(), newRating, ratingPoints));
        }
    }

    // обнулить рейтинг игроку
    public void wipeRating(UserProfile profile) {
        // обнуляем глобальный рейтинг
        profile.setRating(0);
        if(contains(profile)) {
            updateRating(profile);
        }

        if(profileService.isClansEnabled()) {
            // обнуляем клановый рейтинг
            short socialId = profileService.getSocialIdForClan(profile);
            UpdateRatingRequest request = new UpdateRatingRequest(socialId, profile.getId().intValue(), 0, 0);
            request.wipeRating = true;
            clanService.updateRating(request);
        }
    }

    public void onCloseClanSeason() {
        for(Division division : getDivisions().values()) {
            for(RatingProfileStructure ratingProfileStructure : division.getTopPlayers().values()) {
                UserProfile profile = profileService.getUserProfile(ratingProfileStructure.id);
                ratingProfileStructure.clanMember = profileService.newClanMemberStructure(profile);
            }
        }
    }

    public void onRename(final UserProfile profile) {
        RatingProfileStructure ratingProfileStructure = getRatingProfileStructure(profile);
        if(ratingProfileStructure != null) {
            ratingProfileStructure.name = profile.getName();
        }
    }

    //====================== Getters and Setters =================================================================================================================================================

    public void setLeagues(List<League> leagues) {
        this.leagues = leagues;
    }

    public List<League> getLeagues() {
        return leagues;
    }

    public Map<Integer, Division> getDivisions() {
        return divisions;
    }

    public double getOverheadFactor() {
        return overheadFactor;
    }

    public void setOverheadFactor(double overheadFactor) {
        this.overheadFactor = overheadFactor;
    }

    public int getMinRatingInNewcomersLeague() {
        return minRatingInNewcomersLeague;
    }

    public int getLastLoginDays() {
        return lastLoginDays;
    }

    public void setLastLoginDays(int lastLoginDays) {
        this.lastLoginDays = lastLoginDays;
    }

}
