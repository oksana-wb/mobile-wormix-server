package com.pragmatix.app.controllers;

import com.pragmatix.achieve.services.AchieveCommandService;
import com.pragmatix.app.common.BattleState;
import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.client.Login;
import com.pragmatix.app.messages.client.LoginByProfileStringId;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.services.authorize.LoginService;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.services.ClanServiceImpl;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnAuthConnection;
import com.pragmatix.gameapp.controller.annotations.OnCloseConnection;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.services.OnlineService;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.gameapp.threads.Execution;
import com.pragmatix.pvp.services.PvpService;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.CHEAT;
import static com.pragmatix.app.settings.AppParams.versionToString;

/**
 * User: denis
 * Date: 07.11.2009
 * Time: 17:47:06
 */
@Controller
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Resource
    private ProfileService profileService;

    @Resource
    private AchieveCommandService achieveService;

    @Resource
    private OnlineService onlineService;

    @Resource
    private UserRegistryI userRegistry;

    @Resource
    private BattleService battleService;

    @Resource
    private AppParams appParams;

    @Resource
    private ProfileEventsService profileEventsService;

    // таймаут для игроков с устаревшей версией игры
    private int kickoffTimeoutInMinute = 3;

    @Resource
    private CloneProfileService cloneProfileService;

    @Resource
    private LoginService loginService;

    @Resource
    private ClanServiceImpl clanService;

    /**
     * Подключение к игре.
     * Вызывается после того, как пользователь был успешно залогинен.
     *
     * @param msg     команда по которой произошла авторизация
     * @param profile аккаунт который был авторизован
     */
    @OnAuthConnection
    public void onLogin(Object msg, final UserProfile profile) {
        ILogin login = (ILogin) msg;

        String moveProfileParams = LoginService.getParam(login.getParams(), ILogin.MOVE_PROFILE);
        if (!moveProfileParams.isEmpty()) {
            String[] ss = moveProfileParams.split("_");
            if (ss.length == 3) {
                try {
                    SocialServiceEnum sourceServer = SocialServiceEnum.valueOf(Integer.valueOf(ss[0]));
                    long sourceProfileId = Long.valueOf(ss[1]);
                    String secureToken = ss[2];
                    boolean cloneResult = cloneProfileService.cloneProfile(profile, sourceServer, sourceProfileId, true, secureToken);
                    if (cloneResult)
                        return;
                } catch (Exception e) {
                    log.error("invalid login param [moveProfile='" + moveProfileParams + "']: " + e.toString());
                }
            } else {
                log.error("invalid login param [moveProfile='" + moveProfileParams + "']");
            }
        }

        //меняем счетчик онлайна
        onlineService.increment();


        // говорим, что мы в онлайне 
        profile.setOnline(true);
        Optional.ofNullable(clanService.getClanMember(profile.getId().intValue())).ifPresent(clanMember -> clanMember.setOnline(true));

        try {
            String locale = LoginService.getParam(login.getParams(), ILogin.LOCALE);
            if (!locale.isEmpty()) {
                profile.setLocale(com.pragmatix.app.common.Locale.valueOf(locale));
            }
        } catch (Exception e) {
            log.error(String.format("%s: не корректное значение локали! params:%s %s ", profile, Arrays.toString(login.getParams()), e.toString()));
        }

        // обновляем уровень игрока в глобальном кеше уровней
        // и сбрасывам признак того что игрок долго не заходил в игру
        userRegistry.updateLevelAndSetAbandondedFlag(profile, false);

        // сохраняем версию флешь плеера
        String flashVersion = LoginService.getParam(login.getParams(), ILogin.FLASH_VERSION_PARAM_NAME);
        if (!flashVersion.isEmpty()) {
            try {
                Session session = Sessions.get(profile);
                if (session != null) {
                    session.getStore().put(UserProfile.FLASH_VERSION, Short.valueOf(flashVersion));
                }
            } catch (NumberFormatException e) {
            }
        }

        if (profile.version < appParams.getVersion()) {
            Execution.EXECUTION.get().scheduleTimeout(TimeUnit.MINUTES.toMillis(kickoffTimeoutInMinute), () -> {
                if (profile.isOnline() && profile.version < appParams.getVersion()) {
                    Session session = Sessions.get(profile);
                    if (session != null) {
                        log.error("[{}] версия сервера '{}', версия клиента '{}'. Закрываем соединение с клиентом с устаревшей версией.",
                                profile, versionToString(appParams.getVersion()), versionToString(profile.version));
                        session.close();
                        profileEventsService.fireProfileEventAsync(CHEAT, profile,
                                Param.eventType, "oldVersion",
                                "serverVersion", versionToString(appParams.getVersion()),
                                "clientVersion", versionToString(profile.version),
                                "loginTime", profile.getLastLoginTime()
                        );
                    }
                }
            });
        }
    }

    /**
     * Обработка обрыва соединения - выход игры.
     *
     * @param profile аккаунт который был отсоединён
     */
    @OnCloseConnection
    public void onDisconnect(UserProfile profile) {
        // говорим, что теперь мы не в онлайне
        profile.setOnline(false);
        Optional.ofNullable(clanService.getClanMember(profile.getId().intValue())).ifPresent(clanMember -> clanMember.setOnline(false));

        // меняем счетчик онлайна
        onlineService.decrement();

        battleService.onDisconnect(profile);

        profile.setOrderedFriends(ArrayUtils.EMPTY_INT_ARRAY);
        profile.setLogoutTime(AppUtils.currentTimeSeconds());

        // обнуляем спец предложение
        profile.specialDealItemId = 0;
        profile.specialDealRubyPrice = 0;

        profileService.updateSync(profile);
        achieveService.findAndUpdateAchievements(profileService.getProfileAchieveId(profile.getId()));

        if (loginService.isLogLoginLogoutEvents()) {
            long sessionTime = System.currentTimeMillis() - (profile.getLastLoginTime() != null ? profile.getLastLoginTime().getTime() : System.currentTimeMillis());
            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.LOGOUT, profile,
                    "state", profile.getBattleState() == BattleState.IN_BATTLE_PVP ? profile.pvpBattleType.name() : profile.getBattleState().name(),
                    "sessionTime", PvpService.formatTime(sessionTime)
            );
        }

        ClanMember user = clanService.getClanMember((short) 0, profile.getId().intValue());
        if (user != null) {
            user.setOnline(false);
            clanService.onLogout(user, true);
        }

        if (log.isInfoEnabled()) {
            long sessionTime = System.currentTimeMillis() - (profile.getLastLoginTime() != null ? profile.getLastLoginTime().getTime() : System.currentTimeMillis());
            log.info("disconnected. Logged in {} Session time {}", AppUtils.formatDate(profile.getLastLoginTime()), PvpService.formatTime(sessionTime));
        }
    }

    /**
     * В авторизованной сессии клиент повторно посылает команду Login
     */
    @OnMessage(value = Login.class)
    public void onLogin(Login login, UserProfile profile) throws Exception {
        // do nothing
    }

    /**
     * В авторизованной сессии клиент повторно посылает команду Login
     */
    @OnMessage(value = LoginByProfileStringId.class)
    public void onLoginByProfileStringId(LoginByProfileStringId login, UserProfile profile) throws Exception {
        // do nothing
    }

}
