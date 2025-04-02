package com.pragmatix.pvp.filters;

import com.google.gson.Gson;
import com.pragmatix.app.common.Connection;
import com.pragmatix.app.model.MainServers;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.gameapp.IGameApp;
import com.pragmatix.gameapp.common.LoginErrorEnum;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.security.annotations.Authenticate;
import com.pragmatix.gameapp.security.annotations.Filter;
import com.pragmatix.gameapp.services.OnlineService;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.pvp.messages.battle.client.CountedCommandI;
import com.pragmatix.pvp.messages.handshake.client.*;
import com.pragmatix.pvp.messages.handshake.server.PvpLoginError;
import com.pragmatix.pvp.messages.handshake.server.StartPvpBattle;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.battletracking.PvpBattleStateEnum;
import com.pragmatix.pvp.services.battletracking.PvpBattleTrackerService;
import com.pragmatix.sessions.AppServerAddress;
import com.pragmatix.sessions.IAppServer;
import com.pragmatix.sessions.ISessionContainer;
import com.pragmatix.sessions.IUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.Map;


/**
 * Класс для авторизации на PVP сервере
 */
@Filter
public class PvpAuthFilter {

    private static final Logger log = LoggerFactory.getLogger(PvpAuthFilter.class);

    @Resource
    private OnlineService onlineService;

    @Resource
    private PvpService pvpService;

    @Resource
    private SocialService socialService;

    @Resource
    private PvpBattleTrackerService pvpBattleTrackerService;

    @Resource
    private IGameApp gameApp;

    @Autowired(required = false)
    private MainServers mainServers;

    @Resource
    private ProfileService profileService;

    @Authenticate(CreateBattleRequest.class)
    public ISessionContainer authenticate(CreateBattleRequest msg) {
        return authCommand(msg, false);
    }

    @Authenticate(JoinToBattle.class)
    public ISessionContainer authenticate(JoinToBattle msg) {
        return authCommand(msg, false);
    }

    @Authenticate(ReconnectToBattle.class)
    public ISessionContainer reconnect(ReconnectToBattle msg) {
        return authCommand(msg, true);
    }

    @Authenticate(RejectBattleOffer.class)
    public ISessionContainer authenticate(RejectBattleOffer msg) {
        if(profileService.validateLogin(msg.secureResult && socialService.checkKey(msg.socialNetId, msg.profileStringId, msg.authKey), msg.authKey)) {
            PvpUser pvpUser = getPvpUserOrCreate(msg);
            pvpUser.setBattleId(msg.battleId);
            BattleBuffer battle = pvpBattleTrackerService.getBattle(pvpUser.getBattleId());
            if(battle != null) {
                /**
                 * {@link com.pragmatix.pvp.services.battletracking.handlers.CancelHandler}
                 */
                battle.handleEvent(pvpUser, msg);
            }
        } else {
            log.warn("Неправильный ключ для пользователя [{}/{}]", msg.profileId, SocialServiceEnum.valueOf(msg.socialNetId));
            Messages.toUser(new PvpLoginError(LoginErrorEnum.INCORRECT_KEY));
        }
        return null;
    }

    private ISessionContainer authCommand(PvpLogin msg, boolean reconnect) {
        if(onlineService.isDiscard()) {
            Messages.toUser(new PvpLoginError(LoginErrorEnum.PROFILACTIC_WORK));
            return null;
        }
        boolean condition = msg.secureResult;
        if(profileService.validateLogin(condition, msg.authKey)) {
            PvpUser pvpUser = getPvpUserOrCreate(msg);

            Session session = Sessions.get(pvpUser);
            ISessionContainer result;
            if(session == null) {
                    result = reconnect ? authByReconnect(pvpUser, (ReconnectToBattle) msg) : authPvpUser(pvpUser);
                    if(result != null){
                        pvpUser.connectTime = System.currentTimeMillis();
                        pvpUser.disconnectTime = 0;

                        if(!reconnect && pvpUser.getBattleId() > 0){
                            pvpBattleTrackerService.unbindFromPausedBattle(pvpUser);
                        }
                    }
            } else {
                result = alreadyInGame(pvpUser);
            }
            return result;
        } else {
            log.warn("Неверный ключ авторизации! {}", msg);
            Messages.toUser(new PvpLoginError(LoginErrorEnum.INCORRECT_KEY));
        }
        return null;
    }

