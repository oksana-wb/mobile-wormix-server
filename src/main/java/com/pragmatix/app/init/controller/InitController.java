package com.pragmatix.app.init.controller;

import com.pragmatix.achieve.controllers.AchieveController;
import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.admin.model.AdminProfile;
import com.pragmatix.app.common.BattleState;
import com.pragmatix.app.common.ServerRole;
import com.pragmatix.app.domain.AdminProfileEntity;
import com.pragmatix.app.domain.BanEntity;
import com.pragmatix.app.filters.AuthFilter;
import com.pragmatix.app.init.AdminProfileCreator;
import com.pragmatix.app.init.InitVkontakteServiceInProduction;
import com.pragmatix.app.model.BanItem;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.services.rating.RatingService;
import com.pragmatix.app.services.rating.SeasonService;
import com.pragmatix.app.services.social.SocialUserIdMapService;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.app.settings.BattleAwardSettings;
import com.pragmatix.chat.GlobalChatService;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.services.ClanDailyRegistry;
import com.pragmatix.clanserver.services.ClanSeasonService;
import com.pragmatix.clanserver.services.ClanServiceImpl;
import com.pragmatix.clanserver.services.RatingServiceImpl;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.gameapp.IGameApp;
import com.pragmatix.gameapp.cache.PermanentCache;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnInit;
import com.pragmatix.gameapp.services.DailyScheduleService;
import com.pragmatix.gameapp.services.OnlineService;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.threads.Execution;
import com.pragmatix.gameapp.threads.ExecutionContext;
import com.pragmatix.notify.NotifyService;
import com.pragmatix.performance.statictics.ValueHolder;
import com.pragmatix.pvp.services.PvpDailyRegistry;
import com.pragmatix.pvp.services.battletracking.PvpBattleTrackerService;
import com.pragmatix.pvp.services.matchmaking.BlackListService;
import com.pragmatix.pvp.services.matchmaking.WagerMatchmakingService;
import com.pragmatix.serialization.utils.SecurityUtils;
import com.pragmatix.server.Server;
import com.pragmatix.sessions.AppServerAddress;
import com.pragmatix.sessions.IAppServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.pragmatix.app.common.ServerRole.*;
import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.SHUTDOWN_SERVER;

/**
 * Контроллер который отвечает за инициализацию приложения
 * <p/>
 * User: denis
 * Date: 07.11.2009
 * Time: 2:15:32
 */
@Controller
public class InitController implements SmartLifecycle {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private DaoService daoService;

    @Resource
    private PermanentCache permanentCache;

    @Resource
    private RatingService ratingService;

    @Resource
    private SocialUserIdMapService socialUserIdMapService;

    @Resource
    private ProfileService profileService;

    @Resource
    private RestrictionService restrictionService;

    @Resource
    private BanService banService;

    @Resource
    private DailyScheduleService dailyScheduleService;

    @Value("${dailyScheduleService.runTime}")
    private String dailyScheduleServiceRunTime;

    @Resource
    private AdminProfileCreator adminProfileCreator;

    @Resource
    private AppParams appParams;

    @Resource
    private ReactionRateService reactionRateService;

    @Value("${secure.secret}")
    private String secureSecret;

    @Resource
    private SecurityUtils securityUtils;

    @Value("${server.id:development}")
    private String serverId;

    @Value("${server.role:ALL}")
    private ServerRole serverRole;

    @Value("${app.isMobile:false}")
    private boolean isMobile = false;

    @Resource
    private PvpBattleTrackerService pvpBattleTrackerService;

    @Resource
    private BlackListService blackListService;

    @Resource
    private AchieveController achieveController;

    @Resource
    private IGameApp gameApp;

    @Resource
    private OnlineService onlineService;

    @Resource
    private StuffService stuffService;

    @Resource
    private CraftService craftService;

    @Resource
    private CheatersCheckerService cheatersCheckerService;

    @Resource
    private CheatRegisterService cheatRegisterService;

    @Resource
    private GlobalChatService globalChatService;

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private UserRegistry userRegistry;

    @Resource
    private PvpDailyRegistry pvpDailyRegistry;

    @Resource
    private ClanDailyRegistry clanDailyRegistry;

    @Resource
    private PaymentService paymentService;

    private volatile boolean isInitComplete = false;

    private volatile boolean running = false;

    private volatile boolean stopping = false;

    @Value("${InitController.persistStateOnExit:true}")
    private boolean persistStateOnExit = true;

    @Resource
    private BattleAwardSettings battleAwardSettings;

