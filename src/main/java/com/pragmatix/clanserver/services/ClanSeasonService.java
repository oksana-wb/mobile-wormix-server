package com.pragmatix.clanserver.services;

import com.google.common.io.Resources;
import com.pragmatix.admin.messages.client.Discard;
import com.pragmatix.app.cache.loaders.UserProfileLoader;
import com.pragmatix.app.common.BattleState;
import com.pragmatix.app.init.controller.InitController;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.clan.ClanInteropServiceImpl;
import com.pragmatix.clanserver.dao.DAO;
import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.clanserver.domain.Season;
import com.pragmatix.gameapp.IGameApp;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.services.OnlineService;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.gameapp.threads.Execution;
import com.pragmatix.gameapp.threads.ExecutionContext;
import org.apache.commons.codec.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.pragmatix.server.Server.sysLog;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 10.10.13 12:33
 */
@Service
public class ClanSeasonService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private IGameApp gameApp;

    @Resource
    private ClanServiceImpl clanService;

    @Resource
    private SoftCache softCache;

    @Resource
    private ClanRepoImpl clanRepo;

    @Resource
    private DAO dao;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private OnlineService onlineService;

    @Resource
    private SocialService socialService;

    @Resource
    private InitController initController;

    @Resource
    private ProfileService profileService;

    @Resource
    private UserProfileLoader userProfileLoader;

    @Value("${dataSource.port:5432}")
    private int dataSourcePort;

    @Value("${dataSource.db:wormswar}")
    private String dataSourceDb;

    @Value("${pgDumpPath:}")
    private String pgDumpPath;

    @Value("${season.duration.calendarField:MONTH}")
    private String seasonDurationCalendarField;

    private int seasonDurationCalendarIntField = -1;

    @Value("${season.duration:1}")
    private int seasonDuration;

    @Resource
    private RatingServiceImpl ratingService;

    @Resource
    private ClanInteropServiceImpl interopService;

    /**
     * true если НЕ разрешено логиниться на сервер
     */
    private volatile boolean discard = false;

    /**
     * true если НЕ разрешено обращаться к базе данных
     */
    private volatile boolean discardDAO = false;

    /**
     * заморозить изменение сезонного рейтинга кланов
     */
    public volatile boolean rejectUpdateRating;

    private volatile Season currentSeason;

    private static final String[] CALENDAR_FIELD_NAME = {
            "ERA", "YEAR", "MONTH", "WEEK_OF_YEAR", "WEEK_OF_MONTH", "DAY_OF_MONTH",
            "DAY_OF_YEAR", "DAY_OF_WEEK", "DAY_OF_WEEK_IN_MONTH", "AM_PM", "HOUR",
            "HOUR_OF_DAY", "MINUTE", "SECOND", "MILLISECOND", "ZONE_OFFSET",
            "DST_OFFSET"
    };

    @Value("${ClanSeasonService.enabled:true}")
    private boolean ENABLED = true;

    @Value("${ClanSeasonService.canQuitClanDays:5}")
    private int canQuitClanDays = 5;

    public void init() {
        if (!ENABLED) return;

        for(int i = 0; i < CALENDAR_FIELD_NAME.length; i++) {
            if (CALENDAR_FIELD_NAME[i].equals(seasonDurationCalendarField)) {
                seasonDurationCalendarIntField = i;
                break;
            }
        }
        if (seasonDurationCalendarIntField == -1) {
            throw new IllegalStateException("Не верное значение свойства season.duration.calendarField " + seasonDurationCalendarField);
        }
        try {
            currentSeason = dao.selectCurrentOpenSeason();
        } catch (Exception e) {
            sysLog.error(e.toString(), e);
        }
        if (currentSeason == null) {
            throw new IllegalStateException("Не найден открытый сезон для текущей даты");
        }
    }

    // в 00:02 первого числа каждого месяца
    public void closeCurrentSeason() {
        sysLog.info("closeCurrentSeason...");

        if (!ENABLED) {
            sysLog.info("Clan server is disabled!");
            return;
        }

        if (currentSeason != null) {
            closeCurrentSeason(sysLog);
        } else {
            sysLog.error("currentSeason is NULL!");
        }
    }

    public void closeCurrentSeason(Logger logger) {
        logger.info("Close current {}", currentSeason);
        if (new Date().before(currentSeason.finish)) {
            logger.error("время окончаняи сезона ещё не пришло, операция закрытия сезона игнорируется");
            return;
        }

        if (Execution.EXECUTION.get() == null) {
            Execution.EXECUTION.set(new ExecutionContext(gameApp));
        }
        logger.info("discard true");
        onlineService.setDiscard(true);

        rejectUpdateRating = true;
        discard = true;

        try {
            // уведомляем PVP сервер
            Discard discardMsg = new Discard(Discard.DISCARD);
            Messages.toServer(discardMsg, initController.getPvpServerAddress(), true);
            // делаем паузу, чтобы получить с pvp сервера команды об окончании боёв
            try {
                logger.info("wait ...");
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }

            int i = 0;
            logger.info("close sessions ...");
            for(Session session : gameApp.getSessions()) {
                Object user = session.getUser();
                if (user instanceof UserProfile) {
                    UserProfile userProfile = (UserProfile) user;
                    if (userProfile.getBattleState() == BattleState.SIMPLE) {
                        userProfile.setBattlesCount(userProfile.getBattlesCount() + 1);
                    }
                    ClanMember clanMember = clanService.getClanMember(userProfile.getId().intValue());
                    //выполняем действия при выходе
                    clanService.onLogout(clanMember, false);

                    profileService.updateSync(userProfile);
                    session.close();
                    i++;
                }
            }
            logger.info("closed {} sessions", i);
            userProfileLoader.setEnabled(false);

            logger.info("clean cache ...");
            softCache.visit(UserProfile.class, (key, value) -> softCache.remove(UserProfile.class, key));

            i = 0;
            Set<Integer> allClans = new HashSet<>(clanRepo.getMemberCache().values());
            for(Integer clanId : allClans) {
                softCache.remove(Clan.class, clanId);
                i++;
            }
            logger.info("removed {} clans from cache", i);

            cleanSoftCache(logger);

            try {
                // выжидаем для верности
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            discardDAO = true;

            clanRepo.getMemberCache().clear();

            try {
                // выжидаем для верности
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            if (clanRepo.getMemberCache().size() > 0) {
                throw new IllegalStateException("не удалось обеспечить чистоту кеша, прерываем операцию");
            }

            // бекапим кланы
            if (!backupClanScheme(logger)) {
                throw new IllegalStateException("ошибка создания резервной копии, прерываем операцию");
            }

            // создаем новый сезон
            if (!createNewSeason(logger)) {
                throw new IllegalStateException("ошибка создания нового сезона, прерываем операцию");
            }
            logger.info("Create new {}", currentSeason);

            //обнуляем TOP
            ratingService.cleanTop();

            try {
                // выдаем награды
                logger.info("award users ...");
                URL url = Resources.getResource("close_season.sql");
                final String closeSeasonSql = Resources.toString(url, StandardCharsets.UTF_8).replaceAll("#seasonId#", "" + (currentSeason.id - 1));
                transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                        jdbcTemplate.update(closeSeasonSql);
                    }
                });
            } catch (Exception e) {
                logger.error(e.toString(), e);
            }

            try {
                // чистим ещё раз, для верности
                logger.info("clean season rating and cache (again) ...");

                cleanSoftCache(logger);
                clanRepo.getMemberCache().clear();

                transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                        jdbcTemplate.update("UPDATE clan.clan SET season_rating = 0;\n" +
                                "UPDATE clan.clan_member SET season_rating = 0;");
                    }
                });
            } catch (Exception e) {
                logger.error(e.toString(), e);
            }

            userProfileLoader.setEnabled(true);

            logger.info("reload top ...");
            // обновляем TOP
            ratingService.reloadTop();

            logger.info("Close season done.");
        } catch (Exception e) {
            logger.error(e.toString(), e);
        } finally {
            discardDAO = false;
            rejectUpdateRating = false;
            discard = false;

            Discard discardMsg = new Discard(Discard.UNDISARD);
            Messages.toServer(discardMsg, initController.getPvpServerAddress(), true);

            userProfileLoader.setEnabled(true);

            logger.info("discard false");
            onlineService.setDiscard(false);
        }

        logger.info("fire main server ...");
        interopService.onCloseSeason();
    }

    public void cleanSoftCache(Logger logger) {
        final int[] i = {0};
        softCache.visit(Clan.class, (key, value) -> {
            Integer clanId = (Integer) key;
            softCache.remove(Clan.class, clanId);
            i[0]++;
        });
        logger.info("#2: removed {} clans from cache", i[0]);
    }

    protected boolean createNewSeason(Logger logger) {
        int nextSeasonId = currentSeason.id + 1;
        Date nextStartDate = currentSeason.finish;
        Date nextFinishDate = addToDate(currentSeason.finish, seasonDurationCalendarIntField, seasonDuration);

        int result = 0;
        try {
            Season newSeason = new Season(nextSeasonId, nextStartDate, nextFinishDate, false);
            logger.info("insert new " + newSeason);
            result = dao.insertNewSeason(newSeason);
        } catch (Exception e) {
            logger.error(e.toString(), e);
            return false;
        }
        Season season = dao.selectCurrentOpenSeason();
        if (result != 1 || season == null || season.id != nextSeasonId) {
            logger.error("ошибка создания нового сезона в базе");
            return false;
        }

        this.currentSeason = season;
        return true;
    }

    private Date addToDate(Date date, int calendarField, int amount) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(calendarField, amount);
        return cal.getTime();
    }

    private boolean backupClanScheme(Logger logger) {
        Process p;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        try {
            String backupFileName = "data/clan_scheme_" + (sdf.format(new Date())) + ".backup";
            String command = pgDumpPath + "pg_dump --format=c --schema=clan --username=postgres --port=" + dataSourcePort + " --file=" + backupFileName + " " + dataSourceDb;
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
     * проверяем премировали ли игрока по итогам сезона и не выдавали ли ещё ему награду
     */
    public AwardForClosedSeason getMembersAwardForClosedSeason(final short socialId, final int profileId) {
        final AwardForClosedSeason result = new AwardForClosedSeason();
        final int closedSeasonId = currentSeason.id - 1;
        try {
            jdbcTemplate.query("SELECT ST.clan_id, ST.place, rank, medal_count, season_rating FROM clan.member_award MA " +
                    " INNER JOIN clan.season_total ST ON ST.clan_id = MA.clan_id AND ST.season_id = MA.season_id " +
                    " WHERE MA.season_id = ? AND social_id = ? AND profile_id = ? AND NOT granted ", res -> {
                result.clanId = res.getInt("clan_id");
                result.place = res.getInt("place");
                result.medalCount = res.getShort("medal_count");
                result.seasonRating = res.getInt("season_rating");
                result.rank = Rank.valueOf(res.getShort("rank"));
            }, closedSeasonId, socialId, profileId);

            // награда есть, отмечаем что мы её выдали
            if (!result.isEmpty()) {
                transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                        jdbcTemplate.update("UPDATE clan.member_award SET granted = TRUE WHERE season_id = ? AND social_id = ? AND profile_id = ?", closedSeasonId, socialId, profileId);
                    }
                });
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        return result;
    }

    public boolean canQuitClan() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentSeason.start);
        cal.add(Calendar.DAY_OF_MONTH, canQuitClanDays);
        return new Date().before(cal.getTime());
    }

    public static class AwardForClosedSeason {
        public int clanId;
        public int seasonRating;
        public int place;
        public Rank rank;
        public short medalCount;

        public boolean isEmpty() {
            return medalCount == 0;
        }
    }

    //====================== Getters and Setters =================================================================================================================================================

    public boolean isDiscard() {
        return discard;
    }

    public void setDiscard(boolean discard) {
        this.discard = discard;
    }

    public boolean isRejectUpdateRating() {
        return rejectUpdateRating;
    }

    public void setRejectUpdateRating(boolean rejectUpdateRating) {
        this.rejectUpdateRating = rejectUpdateRating;
    }

    public Season getCurrentSeason() {
        return currentSeason;
    }

    public void setCurrentSeason(Season currentSeason) {
        this.currentSeason = currentSeason;
    }

    public boolean isDiscardDAO() {
        return discardDAO;
    }

    public void setDiscardDAO(boolean discardDAO) {
        this.discardDAO = discardDAO;
    }
}