    private PvpUser getPvpUserOrCreate(PvpLogin msg) {
        try {
            PvpUser pvpUser = pvpService.getUser(msg.socialNetId, msg.profileId);
            if(pvpUser == null) {
                IAppServer mainServerAddress = getMainServerAddress(msg.socialNetId);
                pvpUser = new PvpUser(msg.profileId, msg.profileStringId, msg.socialNetId, mainServerAddress);
                pvpService.addUser(pvpUser);
            }
            pvpUser.setProfileName(msg.profileName);
            String profileName = msg.profileName;
            // если пришел JSON соответствия id игроков (игрока и членов его команды) и их имен
            if(msg.profileName.startsWith("{\"")) {
                try {
                    Map<String, String> map = new Gson().fromJson(msg.profileName, Map.class);
                    String key = "" + pvpUser.getProfileId();
                    if(map.containsKey(key))
                        profileName = map.get(key);
                    else {
                        log.warn("JSON map [{}] не содержит имени самого игрока!", msg.profileName);
                    }
                } catch (Exception e) {
                    log.warn("parse JSON map [" + msg.profileName + "] error: " + e.toString());
                }
            }
            pvpService.getUserIdToNameMap().put(pvpUser.getId(), profileName);
            return pvpUser;
        } catch (Exception e) {
            log.error("{}: " + e.toString(), msg);
            throw e;
        }
    }

    private IAppServer getMainServerAddress(int mainServerId) {
        String addr;
        if(mainServers == null) {
            addr = "main";
        } else if(mainServers.map.containsKey(mainServerId)) {
            addr = mainServers.map.get(mainServerId);
        } else {
            throw new IllegalArgumentException("main server having id [" + mainServerId + "] is not registered!");
        }
        return new AppServerAddress(addr);
    }

    public ISessionContainer authByReconnect(final PvpUser pvpUser, ReconnectToBattle reconnectToBattle) {
        long battleId = pvpUser.getBattleId();
        short turnNum = reconnectToBattle.turnNum;

        if(log.isDebugEnabled()) {
            log.debug("[{}] message in << {}", pvpUser, reconnectToBattle);
        }
        Connections.setOwner(pvpUser.toString());

        BattleBuffer battleBuffer = pvpBattleTrackerService.tryReConnectToBattle(pvpUser, battleId, turnNum);
        if(battleBuffer != null) {
            final String sessionKey = gameApp.getSessionService().generateSessionKey(pvpUser);

            int offlineTime = (int)((System.currentTimeMillis() - pvpUser.disconnectTime) / 1000L);
            log.info("[{}] battleId={} reconnect successful offlineTime {}", pvpUser, battleId, PvpService.formatTimeInSeconds(offlineTime));

            Messages.toUser(new StartPvpBattle(battleId, sessionKey, reconnectToBattle.playerNum));

            if(battleBuffer.inState(PvpBattleStateEnum.WaitForTurnTransfer)){
                CountedCommandI cmd = battleBuffer.getLastBufferedCommand();
                if(cmd != null && battleBuffer.getTurningPvpId() != pvpUser.getId()){
                    Messages.toUser(cmd);
                }
            }
            return new ISessionContainer() {
                @Override
                public IUser getSessionUser() {
                    return pvpUser;
                }

                @Override
                public String getSessionKey() {
                    return sessionKey;
                }
            };
        } else {
            Messages.toUser(new PvpLoginError(LoginErrorEnum.RECONNECT_TIMEOUT));
            return null;
        }
    }

    private ISessionContainer alreadyInGame(final PvpUser pvpUser) {
        // игрок уже в игре
        Messages.toUser(new PvpLoginError(LoginErrorEnum.ALREADY_IN_GAME));
        // сессия нам не понадобится, но если мы не вернем  ISessionContainer текущая сессия не закроется
        final String sessionKey = gameApp.getSessionService().generateSessionKey(pvpUser);

        return new ISessionContainer() {
            @Override
            public IUser getSessionUser() {
                return pvpUser;
            }

            @Override
            public String getSessionKey() {
                return sessionKey;
            }
        };
    }

    public ISessionContainer authPvpUser(final PvpUser pvpUser) {
        final String sessionKey = gameApp.getSessionService().generateSessionKey(pvpUser);

        return new ISessionContainer() {
            @Override
            public IUser getSessionUser() {
                return pvpUser;
            }

            @Override
            public String getSessionKey() {
                return sessionKey;
            }
        };

    }

}
