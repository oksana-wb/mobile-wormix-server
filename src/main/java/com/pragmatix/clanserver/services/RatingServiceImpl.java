package com.pragmatix.clanserver.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pragmatix.clanserver.dao.DAO;
import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.domain.RatingItem;
import com.pragmatix.clanserver.domain.ReviewState;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.server.Server;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: Vladimir
 * Date: 22.04.13 14:57
 */
@Service
public class RatingServiceImpl implements RatingService {

    static Logger log = LoggerFactory.getLogger(RatingServiceImpl.class);

    public static final int PROGRESS_DEEP = 6;

    @Autowired
    private DAO dao;

    private List<RatingItem> seasonTop;

    private List<RatingItem> joins;

    private Map<Integer, RatingItem> topMap;

    private static final Comparator<RatingItem> SEASON_RATING_COMPARATOR = (o1, o2) -> o1.seasonRating != o2.seasonRating ? o2.seasonRating - o1.seasonRating : o2.clanId.compareTo(o1.clanId);

    private static final Comparator<RatingItem> JOIN_RATING_COMPARATOR = (o1, o2) -> o1.joinRating != o2.joinRating ? o1.joinRating - o2.joinRating : o1.clanId.compareTo(o2.clanId);

    /**
     * история позиции клана в сезонном топе, с шагом 4 часа в течении суток
     */
    private Map<Integer, short[]> seasonProgressMap = new ConcurrentHashMap<>();

    /**
     * история позиции игрока в топе клана, с шагом 4 часа в течении суток
     */
    private Map<Long, byte[]> clanMembersProgressMap = new ConcurrentHashMap<>();

    @Resource
    private ClanRepoImpl clanRepo;

    @Resource
    private PersistenceService persistenceService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private ClanSeasonService seasonService;

    static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();

    private boolean initialized = false;

    private String seasonProgressKeepFileName = "ClanRatingService.seasonProgress";

    private String membersProgressKeepFileName = "ClanRatingService.membersProgress";

    @Value("${ClanSeasonService.enabled:true}")
    private boolean ENABLED = true;

    public void init() throws ParseException {
        if(!ENABLED) return;
        // пытаемся восстановить текущий прогресс (история позиции клана в сезонном топе) с диска
        try {
            Map<Integer, short[]> progressMap = persistenceService.restoreObjectFromFile(Map.class, seasonProgressKeepFileName);
            if(progressMap != null && progressMap.size() > 0) {
                Server.sysLog.info("Loaded clanSeasonProgress from file for {} clans", progressMap.size());
                this.seasonProgressMap = progressMap;
            }
        } catch (Exception e) {
            log.error("Error during restore seasonProgressMap of ClanRatingService: " + e.toString(), e);
        }

        // пытаемся восстановить текущий прогресс участников (история позиции игрока в клане) с диска
        try {
            Map<Long, byte[]> progressMap = persistenceService.restoreObjectFromFile(Map.class, membersProgressKeepFileName);
            if(progressMap != null && progressMap.size() > 0) {
                Server.sysLog.info("Loaded membersSeasonProgress from file for {} members", progressMap.size());
                this.clanMembersProgressMap = progressMap;
            } else {
                // заполняем прогресс участникоа кланов текущими позициями
                Server.sysLog.info("Load membersSeasonProgress from DB ...");
                storeMembersTopPositions();
                Server.sysLog.info("Loaded membersSeasonProgress from DB for {} members", clanMembersProgressMap.size());
            }
        } catch (Exception e) {
            log.error("Error during restore clanMembersProgressMap of ClanRatingService: " + e.toString(), e);
        }

        reloadTop();

        initialized = true;
    }

    public void cleanTop() {
        seasonProgressMap = new ConcurrentHashMap<>();
        clanMembersProgressMap = new ConcurrentHashMap<>();
        synchronized (this) {
            this.joins = new ArrayList<>(0);
            this.seasonTop = new ArrayList<>(0);
            this.topMap = new ConcurrentHashMap<>(0);
        }
    }

