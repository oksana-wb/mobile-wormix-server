package com.pragmatix.pvp.controllers;

import com.pragmatix.admin.messages.client.Discard;
import com.pragmatix.app.common.Connection;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.services.OnlineService;
import com.pragmatix.intercom.messages.EndPvpBattleResponse;
import com.pragmatix.intercom.messages.GetProfileError;
import com.pragmatix.intercom.messages.GetProfileResponse;
import com.pragmatix.pvp.messages.handshake.client.RejectBattleOffer;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.battletracking.PvpBattleTrackerService;
import com.pragmatix.sessions.IAppServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.05.12 12:16
 */
@Controller
public class PvpIntercomController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private PvpService service;

    @Resource
    private PvpBattleTrackerService pvpBattleTrackerService;

    @Resource
    private OnlineService onlineService;

    @OnMessage(value = GetProfileResponse.class, connections = {Connection.INTERCOM})
    public void onGetProfileResponse(GetProfileResponse msg, IAppServer mainServer) {
        if(msg.battleId > 0) {
            BattleBuffer battle = pvpBattleTrackerService.getBattle(msg.battleId);
            if(battle != null) {
                PvpUser user = service.getUser(msg.socialNetId, msg.getProfileId());
                /**
                 *  {@link com.pragmatix.pvp.services.battletracking.translators.GetProfileResponseTranslator}
                 */
                battle.handleEvent(user, msg);
            }
        } else {
            log.error("в сообщении не указан battleId " + msg);
        }
    }

    @OnMessage(value = GetProfileError.class, connections = {Connection.INTERCOM})
    public void onGetProfileError(GetProfileError msg, IAppServer mainServer) {
        if(msg.battleId > 0) {
            BattleBuffer battle = pvpBattleTrackerService.getBattle(msg.battleId);
            if(battle != null) {
                PvpUser user = service.getUser(msg.socialNetId, msg.profileId);
                /**
                 *  {@link com.pragmatix.pvp.services.battletracking.CommandTranslator#init()}
                 *  {@link com.pragmatix.pvp.services.battletracking.handlers.CancelHandler}
                 */
                battle.handleEvent(user, msg);
            }
        } else {
            log.error("в сообщении не указан battleId " + msg);
        }
    }

    /**
     * Отмена боя main сервером, от имени игрока, который уже не сможет приянять приглашение в дружеский бой
     */
    @OnMessage(value = RejectBattleOffer.class, connections = {Connection.INTERCOM})
    public void onRejectBattleOffer(RejectBattleOffer msg, IAppServer mainServer) {
        if(msg.battleId > 0) {
            BattleBuffer battle = pvpBattleTrackerService.getBattle(msg.battleId);
            if(battle != null) {
                PvpUser user = service.getUser(msg.socialNetId, msg.profileId);
                if(user != null) {
                    // зануляем структуру, чтобы при отмене боя этому игроку не пришел запрос на изменение статуса WAIT_START_BATTLE -> NOT_IN_BATTLE
                    BattleParticipant participant = battle.getParticipant(user.getId());
                    if(participant != null) {
                        participant.setPvpProfileStructure(null);
                        /**
                         *  {@link com.pragmatix.pvp.services.battletracking.CommandTranslator#init()}
                         *  {@link com.pragmatix.pvp.services.battletracking.handlers.CancelHandler}
                         */
                        battle.handleEvent(user, msg);
                    }
                }
            }
        } else {
            log.error("в сообщении не указан battleId " + msg);
        }
    }

    @OnMessage(value = EndPvpBattleResponse.class, connections = {Connection.INTERCOM})
    public void onEndPvpBattleResponse(EndPvpBattleResponse msg, IAppServer mainServer) {
        BattleBuffer battle = pvpBattleTrackerService.getBattle(msg.getBattleId());
        if(battle != null) {
            PvpUser user = service.getUser(msg.socialNetId, msg.profileId);
            /**
             * {@link com.pragmatix.pvp.services.battletracking.translators.EndPvpBattleResponseTranslator}
             */
            battle.handleEvent(user, msg);
        }
    }

    @OnMessage(value = Discard.class, connections = {Connection.INTERCOM})
    public synchronized void onDiscard(Discard msg, IAppServer mainServer) {
        log.info("receive msg: {}", msg);

        if(msg.event == Discard.DISCARD) {
            onlineService.setDiscard(true);

            pvpBattleTrackerService.finishAllBattles();
            service.initCounters();
        } else {
            onlineService.setDiscard(false);
        }
    }

}