    @Resource
    private HeroicMissionService heroicMissionService;

    @Resource
    private BattleService battleService;

    @Resource
    private ClanSeasonService clanSeasonService;

    @Resource
    private RatingServiceImpl clanRatingService;

    @Resource
    private ReferralLinkService referralLinkService;

    @Resource
    private BundleService bundleService;

    @Resource
    private WipeProfileService wipeProfileService;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private NotifyService notifyService;

    @Resource
    private BossBattleExtraRewardService bossBattleExtraRewardService;

    @Resource
    private WagerMatchmakingService wagerMatchmakingService;

    @Autowired(required = false)
    private SeasonService seasonService;

    @Autowired(required = false)
    private InitVkontakteServiceInProduction initVkontakteServiceInProduction;

    @Resource
    private org.eclipse.jetty.server.Server jettyServer;

    @Autowired
    protected ClanServiceImpl clanService;

    @Autowired
    protected AuthFilter authFilter;

    @Value("${pvpServer.address:pvp}")
    private String pvpServerAddress;

    @OnInit
    public void onInit() {
        try {
            // признак мобильной версии сервера
            AppParams.IS_MOBILE = isMobile;

            jettyServer.start();

            Server.sysLog.info("onInit started for server [{}] in role [{}]", serverId, serverRole);

            onlineService.setDiscard(true);

            profileService.init();

            //загружаем админов приложения
            loadAdmins();

            // задаём секретное слово для "засекреченных" команд
            securityUtils.setup(secureSecret);

            //инициализируем сервис который будет запускаться раз в сутки
            initDialyService();

            if (serverRole == MAIN || serverRole == ALL) {
                //ручная инициализация наград за бои
                battleAwardSettings.init();

                // регистрируем сброс онлайна в статистику
                gameApp.getStatCollector().needCollect("online", "main", new ValueHolder() {
                    @Override
                    public long getValue() {
                        return onlineService.get();
                    }
                }, false);

                // подгружаем структуры соответствий строковых и лонговых ID
                socialUserIdMapService.init();

                heroicMissionService.init();

                //инициализируем рейтинг игроков
                ratingService.init();

                restrictionService.init();

                //загружает бан лист из БД в память
                loadBanList();

                //дозаполняем призы
                profileBonusService.init();

                //инициализируем глобальный кеш выделенныых данных об игроках
                userRegistry.init();

                if (AppParams.IS_NOT_MOBILE()) {
                    //восстанавливаем информацию кто кому качал реакцию
                    reactionRateService.init();
                }

                // загружаем информацию о выданных временных шапках и переводим их во временные
                stuffService.init();

                craftService.init();

                // обновляем список игроков выбравших свой лимит рубинов за поиски домиков
                cheatersCheckerService.init();
                cheatRegisterService.init();

                battleService.init();

                // сервер кланов
                clanSeasonService.init();
                clanRatingService.init();

                referralLinkService.init();
                bundleService.init();

                wipeProfileService.init();

                dailyRegistry.init();

                clanDailyRegistry.init();

                paymentService.init();

                bossBattleExtraRewardService.loadReward();

                notifyService.init();

                if (seasonService != null)
                    seasonService.init();

                Execution.EXECUTION.get().scheduleTimeout(TimeUnit.MINUTES.toMillis(20), () -> {
                    Server.sysLog.info("Turn Off logon limit");
                    authFilter.setLoginsLimit(1024);
                });
            }

            if (serverRole == PVP || serverRole == ALL) {
                pvpDailyRegistry.init();

                // запускаем поток опроса боёв на предмет таймаутов
                pvpBattleTrackerService.init();

                // ограничение подбора
                blackListService.init();

                // инициализируем сервис подбора соперников
                wagerMatchmakingService.init();
            }

            // загружаем настройки приложения из базы
            appParams.init();

            if (initVkontakteServiceInProduction != null) {
                initVkontakteServiceInProduction.init();
            }

            Server.sysLog.info("IS_MOBILE = {}", AppParams.IS_MOBILE);
            Server.sysLog.info("onInit is completed for server [{}]", serverId);

            isInitComplete = true;
            running = true;

            onlineService.setDiscard(false);
        } catch (Exception e) {
            Server.sysLog.error(e.toString(), e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void stop() {
        if (!running) {
            Server.sysLog.warn("stop() is ignored");
        }
        Server.sysLog.info("Server stopping...");
        stopping = true;
        onlineService.setDiscard(true);

        if (isInitComplete) {
            if (persistStateOnExit) {
                try {
                    // Устанавливаем контекст выполнения
                    Execution.EXECUTION.set(new ExecutionContext(gameApp));

                    persistOnlineProfilesToDb();

                    if (serverRole == MAIN || serverRole == ALL) {
                        reactionRateService.persistToDisk();
                        ratingService.persistToDisk();
                        battleService.persistToDisk();
                        heroicMissionService.persistToDisk();
                        cheatersCheckerService.persistToDisk();
                        userRegistry.persistToDisk();
                        socialUserIdMapService.persistToDisk();
                        cheatRegisterService.persistStatictic();
                        globalChatService.persistToDisk();
                    }

                    if (serverRole == PVP || serverRole == ALL) {
                        pvpBattleTrackerService.finishAllBattles();

                        // ограничение подбора
                        blackListService.persistToDisk();

                        // держим паузу, чтобы гарантированно разошлись сообщения об окончании боя
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            Server.sysLog.warn(e.toString());
                        }
                    }
                } catch (Exception e) {
                    Server.sysLog.error(e.toString(), e);
                }
            }
        } else {
            Server.sysLog.warn("server initialized not complete");
        }

        Server.sysLog.info("Server stopped.");
        Server.sysLog.info("");
        Server.sysLog.info("###########################################################################################");
        Server.sysLog.info("");

        running = false;
    }

    private void persistOnlineProfilesToDb() {
        int mainSessions = 0;
        int achieveSessions = 0;
        int clanSessions = 0;
        for (Session session : gameApp.getSessions()) {
            Object user = session.getUser();
            if (user instanceof UserProfile) {
                final UserProfile userProfile = (UserProfile) user;
                if (userProfile.getBattleState() == BattleState.SIMPLE) {
                    userProfile.setBattlesCount(userProfile.getBattlesCount() + 1);
                    profileEventsService.fireProfileEventAsync(SHUTDOWN_SERVER, userProfile,
                            Param.battles, 1
                    );
                }
                profileService.updateSync(userProfile);

                mainSessions++;
            } else if (user instanceof ProfileAchievements) {
                achieveController.onDisconnect((ProfileAchievements) user);
                achieveSessions++;
            } else if (user instanceof ClanMember) {
                clanService.onLogout((ClanMember) user, false);
                clanSessions++;
            }
            session.close();

            int sessions = mainSessions + achieveSessions + clanSessions;
            if (sessions > 0 && sessions % 1000 == 0) {
                String message = String.format("saved sessions: main=%s; achieve=%s; clan=%s ...", mainSessions, achieveSessions, clanSessions);
                Server.sysLog.info(message);
            }
        }
        String message = String.format("saved sessions total: %s", mainSessions);
        Server.sysLog.info(message);
    }

    private void initDialyService() throws ParseException {
        dailyScheduleService.setTimeOfRun(dailyScheduleServiceRunTime);
        dailyScheduleService.init();
    }

    private void loadBanList() {
        List<BanEntity> banEntities = daoService.getBanDao().getActualBanList();
        Map<Long, BanItem> banList = new HashMap<Long, BanItem>();
        for (BanEntity banEntity : banEntities) {
            Long banKey = banEntity.getProfileId();
            // дополнительная проверка на случай если у игрока больше одного действующего бана
            if (!banList.containsKey(banKey)) {
                banList.put(banKey, new BanItem(banEntity));
            } else {
                BanItem banItem = banList.get(banKey);
                if (banItem.getEndDate() == null) {
                    // вечный бан - остальные игнориуются
                    continue;
                } else if (banEntity.getEndDate() == null || banEntity.getEndDate().getTime() > banItem.getEndDate()) {
                    // затираем прежний бан
                    banList.put(banKey, new BanItem(banEntity));
                }
            }
        }
        banService.init(banList);
    }

    private void loadAdmins() {
        List<AdminProfileEntity> entities = daoService.getAdminProfileDao().getAllList();
        if (entities.size() == 0) {
            entities = adminProfileCreator.createAdmins();
        }
        for (AdminProfileEntity adminProfileEntity : entities) {
            AdminProfile adminProfile = new AdminProfile(adminProfileEntity);
            //кешируем админов по логину
            permanentCache.put(AdminProfile.class, adminProfile.getLogin(), adminProfile);
        }
    }

    public IAppServer getPvpServerAddress() {
        return new AppServerAddress("main", pvpServerAddress);
    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

    @Override
    public void stop(Runnable callback) {
        this.stop();
        callback.run();
    }

    @Override
    public void start() {
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    public boolean isStopping() {
        return stopping;
    }

}
