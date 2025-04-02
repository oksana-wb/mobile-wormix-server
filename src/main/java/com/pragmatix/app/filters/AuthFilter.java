package com.pragmatix.app.filters;

import com.pragmatix.app.common.BanType;
import com.pragmatix.app.common.LoginErrorEnum;
import com.pragmatix.app.common.WhiteList;
import com.pragmatix.app.init.controller.InitController;
import com.pragmatix.app.messages.client.CrashLog;
import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.client.Login;
import com.pragmatix.app.messages.client.LoginByProfileStringId;
import com.pragmatix.app.messages.server.LoginError;
import com.pragmatix.app.messages.server.ShowSystemMessage;
import com.pragmatix.app.messages.server.UserIsBanned;
import com.pragmatix.app.model.BanItem;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.BanService;
import com.pragmatix.app.services.CheatersCheckerService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.authorize.LoginService;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.clanserver.services.ClanSeasonService;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.IGameApp;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.secure.SecuredLogin;
import com.pragmatix.gameapp.security.annotations.Authenticate;
import com.pragmatix.gameapp.security.annotations.Filter;
import com.pragmatix.gameapp.services.OnlineService;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.gameapp.sessions.NettyConnectionImpl;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.server.Server;
import com.pragmatix.sessions.ISessionContainer;
import com.pragmatix.sessions.IUser;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.vavr.Tuple2;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.pragmatix.gameapp.messages.Messages.toUser;


/**
 * класс обрабатывает запросы для
 * авторизации игроков на сервере
 * <p/>
 * User: denis
 * Date: 07.11.2009
 * Time: 18:49:19
 */