    public void persistToDisk() {
        persistenceService.persistObjectToFile(seasonProgressMap, seasonProgressKeepFileName);
        Server.sysLog.info("Persisted clanSeasonProgress for {} clans", seasonProgressMap.size());

        persistenceService.persistObjectToFile(clanMembersProgressMap, membersProgressKeepFileName);
        Server.sysLog.info("Persisted clanMembersSeasonProgress for {} members", clanMembersProgressMap.size());
    }

    @Scheduled(cron = "0 0 */4 * * *")
    public void cronTask() {
        if(initialized) {
            storeSeasonTopPositions();
            storeMembersTopPositions();
        }
    }

    public void storeSeasonTopPositions() {
        Set<Integer> updatedClans = new HashSet<>();
        // обновляем позицию в прогрессе кланам если их сезонный рейтинг положительный
        for(int i = 0; i < seasonTop.size(); i++) {
            RatingItem ratingItem = seasonTop.get(i);
            Integer clanId = ratingItem.clanId;
            updateTopProgresFor(clanId, i);
            updatedClans.add(clanId);
            ratingItem.oldPlace = getOldPlace(clanId);
        }
        // если сезонный рейтинг клана <= 0
        for(RatingItem ratingItem : topMap.values()) {
            if(!updatedClans.contains(ratingItem.clanId)) {
                setEmptyPositionAndRemoveEmptiedProgress(ratingItem.clanId);
            }
        }
    }

