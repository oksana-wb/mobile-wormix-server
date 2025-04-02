package com.pragmatix.pvp.controllers;

import com.pragmatix.app.common.BattleState;
import com.pragmatix.app.common.Connection;
import com.pragmatix.app.settings.BossBattleSettings;
import com.pragmatix.app.settings.SimpleBattleSettings;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnAuthConnection;
import com.pragmatix.gameapp.controller.annotations.OnCloseConnection;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.intercom.messages.GetProfileRequest;
import com.pragmatix.pvp.messages.PvpProfileStructure;
import com.pragmatix.pvp.messages.handshake.client.CreateBattleRequest;
import com.pragmatix.pvp.messages.handshake.client.JoinToBattle;
import com.pragmatix.pvp.messages.handshake.client.ReconnectToBattle;
import com.pragmatix.pvp.messages.handshake.server.BattleCreationFailure;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.BattleFactory;
import com.pragmatix.pvp.services.ExtraBattlesTimetableService;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.battletracking.PvpBattleTrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.*;

import static com.pragmatix.pvp.BattleWager.*;

/**
 * Created by IntelliJ IDEA.
 * User: denver
 * Date: 23.02.2010
 * Time: 2:37:38
 */
@Controller
public class PvpLoginController {

    private static final Logger log = LoggerFactory.getLogger(PvpLoginController.class);

    @Resource
    private PvpBattleTrackerService pvpBattleTrackerService;

    @Resource
    private PvpService pvpService;

    @Resource
    private BattleFactory battleFactory;

    @Resource
    private ExtraBattlesTimetableService extraBattlesTimetableService;

    @Value("#{battleAwardSettings.awardSettingsMap}")
    private Map<Short, SimpleBattleSettings> awardSettingsMap;

    /**
     * Подключение к игре.
     * Вызывается после того, как пользователь был успешно залогинен.
     *
     * @param command команда по которой произошла авторизация
     * @param user    аккаунт который был авторизован
     */
    @OnAuthConnection(connections = {Connection.PVP})
    public void onLogin(Object command, final PvpUser user) {
        user.setOnline(true);

        //если авторизовались по команде CreateBattleRequest
        if(command instanceof CreateBattleRequest) {
            if(log.isDebugEnabled()){
                log.debug("message in << {}", command);
            }
            CreateBattleRequest request = (CreateBattleRequest) command;

            // заполняем
            if(request.isSingleBossBattle()) {
                SimpleBattleSettings battleSettings = awardSettingsMap.get(request.getMissionId());
                if(battleSettings == null) {
                    log.error("AwardSettings not found for missionId={} in PvE_FRIEND battle", request.getMissionId());
                } else if(!battleSettings.isNewBossBattle()) {
                    log.error("AwardSettings for missionId={} not match for NewBossBattle {}", request.getMissionId(), battleSettings);
                } else {
                    request.missionTeamSize = ((BossBattleSettings) battleSettings).getMissionTeamSize();
                }
            } else if(request.isSuperBossBattle()) {
                // если боссов больше одного, размер команд равен их кол-ву
                request.missionTeamSize = (byte) request.missionIds.length;
            } else if(request.wager.questId == 2) {
                request.missionTeamSize = (byte) 1;
            }

            if(!validateCreateBattleRequest(request)) {
                log.error("команда на создание боя не прошла валидацию " + request);
                Connections.closeConnectionDeferred();
                return;
            }

            user.setCreateBattleRequest(request);
            //создаем бой
            BattleBuffer battle = battleFactory.createBattle(user, request.wager);
            user.setBattleId(battle.getBattleId());

            //запрашиваем профили участников
            for(BattleParticipant participant : battle.getParticipants()) {
                if(participant.inState(BattleParticipant.State.needProfile)) {
                    GetProfileRequest message = new GetProfileRequest(participant.getProfileId(), participant.getSocialNetId(), battle.getBattleId(), battle.getWager())
                            .ifSuccessSet(BattleState.WAIT_START_BATTLE);
                    message.battleCreatorPvpId = user.getId();
                    message.hasLevel(request.wager.getMinLevel());
                    if(request.isSingleBossBattle()) {
                        message.canGotoMission(request.getMissionId());
                    } else if(request.isSuperBossBattle()) {
                        message.canGotoHeroicMission(request.missionIds, request.mapId);
                    }
                    Messages.toServer(message, participant.getMainServer(), false);
                }
            }
        } else if(command instanceof JoinToBattle) {
            if(log.isDebugEnabled()){
                log.debug("message in << {}", command);
            }
            JoinToBattle request = (JoinToBattle) command;

            user.setBattleId(((JoinToBattle) command).battleId);

            BattleBuffer battle = pvpBattleTrackerService.getBattle(request.battleId);
            if(battle != null && !battle.isFinished()) {
                // заполняем инфо от соц. сети
                BattleParticipant participant = battle.getParticipant(user.getId());
                PvpProfileStructure profileStructure = participant.getPvpProfileStructure();
                profileStructure.profileStringId = request.profileStringId;
                profileStructure.profileName = request.profileName;
                /**
                 * {@link com.pragmatix.pvp.services.battletracking.translators.JoinToBattleTranslator}
                 */
                battle.handleEvent(user, request);
            } else {
                BattleCreationFailure battleCreationFailure = new BattleCreationFailure();
                battleCreationFailure.participant = user.getProfileId();
                battleCreationFailure.socialNetId = user.getSocialId();
                battleCreationFailure.battleId = request.getBattleId();
                battleCreationFailure.callToBattleResult = BattleCreationFailure.CallToBattleResultEnum.CANCELED;
                pvpService.sendToUser(battleCreationFailure, user);
            }
        } else if(command instanceof ReconnectToBattle) {
            ReconnectToBattle reconnectToBattle = (ReconnectToBattle) command;
            pvpBattleTrackerService.reconnectToBattle(user, reconnectToBattle.battleId, reconnectToBattle.turnNum, reconnectToBattle.lastCommandNum);
        }
    }

    private boolean validateCreateBattleRequest(CreateBattleRequest request) {
        return extraBattlesTimetableService.validateBattle(request.wager);
    }

    /**
     * Обработка обрыва соединения
     *
     * @param user аккаунт который был отсоединён
     */
    @OnCloseConnection(connections = {Connection.PVP})
    public void onDisconnect(PvpUser user) {
        user.disconnectTime = System.currentTimeMillis();
        if(user.getBattleId() > 0) {
            user.setOnline(false);
            pvpBattleTrackerService.disconnectFromBattle(user);
        }
    }

}
