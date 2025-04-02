package com.pragmatix.app.services.rating;

import com.pragmatix.admin.messages.client.Discard;
import com.pragmatix.app.cache.loaders.UserProfileLoader;
import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.common.BattleState;
import com.pragmatix.app.domain.BackpackConfEntity;
import com.pragmatix.app.init.StuffCreator;
import com.pragmatix.app.init.WeaponsCreator;
import com.pragmatix.app.init.controller.InitController;
import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.app.model.*;
import com.pragmatix.app.services.*;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.clanserver.services.ClanSeasonService;
import com.pragmatix.gameapp.GameApp;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.services.OnlineService;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.gameapp.threads.Execution;
import com.pragmatix.gameapp.threads.ExecutionContext;
import com.pragmatix.server.Server;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple4;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.EXTRA;
import static com.pragmatix.server.Server.sysLog;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 06.05.2016 15:27
 */
public class SeasonService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // список сезонного(временного) оружия по месяцам(сезонам)
    private Map<Integer, List<SeasonWeaponItem>> weaponItemsBySeasons;

    private Map<Integer, Set<Integer>> stuffBySeasons;

    private Map<Integer, Set<Integer>> weaponsBySeasons;

    private Map<Integer, int[]> weaponsBySeasonsArr;

    private Map<Integer, int[]> stuffBySeasonsArr;

    @Resource
    private WeaponsCreator weaponsCreator;

    @Resource
    private WeaponService weaponService;

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private OnlineService onlineService;

    @Resource
    private SocialService socialService;

    @Resource
    private InitController initController;

    @Resource
    private GameApp gameApp;

    @Resource
    private ProfileService profileService;

    @Resource
    private SoftCache softCache;

    @Resource
    private UserProfileLoader userProfileLoader;

    @Resource
    private ClanSeasonService clanSeasonService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private DaoService daoService;

    @Resource
    private RatingServiceImpl ratingService;

    @Resource
    private StuffCreator stuffCreator;

    @Resource
    private RankService rankService;

    @Resource
    private Store store;

    @Resource
    private ProfileEventsService profileEventsService;

    public final String currentSeasonStartDateTimeStoreKey = "SeasonService.currentSeasonStartDateTime";

    @Value("${SeasonService.debugMode:false}")
    private boolean debugMode = false;

    @Value("${SeasonService.firstSeasonStartDate}")
    private String firstSeasonStartDate;

    // с этими очками начинают сезон те кто достиг рубиновой лиги
    @Value("${SeasonService.rubyRankStartPoints:5400}")
    private int rubyRankStartPoints;

    // если по итогам сезона очки падают ниже этого порога, обнуляем очки
    @Value("${SeasonService.maxPointsToRetain:50}")
    private int maxPointsToRetain;

    // награда начисляется только тем кто перодолел этот порог
    @Value("${SeasonService.bestAwardRank:15}")
    private int bestAwardRank;

    private Map<Integer, GenericAward> seasonAward;

    private Map<Integer, Map<Integer, Short>> topSeasonsAward;

    private LocalDateTime firstSeasonStartDateTime;

    private LocalDateTime currentSeasonStartDateTime;

    private boolean initialized = false;

    public void init() {
        firstSeasonStartDateTime = LocalDateTime.parse(firstSeasonStartDate);
        sysLog.info("firstSeasonStartDateTime = {}", firstSeasonStartDateTime);

        currentSeasonStartDateTime = LocalDateTime.parse(store.load(currentSeasonStartDateTimeStoreKey, String.class));
        sysLog.info("currentSeasonStartDateTime = {}", currentSeasonStartDateTime);

        // проверяем корректность конфига
        for(List<SeasonWeaponItem> seasonWeapons : weaponItemsBySeasons.values()) {
            for(SeasonWeaponItem seasonWeapon : seasonWeapons) {
                int weaponId = seasonWeapon.weaponId;
                Weapon weapon = weaponsCreator.getWeapon(weaponId);
                if(weapon == null)
                    throw new IllegalStateException("сезонное оружие id=" + weaponId + " отсутствует в конфиге!");
                if(!weapon.isSeasonal())
                    throw new IllegalStateException("оружие [" + weapon + "] должно быть сезонным!");
            }
        }
        for(Set<Integer> seasonStuff : stuffBySeasons.values()) {
            for(Integer stuffId : seasonStuff) {
                Stuff stuff = stuffCreator.getStuff(stuffId.shortValue());
                if(stuff == null)
                    throw new IllegalStateException("сезонный предмет id=" + stuffId + " отсутствует в конфиге!");
            }
        }
        // Map<Season, List<SeasonWeaponItem>> => Map<Season, Set<WeaponId>>
        weaponsBySeasons = weaponItemsBySeasons.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().map(item -> item.weaponId).collect(toSet())
                ));
        // Map<Season, List<SeasonWeaponItem>> => Map<Season, Array[WeaponId]>
        weaponsBySeasonsArr = weaponItemsBySeasons.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().mapToInt(item -> item.weaponId).toArray()
                ));

        // Map<Season, Set<Integer>> => Map<Season, Array[StuffId]>
        stuffBySeasonsArr = stuffBySeasons.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().mapToInt(item -> item).toArray()
                ));

        IntStream.rangeClosed(1, 12).forEach(season -> {
            if(weaponsBySeasons.getOrDefault(season, Collections.emptySet()).isEmpty())
                throw new IllegalStateException("не задано оружие для сезона [" + season + "]");
            if(stuffBySeasons.getOrDefault(season, Collections.emptySet()).isEmpty())
                throw new IllegalStateException("не заданы предметы для сезона [" + season + "]");
        });

        initialized = true;
    }

    @Scheduled(cron = "0 1 0 1 * *")
    public void closeSeason() {
        sysLog.info("Try close season...");
        if(!initialized) {
            sysLog.info("SeasonService is disabled!");
            return;
        }
        closeSeason(Server.sysLog);
    }

    public void closeSeason(Logger log) {
        log.info("Close current season {}", currentSeasonStartDateTime);
        if(Execution.EXECUTION.get() == null)
            Execution.EXECUTION.set(new ExecutionContext(gameApp));
        LocalDateTime currentSeasonFinishDateTime = currentSeasonStartDateTime.truncatedTo(ChronoUnit.DAYS).plusMonths(1);
        if(LocalDateTime.now().isBefore(currentSeasonFinishDateTime)) {
            log.error("Время окончаняи сезона [{}] ещё не пришло, операция закрытия сезона игнорируется", currentSeasonFinishDateTime);
            return;
        }

        log.info("discard true");
        onlineService.setDiscard(true);
        try {
            // уведомляем PVP сервер
            Discard discardMsg = new Discard(Discard.DISCARD);
            Messages.toServer(discardMsg, initController.getPvpServerAddress(), true);
            // делаем паузу, чтобы получить с pvp сервера команды об окончании боёв
            try {
                log.info("wait ...");
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }

            log.info("close sessions ...");
            for(Session session : gameApp.getSessions()) {
                Object user = session.getUser();
                if(user instanceof UserProfile) {
                    UserProfile userProfile = (UserProfile) user;
                    if(userProfile.getBattleState() == BattleState.SIMPLE) {
                        userProfile.setBattlesCount(userProfile.getBattlesCount() + 1);
                    }
                    profileService.updateSync(userProfile);
                    session.close();
                }
            }
            userProfileLoader.setEnabled(false);
            log.info("clean cache ...");
            softCache.visit(UserProfile.class, (key, value) -> softCache.remove(UserProfile.class, key));

            // бекапим
            if(!backupRanksTable(log))
                throw new IllegalStateException("Ошибка создания резервной копии, прерываем операцию");

            Date currentSeasonStartDate = Date.from(currentSeasonStartDateTime.truncatedTo(ChronoUnit.DAYS).atZone(ZoneId.systemDefault()).toInstant());
            log.info("fill season_total table ...");
            daoService.update(
                    "INSERT INTO wormswar.season_total (season, profile_id, rank, rank_points) SELECT ?, profile_id, best_rank, rank_points FROM wormswar.ranks WHERE best_rank <= ?",
                    currentSeasonStartDate, bestAwardRank);

            log.info("append seasons_best_rank column ...");
            daoService.update(
                    "INSERT INTO wormswar.backpack_conf (profile_id) SELECT profile_id FROM wormswar.ranks R WHERE NOT exists (SELECT * FROM wormswar.backpack_conf C WHERE C.profile_id = R.profile_id) AND best_rank <= ?;" +
                            " UPDATE wormswar.backpack_conf SET seasons_best_rank = seasons_best_rank " +
                            " || trim(E'\\\\x64'::BYTEA FROM substring(int4send(coalesce((SELECT best_rank FROM wormswar.ranks R WHERE R.profile_id = backpack_conf.profile_id), 100)) FROM 4 FOR 1))",
                    bestAwardRank);

            log.info("update ranks table ...");
            daoService.update("UPDATE wormswar.ranks SET best_rank = ?, rank_points = (CASE WHEN best_rank = 0 THEN ? ELSE rank_points END) / 6", RankService.INIT_RANK_VALUE, rubyRankStartPoints);

            log.info("partial clean ranks table ...");
            daoService.update("DELETE FROM wormswar.ranks WHERE rank_points < ?", maxPointsToRetain);

            log.info("fill top_place column ...");
            daoService.doInTransactionWithoutResult(() -> {
                List<RatingProfileStructure> ratingList = ratingService.getSeasonRubyDivision().getRatingList();
                for(int i = 0, ratingListSize = ratingList.size(); i < ratingListSize; i++) {
                    RatingProfileStructure ratingProfileStructure = ratingList.get(i);
                    if(ratingProfileStructure.rank == 0) {
                        jdbcTemplate.update("UPDATE wormswar.season_total SET top_place = ? WHERE season = ? AND profile_id = ?",
                                i + 1, currentSeasonStartDate, ratingProfileStructure.getProfileId());
                    }
                }
            });

            userProfileLoader.setEnabled(true);

            currentSeasonStartDateTime = LocalDateTime.now();
            daoService.doInTransactionWithoutResult(() -> store.save(currentSeasonStartDateTimeStoreKey, currentSeasonStartDateTime.toString()));
            log.info("currentSeasonStartDateTime = {}", currentSeasonStartDateTime);

            ratingService.cleanSeasonAndDailyTop();
            ratingService.init();

            log.info("close clan season ...");
            clanSeasonService.closeCurrentSeason();
        } catch (Exception e) {
            log.error(e.toString(), e);
        } finally {
            Discard discardMsg = new Discard(Discard.UNDISARD);
            Messages.toServer(discardMsg, initController.getPvpServerAddress(), true);

            userProfileLoader.setEnabled(true);

            log.info("discard false");
            onlineService.setDiscard(false);
        }
        log.info("Close season DONE.");
    }

    public Optional<LoginAwardStructure> awardForSeason(UserProfile profile) {
        if(!initialized)
            return Optional.empty();

        LocalDateTime closedSeason = currentSeasonStartDateTime.truncatedTo(ChronoUnit.DAYS).minusMonths(1);
        Tuple2<Integer, Integer> rank_place = null;

        if(debugMode) {
            try {
                Map<String, String> awardParams = (Map<String, String>) Connections.get().getStore().get(ILogin.DEBUG_LOGIN_AWARDS);
                rank_place = Tuple.of(Integer.parseInt(awardParams.get("awardForSeason.rank")), Integer.parseInt(awardParams.get("awardForSeason.topPlace")));
            } catch (Exception e) {
            }
        }
        if(rank_place == null) {
            if(profile.isClosedSeasonAwardGranted(closedSeason.getMonthValue())) {
                return Optional.empty();
            }
            try {
                rank_place = jdbcTemplate.queryForObject("SELECT rank, top_place FROM wormswar.season_total " +
                                " WHERE season = ? AND profile_id = ? AND NOT granted ",
                        (res, rowNum) -> Tuple.of(res.getInt("rank"), res.getInt("top_place")), closedSeason, profile.getId());
            } catch (EmptyResultDataAccessException e) {
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }
        Optional<LoginAwardStructure> result = Optional.empty();
        // награда есть
        if(rank_place != null) {
            int rank = rank_place._1;
            int place = rank_place._2;
            GenericAward award = seasonAward.get(rank);
            if(award != null) {
                Stuff topStuff = stuffCreator.getStuff(topSeasonsAward.get(closedSeason.getMonthValue()).getOrDefault(place, (short) 0));
                if(topStuff != null) {
                    award = award.clone();
                    // выдаем шапку по 23:00 последнего дня текущего месяца
                    long expireTimeInSeconds = ZonedDateTime.now()
                            .plusMonths(1)
                            .withDayOfMonth(1)
                            .truncatedTo(ChronoUnit.DAYS)
                            .minusHours(1)
                            .toEpochSecond();
                    award.addStuffUntilTime(topStuff.getStuffId(), (int) expireTimeInSeconds);
                }
                String note = String.format("rank=%s place=%s", rank, place);
                AwardTypeEnum awardType = AwardTypeEnum.TOP_SEASON;
                List<GenericAwardStructure> awards = profileBonusService.awardProfile(award, profile, awardType, "season#bestRank", "" + rank, "season#topPlace", place);
                result = Optional.of(new LoginAwardStructure(awardType, awards, note));
                // отмечаем что мы её выдали
                daoService.doInTransactionWithoutResult(() -> jdbcTemplate.update("UPDATE wormswar.season_total SET granted = TRUE WHERE season = ? AND profile_id = ?", closedSeason, profile.getId()));
                profile.setClosedSeasonMonth(closedSeason.getMonthValue());
            } else {
                log.error("для ранга [{}] не определена награда!", rank);
            }
        } else {
            profile.setClosedSeasonMonth(closedSeason.getMonthValue());
        }
        return result;
    }

    private boolean backupRanksTable(Logger logger) {
        Process p;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        try {
            String backupFileName = "data/ranks_table_" + (sdf.format(new Date())) + ".backup";
            String command = daoService.pgDumpPath + "pg_dump --format=c --table=wormswar.ranks --username=postgres --port=" + daoService.dataSourcePort + " --file=" + backupFileName + " " + daoService.dataSourceDb;
            logger.info(command);
            p = Runtime.getRuntime().exec(command);
            p.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                logger.info(line);
                line = reader.readLine();
            }

            reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            line = reader.readLine();
            while (line != null) {
                logger.error(line);
                line = reader.readLine();
            }

            File file = new File(backupFileName);
            return file.exists() && file.length() > 0;
        } catch (InterruptedException e) {
            // если задача прерывается монитором
            logger.error(e.toString());
            return true;
        } catch (Exception e) {
            logger.error(e.toString(), e);
            return false;
        }
    }

    /**
     * @return - список удаленного оружия (id и кол-во)
     * - компенсация (фузы и ключи)
     * - список id оружий с прошлого сезона, которые мы сохранили игроку
     */
    public Tuple4<List<BackpackItemStructure>, Integer, Integer, Set<Integer>> withdrawSeasonWeapon(UserProfile profile) {
        List<BackpackItemStructure> withdrawnWeapons = Collections.emptyList();
        Set<Integer> prevSeasonWeapons = Collections.emptySet();

        boolean debugMode = false;
        if(this.debugMode) {
            try {
                Map<String, String> awardParams = (Map<String, String>) Connections.get().getStore().get(ILogin.DEBUG_LOGIN_AWARDS);
                debugMode = Boolean.parseBoolean(awardParams.get("withdrawSeasonWeapon"));
            } catch (Exception e) {
            }
        }

        if(profile.getLastLoginDateTime().isBefore(firstSeasonStartDateTime) && LocalDateTime.now().isAfter(firstSeasonStartDateTime)) {
            withdrawnWeapons = profile.getBackpack().stream()
                    .filter(backpackItem -> weaponsCreator.getWeapon(backpackItem.getWeaponId()).isSeasonal() && backpackItem.isNotEmpty())
                    .map(BackpackItemStructure::new)
                    .collect(Collectors.toList());
        } else if(profile.getLastLoginDateTime().isAfter(firstSeasonStartDateTime) && profile.getLastLoginDateTime().isBefore(currentSeasonStartDateTime)
                || debugMode) {
            // оставляем оружие прошлого сезона
            withdrawnWeapons = profile.getBackpack().stream()
                    .filter(backpackItem -> weaponsCreator.getWeapon(backpackItem.getWeaponId()).isSeasonal() && backpackItem.isNotEmpty())
                    .filter(backpackItem -> !getPrevSeasonWeapons().contains(backpackItem.getWeaponId()))
                    .map(BackpackItemStructure::new)
                    .collect(Collectors.toList());
            // передаем на клиент оставленное оружие прошлого сезона
            prevSeasonWeapons = profile.getBackpack().stream()
                    .filter(backpackItem -> weaponsCreator.getWeapon(backpackItem.getWeaponId()).isSeasonal() && backpackItem.isNotEmpty())
                    .filter(backpackItem -> getPrevSeasonWeapons().contains(backpackItem.getWeaponId()))
                    .map(BackpackItem::getWeaponId)
                    .collect(Collectors.toSet());
            // корректируем best_rank игрока в начале сезона
            rankService.onSetRankPoints(profile);
        }
        int compensationInMoney = 0;
//        int compensationInMoney = withdrawnWeapons.stream()
//                .mapToInt(item -> item.count * weaponsCreator.getWeapon(item.weaponId).getSellPrice())
//                .sum();
        int compensationInKeys = Math.round(compensationInMoney / 50);

        if(compensationInMoney > 0) {
            GenericAward compensation = GenericAward.builder()
                    .addMoney(compensationInMoney)
                    .addReagent(51, compensationInKeys)
                    .build();
            // формируем строку для статистики и удаляем оружие
            List<String> statParamValue = withdrawnWeapons.stream()
                    .peek(item -> weaponService.removeWeapon(profile, item.weaponId))
                    .map(item -> "" + item.weaponId + ":-" + item.count).collect(Collectors.toList());
            // выдаем компенсацию
            profileBonusService.awardProfile(compensation, profile, AwardTypeEnum.SEASON_COMPENSATION,
                    Param.lastLoginTime, profile.getLastLoginTime(),
                    Param.weapons, statParamValue,
                    Param.profile_backpack, profileEventsService.fillBackpackTrimmed(profile)
            );
        } else if(!withdrawnWeapons.isEmpty()) {
            // формируем строку для статистики и удаляем оружие
            List<String> statParamValue = withdrawnWeapons.stream()
                    .peek(item -> weaponService.removeWeapon(profile, item.weaponId))
                    .map(item -> "" + item.weaponId + ":-" + item.count).collect(Collectors.toList());
            profileEventsService.fireProfileEventAsync(EXTRA, profile,
                    Param.eventType, "withdrawWeapons",
                    Param.lastLoginTime, profile.getLastLoginTime(),
                    Param.weapons, statParamValue,
                    Param.profile_backpack, profileEventsService.fillBackpackTrimmed(profile)
            );
        }
        return Tuple.of(withdrawnWeapons, compensationInMoney, compensationInKeys, prevSeasonWeapons);
    }

    public byte[] getSeasonsBestRank(UserProfile profile) {
        BackpackConfEntity backpackConf = weaponService.getBackpackConfEntity(profile);
        if(backpackConf != null) {
            return backpackConf.getSeasonsBestRank();
        } else {
            return null;
        }
    }

    public boolean isEnabledInCurrentSeason(Weapon weapon) {
        return !weapon.isSeasonal() || getCurrentSeasonWeapons().contains(weapon.getWeaponId());
    }

    public List<SeasonWeaponItem> getCurrentSeasonWeaponItems() {
        int monthValue = currentSeasonStartDateTime.getMonthValue();
        return weaponItemsBySeasons.getOrDefault(monthValue, Collections.emptyList());
    }

    public Set<Integer> getCurrentSeasonWeapons() {
        int monthValue = currentSeasonStartDateTime.getMonthValue();
        return weaponsBySeasons.getOrDefault(monthValue, Collections.emptySet());
    }

    public Set<Integer> getNextSeasonWeapons() {
        int monthValue = currentSeasonStartDateTime.plusMonths(1).getMonthValue();
        return weaponsBySeasons.getOrDefault(monthValue, Collections.emptySet());
    }

    public Set<Integer> getPrevSeasonWeapons() {
        int monthValue = currentSeasonStartDateTime.minusMonths(1).getMonthValue();
        return weaponsBySeasons.getOrDefault(monthValue, Collections.emptySet());
    }

    public int[] getCurrentSeasonWeaponsArr() {
        int monthValue = currentSeasonStartDateTime.getMonthValue();
        return weaponsBySeasonsArr.getOrDefault(monthValue, ArrayUtils.EMPTY_INT_ARRAY);
    }

    public int[] getCurrentSeasonStuffArr() {
        int monthValue = currentSeasonStartDateTime.getMonthValue();
        return stuffBySeasonsArr.getOrDefault(monthValue, ArrayUtils.EMPTY_INT_ARRAY);
    }

    public void setSeasonItems(Map<Integer, SeasonItems> seasonItems) {
        this.weaponItemsBySeasons = seasonItems.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSeasonWeapons()));
        this.stuffBySeasons = seasonItems.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new TreeSet<>(e.getValue().getSeasonStuff())));
    }

    public LocalDateTime getFirstSeasonStartDateTime() {
        return firstSeasonStartDateTime;
    }

    public void setFirstSeasonStartDateTime(LocalDateTime firstSeasonStartDateTime) {
        this.firstSeasonStartDateTime = firstSeasonStartDateTime;
    }

    public void setCurrentSeasonStartDateTime(LocalDateTime currentSeasonStartDateTime) {
        this.currentSeasonStartDateTime = currentSeasonStartDateTime;
    }

    public void setSeasonAward(List<GenericAward> seasonAward) {
        this.seasonAward = seasonAward.stream()
                .peek((ga) -> ga.compressResult = false)
                .collect(Collectors.toMap(GenericAward::getKey, (ga) -> ga));
    }

    public void setTopSeasonsAward(Map<int[], Map<String, Short>> topSeasonsAward) {
        this.topSeasonsAward = new HashMap<>();
        try {
            for(Map.Entry<int[], Map<String, Short>> seasons_topAward : topSeasonsAward.entrySet()) {
                int[] seasons = seasons_topAward.getKey();
                Map<Integer, Short> topSeasonAward = new HashMap<>();
                for(Map.Entry<String, Short> entry : seasons_topAward.getValue().entrySet()) {
                    Stuff stuff = stuffCreator.getStuff(entry.getValue());
                    if(stuff == null)
                        throw new IllegalStateException("Награда за ТОП сезоов " + Arrays.toString(seasons) + ": Предмет " + entry.getValue() + " не найден!");
                    if(!stuff.isTemporal())
                        throw new IllegalStateException("Награда за ТОП сезонов " + Arrays.toString(seasons) + ": Предмет " + stuff + " должен быть временным!");

                    if(entry.getKey().contains("_")) {
                        String[] from_to = entry.getKey().split("_");
                        IntStream.rangeClosed(Integer.parseInt(from_to[0]), Integer.parseInt(from_to[1])).forEach((place -> {
                            addAwardForPlace(seasons, topSeasonAward, place, stuff.getStuffId());
                        }));
                    } else {
                        int place = Integer.parseInt(entry.getKey());
                        addAwardForPlace(seasons, topSeasonAward, place, stuff.getStuffId());
                    }
                }
                for(int season : seasons) {
                    Map<Integer, Short> oldValue = this.topSeasonsAward.put(season, topSeasonAward);
                    if(oldValue != null)
                        throw new IllegalStateException("Переопределяется награда за ТОП " + season + " сезона!");
                }
            }
            IntStream.rangeClosed(1, 12).forEach(season -> {
                if(!this.topSeasonsAward.containsKey(season)) throw new IllegalStateException("За сезон " + season + " не определена награда!");
            });
        } catch (Exception e) {
            log.error(e.toString(), e);
            throw e;
        }
    }

    private void addAwardForPlace(int[] seasons, Map<Integer, Short> topSeasonAward, int place, Short stuffId) {
        if(topSeasonAward.put(place, stuffId) != null) {
            throw new IllegalStateException("Награда за ТОП сезонов " + Arrays.toString(seasons) + ": За место " + place + " переопределяется предмет!");
        }
    }

    public int getRubyRankStartPoints() {
        return rubyRankStartPoints;
    }

    public int getMaxPointsToRetain() {
        return maxPointsToRetain;
    }

    public int getBestAwardRank() {
        return bestAwardRank;
    }

    public LocalDate getCurrentSeasonStartDate() {
        return currentSeasonStartDateTime.toLocalDate();
    }
}