    public void storeMembersTopPositions() {
        final Set<Long> updatedMembers = new HashSet<>();

        final int[] curClanId = {0};
        final byte[] curTopPlace = {0};
        // кешируем участников клана, отсортированных по рейтингу
        final List<ClanMember> clanMembers = new ArrayList<>();

        // обновляем позицию в прогрессе участникам клана
        jdbcTemplate.query("SELECT clan_id, social_id, profile_id FROM clan.clan_member " +
                "  ORDER BY clan_id, season_rating DESC", new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet res) throws SQLException {
                int clanId = res.getInt("clan_id");
                short socialId = res.getShort("social_id");
                int profileId = res.getInt("profile_id");

                if(clanId != curClanId[0]) {
                    curTopPlace[0] = 1;
                    curClanId[0] = clanId;
                    clanMembers.clear();
                } else {
                    curTopPlace[0]++;
                }

                ClanMember clanMember = null;

                byte position = 0;
                Long clanMemberId = getClanMemberId(clanId, socialId, profileId);
                Clan clan = clanRepo.getClanFromCache(clanId);
                if(clan == null) {
                    // клан и его участники отсутствуют в кеше
                    position = curTopPlace[0];
                } else {
                    // определяем позицию используя Clan
                    if(clanMembers.size() == 0) {
                        clanMembers.addAll(clan.members());
                        Collections.sort(clanMembers, new Comparator<ClanMember>() {
                            @Override
                            public int compare(ClanMember o1, ClanMember o2) {
                                return o2.seasonRating - o1.seasonRating;
                            }
                        });
                    }
                    int i = 0;
                    for(ClanMember _clanMember : clanMembers) {
                        i++;
                        if(_clanMember.profileId == profileId && _clanMember.socialId == socialId) {
                            position = (byte) i;
                            clanMember = _clanMember;
                            break;
                        }
                    }
                }

                updateClanTopProgresFor(clanMemberId, position);

                updatedMembers.add(clanMemberId);

                if(clanMember != null) {
                    clanMember.oldPlace = getMemberOldPlace(clanMemberId);
                }
            }

        });

        // если игрок перешел в другой клан или уже не состоит в клане
        for(Long _clanMemberId : clanMembersProgressMap.keySet()) {
            if(!updatedMembers.contains(_clanMemberId)) {
                // удаляем его прогресс
                clanMembersProgressMap.remove(_clanMemberId);
            }
        }
    }

    public int getMemberOldPlace(ClanMember clanMember) {
        Integer clanId = clanMember.getClanId();
        if(clanId == null) {
            return 0;
        }
        return getMemberOldPlace(getClanMemberId(clanId, clanMember.socialId, clanMember.profileId));
    }

    private int getMemberOldPlace(Long clanMemberId) {
        byte[] progress = clanMembersProgressMap.get(clanMemberId);
        return progress == null ? 0 : progress[0];
    }

    private Long getClanMemberId(int clanId, short socialId, int profileId) {
        // xxx x xxxx
        // clanId sosialId profileId
        return ((long) clanId) << 40 | ((long) socialId) << 32 | profileId;
    }

    private void updateClanTopProgresFor(Long clanMemberId, byte position) {
        byte[] progress = clanMembersProgressMap.get(clanMemberId);
        if(progress != null) {
            progress = Arrays.copyOf(progress, progress.length);
            System.arraycopy(progress, 1, progress, 0, progress.length - 1);
            progress[progress.length - 1] = position;
            clanMembersProgressMap.put(clanMemberId, progress);
        } else {
            progress = new byte[PROGRESS_DEEP];
            Arrays.fill(progress, position);
            clanMembersProgressMap.put(clanMemberId, progress);
        }
    }

    private void updateTopProgresFor(Integer clanId, Integer position) {
        short[] progress = seasonProgressMap.get(clanId);
        short positionAsShort = (short) ((position + 1) & 0x0000FFFF);
        if(progress != null) {
            progress = Arrays.copyOf(progress, progress.length);
            System.arraycopy(progress, 1, progress, 0, progress.length - 1);
            progress[progress.length - 1] = positionAsShort;
            seasonProgressMap.put(clanId, progress);
        } else {
            progress = new short[PROGRESS_DEEP];
            Arrays.fill(progress, positionAsShort);
            seasonProgressMap.put(clanId, progress);
        }
    }

    private void setEmptyPositionAndRemoveEmptiedProgress(Integer clanId) {
        short[] progress = seasonProgressMap.get(clanId);
        if(progress != null) {
            for(int i = 1; i < progress.length; i++) {
                // присутствует минимум одна положительная позиция
                if(progress[i] != 0) {
                    progress = Arrays.copyOf(progress, progress.length);
                    System.arraycopy(progress, 1, progress, 0, progress.length - 1);
                    progress[progress.length - 1] = 0;
                    seasonProgressMap.put(clanId, progress);
                    return;
                }
            }
            // всё по нулям
            seasonProgressMap.remove(clanId);
        }
    }

    /**
     * @return позицию игрока в топе 30 мин. назад или 0 если он не входит в топ 1000
     */
    public int getOldPlace(Integer clanId) {
        short[] progress = seasonProgressMap.get(clanId);
        return progress == null ? 0 : progress[0] & 0x0000FFFF;
    }

    @Override
    public void reloadTop() {
        List<RatingItem> top = dao.getTopRatings(0);

        List<RatingItem> joins = new ArrayList<>();
        List<RatingItem> seasonTop = new ArrayList<>();
        Map<Integer, RatingItem> topMap = new ConcurrentHashMap<>();
        for(RatingItem item : top) {
            if(item.joinRating >= 0) {
                joins.add(item);
            }
            if(item.seasonRating > 0) {
                seasonTop.add(item);
            }

            item.oldPlace = getOldPlace(item.clanId);

            topMap.put(item.clanId, item);
        }
        Collections.sort(joins, JOIN_RATING_COMPARATOR);
        Collections.sort(seasonTop, SEASON_RATING_COMPARATOR);

        synchronized (this) {
            this.seasonTop = seasonTop;
            this.joins = joins;
            this.topMap = topMap;
        }
    }

    @Override
    public RatingItem[] topN(int count, boolean season) {
        synchronized (this) {
            List<RatingItem> topList = seasonTop;

            RatingItem[] res = new RatingItem[Math.min(count, topList.size())];

            for(int i = 0; i < res.length; i++) {
                res[i] = topList.get(i);
            }
            return res;
        }
    }

    @Override
    public RatingItem[] joinsN(int rating, int count) {
        RatingItem boundary = new RatingItem(Integer.MAX_VALUE, rating, 0, 0);

        synchronized (this) {
            RatingItem[] res;
            int maxIndex = -1 - Collections.binarySearch(joins, boundary, JOIN_RATING_COMPARATOR);
            if(maxIndex <= count) {
                res = new RatingItem[maxIndex];
                for(int i = 0; i < res.length; i++) {
                    res[i] = joins.get(i);
                }
            } else {
                int offset = (int) (0x3FFFFFFF & System.currentTimeMillis());
                int step = maxIndex / count;
                res = new RatingItem[count];
                for(int i = 0; i < res.length; i++) {
                    offset %= maxIndex;
                    res[i] = joins.get(offset);
                    offset += step;
                }
            }

            return res;
        }
    }

    @Override
    public void updateRatings(Integer clanId, int rating, int seasonRating, int joinRating) {
        synchronized (this) {
            RatingItem item = topMap.get(clanId);

            if(item != null) {
                if(item.seasonRating > 0) {
                    seasonTop.remove(Collections.binarySearch(seasonTop, item, SEASON_RATING_COMPARATOR));
                }
                if(item.joinRating >= 0) {
                    joins.remove(Collections.binarySearch(joins, item, JOIN_RATING_COMPARATOR));
                }
                item.joinRating = joinRating;
                item.seasonRating = seasonRating;
                item.oldPlace = getOldPlace(clanId);
            } else {
                item = new RatingItem(clanId, joinRating, seasonRating, getOldPlace(clanId));

                topMap.put(clanId, item);
            }

            if(seasonRating > 0) {
                seasonTop.add(-1 - Collections.binarySearch(seasonTop, item, SEASON_RATING_COMPARATOR), item);
            }
            if(joinRating >= 0) {
                joins.add(-1 - Collections.binarySearch(joins, item, JOIN_RATING_COMPARATOR), item);
            }
        }
    }

    @Override
    public void updateRatings(Clan clan) {
        if(clan.reviewState == ReviewState.LOCKED) {
            updateRatings(clan.id, 0, 0, clan.joinRating);
        } else {
            updateRatings(clan.id, clan.rating, clan.seasonRating, clan.joinRating);
        }
    }

    @Override
    public RatingItem removeRating(Integer clanId) {
        seasonProgressMap.remove(clanId);
        synchronized (this) {
            RatingItem item = topMap.remove(clanId);
            if(item != null) {
                if(item.seasonRating > 0) {
                    seasonTop.remove(Collections.binarySearch(seasonTop, item, SEASON_RATING_COMPARATOR));
                }
                if(item.joinRating >= 0) {
                    joins.remove(Collections.binarySearch(joins, item, JOIN_RATING_COMPARATOR));
                }
            }
            return item;
        }
    }

    @Override
    public int position(Integer clanId, boolean season) {
        synchronized (this) {
            RatingItem item = topMap.get(clanId);

            if(item != null) {
                return Collections.binarySearch(seasonTop, item, SEASON_RATING_COMPARATOR);
            } else {
                return -1;
            }
        }
    }

    @Override
    public int seasonPosition(Integer clanId) {
        synchronized (this) {
            RatingItem item = topMap.get(clanId);

            if(item != null) {
                return Collections.binarySearch(seasonTop, item, SEASON_RATING_COMPARATOR) + 1;
            } else {
                return -1;
            }
        }
    }

    public int getSeasonRating(Integer clanId) {
        RatingItem ratingItem = topMap.get(clanId);
        return ratingItem != null ? ratingItem.seasonRating : 0;
    }

    public static String dailyRatingToGson(int[] dailyRating) {
        try {
            if(dailyRating.length == 0)
                return null;
            Object[] dailyRatingRaw = new Object[dailyRating.length];
            for(int i = 0; i < dailyRatingRaw.length; i = i + 2) {
                dailyRatingRaw[i] = new Date(dailyRating[i] * 1000L);
                dailyRatingRaw[i + 1] = dailyRating[i + 1];
            }
            return GSON.toJson(dailyRatingRaw);
        } catch (Exception e) {
            log.error(e.toString(), e);
            return "";
        }
    }

    public int[] dailyRatingFromGson(String dailyRatingGson) {
        if(StringUtils.isEmpty(dailyRatingGson))
            return ArrayUtils.EMPTY_INT_ARRAY;

        try {
            List<Integer> dailyRating = new ArrayList<>(14);
            long fromInSeconds = weekAgoDayInSeconds();
            Object[] dailyRatingRaw = GSON.fromJson(dailyRatingGson, Object[].class);
            for(int i = 0; i < dailyRatingRaw.length; i = i + 2) {
                int dateInSeconds = (int) LocalDate.parse((String) dailyRatingRaw[i]).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();
                if(dateInSeconds > fromInSeconds && dateInSeconds * 1000L >= seasonService.getCurrentSeason().start.getTime()) {
                    dailyRating.add(dateInSeconds);
                    dailyRating.add(((Double) dailyRatingRaw[i + 1]).intValue());
                }
            }
            return dailyRating.stream().mapToInt(i -> i).toArray();
        } catch (Exception e) {
            log.error(e.toString(), e);
            return ArrayUtils.EMPTY_INT_ARRAY;
        }
    }

    public int weeklyRating(ClanMember clanMember) {
        // подсчитываем рейтинг за последние 7-мь дней
        int weeklyRating = 0;
        long from = weekAgoDayInSeconds() * 1000L;
        int[] dailyRating = clanMember.dailyRating;
        for(int i = 0; i < dailyRating.length; i = i + 2) {
            long date = dailyRating[i] * 1000L;
            if(date > from && date >= seasonService.getCurrentSeason().start.getTime())
                weeklyRating += dailyRating[i + 1];
        }
        return weeklyRating;
    }

    public int yesterdayRating(ClanMember clanMember) {
        // подсчитываем рейтинг за вчера
        long yesterday = yesterdayInSeconds() * 1000L;
        int[] dailyRating = clanMember.dailyRating;
        for(int i = 0; i < dailyRating.length; i = i + 2) {
            long date = dailyRating[i] * 1000L;
            if(date == yesterday)
                return dailyRating[i + 1];
        }
        return 0;
    }

    public void updateDailyRating(ClanMember clanMember, int ratingPoints) {
        if(ratingPoints == 0)
            return;

        int[] dailyRating = clanMember.dailyRating;
        int todayInSeconds = todayInSeconds();
        if(dailyRating.length == 0) {
            clanMember.dailyRating = new int[]{todayInSeconds, ratingPoints};
        } else if(dailyRating[0] == todayInSeconds) {
            dailyRating[1] += ratingPoints;
        } else {
            int[] newDailyRating = new int[dailyRating.length + 2];
            newDailyRating[0] = todayInSeconds;
            newDailyRating[1] = ratingPoints;
            System.arraycopy(dailyRating, 0, newDailyRating, 2, dailyRating.length);
            clanMember.dailyRating = newDailyRating;
        }
    }

    public void wipeDailyRatings(ClanMember clanMember) {
        clanMember.dailyRating = ArrayUtils.EMPTY_INT_ARRAY;
    }

    private int todayInSeconds() {
        return (int) LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private int weekAgoDayInSeconds() {
        return (int) LocalDate.now().minusDays(7).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private int yesterdayInSeconds() {
        return (int) LocalDate.now().minusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();
    }

//====================== Getters and Setters =================================================================================================================================================

    public Map<Integer, short[]> getSeasonProgressMap() {
        return seasonProgressMap;
    }

    public Map<Long, byte[]> getClanMembersProgressMap() {
        return clanMembersProgressMap;
    }

    public void setSeasonService(ClanSeasonService seasonService) {
        this.seasonService = seasonService;
    }
}