@Filter
@Controller
public class AuthFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    @Value("${AuthFilter.loginsLimit:20}")
    public int loginsLimit = 20;

    @Resource
    private ProfileService profileService;

    @Resource
    private SocialService socialService;

    @Resource
    private OnlineService onlineService;

    @Resource
    private InitController initController;

    @Resource
    private BanService banService;

    @Resource
    private IGameApp gameApp;

    @Resource
    private LoginService loginService;

    @Resource
    private ClanSeasonService clanSeasonService;

    @Resource
    private CheatersCheckerService cheatersCheckerService;

    @Value("${connection.main.allowAlreadyInGame:true}")
    private boolean allowAlreadyInGame = true;

    @Value("${connection.main.preventOldClients:true}")
    private boolean preventOldClients = true;

    @Resource
    private AppParams appParams;

    private AtomicInteger loginsInSecond = new AtomicInteger(0);
    private AtomicInteger rejectedLogins = new AtomicInteger(0);
    private volatile long intervalStart = System.currentTimeMillis();

    @Resource
    private WhiteList whiteList;

    @Authenticate(TextWebSocketFrame.class)
    public ISessionContainer authenticate(TextWebSocketFrame msg) {
        log.info("msg in << TextWebSocketFrame(text='" + msg.text()+"')");
        ((NettyConnectionImpl) Connections.get()).getChannel().writeAndFlush(new TextWebSocketFrame("echo >> " + msg.text()));
        return null;
    }

    /**
     * Аутентификация для Контакта
     *
     * @param msg команда Login от клиента Контакта для авторизации на сервере
     * @return инстанс класса ISessionContainer если авторизовать удалось либо null
     */
    @Authenticate(Login.class)
    public ISessionContainer authenticate(Login msg) {
        if(log.isDebugEnabled()) {
            log.debug("[{}] {} message in << {}", msg.id, Connections.get().getIP(), msg);
        }
        // валидируем команду
        if(validateLogin(msg)) {
            return authenticate(msg, msg.id, msg.ids, msg.referrerId);
        } else {
            return null;
        }
    }

    /**
     * Аутентификация для Прочих сетей
     *
     * @param msg команда Login от клиента для авторизации на сервере
     * @return инстанс класса ISessionContainer если авторизовать удалось либо null
     */
    @Authenticate(LoginByProfileStringId.class)
    public ISessionContainer authenticateByStringProfileId(LoginByProfileStringId msg) {
        if(log.isDebugEnabled()) {
            log.debug("[{}] message in << {}", msg.id, msg);
        }
        // валидируем команду
        if(!validateLogin(msg)) {
            return null;
        }
        long profileId = profileService.getProfileLongId(msg.id, msg.socialNet, true);
        long referrerId = 0;
        if(msg.referrerId != null && !msg.referrerId.isEmpty()) {
            Long profileLongId = profileService.getProfileLongId(msg.referrerId, msg.socialNet, false);
            referrerId = profileLongId != null ? profileLongId : 0;
        }
        // переводим строковые id друзей в лонговые (при отсутствии НЕ создаём их)
        List<Long> friendLongIds = msg.ids.stream()
                .map(stringId -> profileService.getProfileLongId(stringId, msg.socialNet, false))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return authenticate(msg, profileId, friendLongIds, referrerId);
    }

    @Authenticate(CrashLog.class)
    public ISessionContainer onCrashLog(CrashLog msg) {
        onCrashLog(msg, null);
        return null;
    }

    @OnMessage
    public void onCrashLog(CrashLog msg, UserProfile profile) {
        try {
            Calendar cal = Calendar.getInstance();
            int month = cal.get(Calendar.MONTH) + 1;
            int day = cal.get(Calendar.DAY_OF_MONTH);
            String date = cal.get(Calendar.YEAR) + "-" + (month < 10 ? "0" + month : "" + month) + "-" + (day < 10 ? "0" + day : "" + day);
            File destDir = new File("data/crashLog/" + date);
            destDir.mkdirs();
            String fileName = String.format("%s-%s.log", msg.platform, msg.userId);
            File destFile = new File(destDir, fileName);
            FileUtils.writeByteArrayToFile(destFile, msg.log.substring(0, Math.min(msg.log.length(), 100_000)).getBytes("UTF-8"));
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    private boolean validateLogin(ILogin login) {
        if(initController.isStopping()) {
            return false;
        }
        if(!initController.isRunning()) {
            Messages.toUser(new LoginError(LoginErrorEnum.SERVER_IS_STARTING));
            return false;
        }
        if(onlineService.isDiscard() || clanSeasonService.isDiscard()) {
            toUser(new LoginError(LoginErrorEnum.PROPHYLACTIC_WORK));
            return false;
        }
        // ограничение: не более 20 логинов в секунду
        int logins = loginsInSecond.incrementAndGet();
        if(logins > loginsLimit) {
            if(System.currentTimeMillis() - intervalStart > 1000) {
                intervalStart = System.currentTimeMillis();
                loginsInSecond.set(0);
                int rejectedLoginsValue = rejectedLogins.getAndSet(0);
                if(rejectedLoginsValue > 0) {
                    Server.sysLog.info("rejected logins were [{}]", rejectedLoginsValue);
                }
            } else {
                toUser(new LoginError(LoginErrorEnum.ALREADY_IN_GAME));
                rejectedLogins.incrementAndGet();
                return false;
            }
        }
        // проверяем версию
        if(login.getVersion() < appParams.getVersion()) {
            log.warn("[{}] Версия клиента устарела [{}]", login.getId(), AppParams.versionToString(login.getVersion()));
            if(preventOldClients) {
                toUser(new LoginError(LoginErrorEnum.INCORRECT_PROTOCOL_VERSION));
                return false;
            }
        }
        // проверяем подпись Логина
        if(!profileService.validateLogin(((SecuredLogin) login).secureResult, login.getAuthKey())) {
            log.error("Login's secureResult is false: {}", login);
            return false;
        }
        // валидируем AuthKey
        boolean keyValidateResult = socialService.checkKey((byte) login.getSocialNet().getType(), login.getId(), login.getAuthKey());
        if(!profileService.validateLogin(keyValidateResult, login.getAuthKey())) {
            log.warn("Неправильный ключ для пользователя [{}]", login.getId());
            toUser(new LoginError(LoginErrorEnum.INCORRECT_KEY));
            return false;
        }
        // проверяем на Бан
        Long profileId = login.getId() instanceof String ? profileService.getProfileLongId((String) login.getId(), login.getSocialNet(), false) : (Long) login.getId();
        BanItem ban = banService.get(profileId);
        if(ban != null) {
            toUser(new UserIsBanned(ban.getBanReason(), ban.getEndDate(), profileId));
            return false;
        }

        if(!cheatersCheckerService.checkFriendsCount(profileId, login.getFriendsCount())) {
            log.error("[{}] подменил список друзей friendsCount={}", login.getId(), login.getFriendsCount());
            return false;
        }

        // задействуем белый список тестеров
        if(!whiteList.isValid(login.getSocialNet(), login.getId())) {
            log.error("[{}] Not in white list!", login.getId());
            toUser(new LoginError(LoginErrorEnum.NOT_IN_WHITE_LIST));
            return false;
        }

        return true;
    }

    public ISessionContainer authenticate(ILogin msg, Long profileId, List<Long> ids, long referrerId) {
        Tuple2<UserProfile, Boolean> profile_createFlag = profileService.getProfileOrCreate(profileId, msg.getParams());
        UserProfile profile = profile_createFlag._1;

        Connections.setOwner(profile.toString());

        String sessionKey = gameApp.getSessionService().generateSessionKey(profile);
        if(Sessions.get(profile) != null && allowAlreadyInGame) {
            toUser(new LoginError(LoginErrorEnum.ALREADY_IN_GAME));
        } else {
            if(log.isInfoEnabled()) {
                log.info("Login successful lastLoginTime {} sessionKey {}", AppUtils.formatDate(profile.getLastLoginTime()), sessionKey);
            }
            loginService.onSuccessAuthorize(sessionKey, profile, ids, referrerId, msg, profile_createFlag._2);
        }
        return new ISessionContainer() {
            @Override
            public IUser getSessionUser() {
                return profile;
            }

            @Override
            public String getSessionKey() {
                return sessionKey;
            }
        };
    }

    public void setLoginsLimit(int loginsLimit) {
        this.loginsLimit = loginsLimit;
    }

}
