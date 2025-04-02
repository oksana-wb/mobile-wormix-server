package com.pragmatix.pvp.controllers;

import com.pragmatix.app.common.Connection;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.pvp.messages.battle.client.*;
import com.pragmatix.pvp.messages.handshake.client.CancelBattle;
import com.pragmatix.pvp.messages.handshake.client.ReadyForBattle;
import com.pragmatix.pvp.messages.handshake.client.WidenSearch;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleTrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * Контроллер для pvp команд
 * User: denver
 * Date: 23.02.2010
 * Time: 1:13:15
 */
@Controller
public class PvpController {

    private static final Logger log = LoggerFactory.getLogger(PvpController.class);

    @Resource
    private PvpBattleTrackerService pvpBattleTrackerService;

    @OnMessage(value = ReadyForBattle.class, connections = {Connection.PVP})
    public void onReadyForBattle(ReadyForBattle msg, PvpUser profile) {
        BattleBuffer battle = pvpBattleTrackerService.getBattle(msg.getBattleId());
        if(battle != null) {
            /**
             * {@link com.pragmatix.pvp.services.battletracking.translators.ReadyForBattleTranslator}
             */
            battle.handleEvent(profile, msg);
        }
    }

    @OnMessage(value = CancelBattle.class, connections = {Connection.PVP})
    public void onCancelBattle(CancelBattle msg, PvpUser profile) {
        long battleId = profile.getBattleId();
        BattleBuffer battle = pvpBattleTrackerService.getBattle(battleId);
        if(battle != null) {
            /**
             * {@link com.pragmatix.pvp.services.battletracking.handlers.CancelHandler}
             */
            battle.handleEvent(profile, msg);
        }
    }

    @OnMessage(value = WidenSearch.class, connections = {Connection.PVP})
    public void onWidenSearch(WidenSearch msg, PvpUser profile) {
        long battleId = profile.getBattleId();
        BattleBuffer battle = pvpBattleTrackerService.getBattle(battleId);
        if(battle != null) {
            /** WidenSearch -> WidenSearch
             *
             * {@link com.pragmatix.pvp.services.battletracking.handlers.WidenSearchHandler}
             */
            battle.handleEvent(profile, msg);
        }
    }

    @OnMessage(value = PvpActionEx.class, connections = {Connection.PVP})
    public void onPvpExCommand(PvpActionEx msg, PvpUser profile) {
        if(pvpBattleTrackerService.checkCountedCommand(msg, profile)) {
            /**
             * {@link com.pragmatix.pvp.services.battletracking.handlers.ValidCommandHandler}
             */
            pvpBattleTrackerService.handleCommand(profile, msg);

            BattleBuffer battleBuffer = pvpBattleTrackerService.getBattle(msg.getBattleId());
            if(battleBuffer != null) {
                // обрабатываем последующие команды полученные ранее (если они есть)
                CountedCommandI futureCommand = battleBuffer.getFutureCommandBuffer().remove((short) (battleBuffer.getCurrentCommandNum().get() + 1));
                while (futureCommand != null) {
                    if(log.isDebugEnabled()) {
                        log.debug("battleId={}, Handle future command: {}", battleBuffer.getBattleId(), futureCommand);
                    }
                    /**
                     * {@link com.pragmatix.pvp.services.battletracking.handlers.ValidCommandHandler}
                     */
                    pvpBattleTrackerService.handleCommand(profile, futureCommand);
                    futureCommand = battleBuffer.getFutureCommandBuffer().remove((short) (battleBuffer.getCurrentCommandNum().get() + 1));
                }
            }
        }
    }

    @OnMessage(value = PvpEndTurn.class, connections = {Connection.PVP})
    public void onPvpEndTurn(PvpEndTurn msg, PvpUser profile) {
        if(pvpBattleTrackerService.checkCountedCommand(msg, profile)) {
            /**
             * {@link com.pragmatix.pvp.services.battletracking.handlers.EndTurnHandler}
             */
            pvpBattleTrackerService.handleCommand(profile, msg);
        }
    }

    @OnMessage(value = PvpDropPlayer.class, connections = {Connection.PVP})
    public void onPvpExitFromBattle(PvpDropPlayer msg, PvpUser profile) {
        /**
         *  {@link com.pragmatix.pvp.services.battletracking.translators.PvpDropPlayerTranslator}
         */
        pvpBattleTrackerService.handleCommand(profile, msg);
    }

    @OnMessage(value = PvpEndTurnResponse.class, connections = {Connection.PVP})
    public void onPvpEndTurnResponse(PvpEndTurnResponse msg, PvpUser profile) {
        /**
         *  {@link com.pragmatix.pvp.services.battletracking.translators.PvpEndTurnResponseTranslator}
         */
        pvpBattleTrackerService.handleCommand(profile, msg, true);
    }

    @OnMessage(value = PvpChatMessage.class, connections = {Connection.PVP})
    public void onPvpChatMessage(PvpChatMessage msg, PvpUser profile) {
        pvpBattleTrackerService.dispatchChatMessage(msg, profile);
    }

    @OnMessage(value = PvpRetryCommandRequestClient.class, connections = {Connection.PVP})
    public void onPvpRetryCommandRequestClient(PvpRetryCommandRequestClient msg, PvpUser profile) {
        pvpBattleTrackerService.retryCommandsByRequest(profile, msg);
    }

}
