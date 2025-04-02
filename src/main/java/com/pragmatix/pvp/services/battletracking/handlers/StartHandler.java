package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.app.common.Connection;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.threads.Execution;
import com.pragmatix.intercom.messages.CompareAndSetBattleState;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.handshake.server.StartPvpBattle;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.pragmatix.app.common.BattleState.IN_BATTLE_PVP;
import static com.pragmatix.app.common.BattleState.WAIT_START_BATTLE;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.06.11 11:50
 */
@Component
public class StartHandler extends AbstractHandler {

    private static final Logger log = LoggerFactory.getLogger(StartHandler.class);

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {

        Execution exec = Execution.EXECUTION.get();

        for(BattleParticipant participant : battleBuffer.getParticipants()) {
            if(participant.isEnvParticipant()) {
                participant.setState(BattleParticipant.State.acceptCommand);
                continue;
            }
            Long pvpUserId = participant.getPvpUserId();
            PvpUser pvpUser = pvpService.getUser(pvpUserId);
            if(pvpUser != null) {
                Session session = Sessions.get(pvpUser);
                if(session != null) {
                    com.pragmatix.gameapp.sessions.Connection conn = session.getConnection(Connection.PVP);
                    if(conn != null) {
                        StartPvpBattle startPvpBattle = new StartPvpBattle(battleBuffer.getBattleId(), session.getKey(), participant.getPlayerNum());
                        // далее отправляем команду которая скажет, о том,
                        // что все игроки зашли и готовы к бою
                        exec.sendMessage(startPvpBattle, conn);
                        if(log.isDebugEnabled()) {
                            log.debug("battleId={}, send StartPvpBattle msg to [{}]", battleBuffer.getBattleId(), PvpService.formatPvpUserId(pvpUserId));
                        }

                        if(participant.getPlayerNum() == 0) {
                            battleBuffer.setTurningPvpId(pvpUserId);
                            participant.setState(BattleParticipant.State.sendCommand);
                        } else {
                            participant.setState(BattleParticipant.State.acceptCommand);
                        }

                        CompareAndSetBattleState compareAndSetBattleState = new CompareAndSetBattleState(participant, WAIT_START_BATTLE, IN_BATTLE_PVP, battleBuffer.getBattleId(), battleBuffer.getBattleType());
                        Messages.toServer(compareAndSetBattleState, participant.getMainServer(), false);
                    }
                }
            } else {
                log.error("battleId={}, User [{}] not found in cache!", battleBuffer.getBattleId(), PvpService.formatPvpUserId(pvpUserId));
            }
        }

        onStartBattle(battleBuffer);

        return null;
    }

    private void onStartBattle(BattleBuffer battleBuffer) {
        try {
            battleBuffer.setStartBattleTime(System.currentTimeMillis());
            battleBuffer.setStartTurnTime(System.currentTimeMillis());
            if(battleBuffer.getOnStartBattle() != null) {
                battleBuffer.getOnStartBattle().run();
            }
            battleBuffer.getBattleLog().ifPresent(battleLog ->
                battleLog.startNewTurnBy(battleBuffer.getInTurn(), battleBuffer.getCurrentTurn().get(), battleBuffer.getStartTurnTime())
            );
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        pvpService.onStartBattle(battleBuffer);
    }

}
