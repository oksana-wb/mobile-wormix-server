package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.client.DropReasonEnum;
import com.pragmatix.pvp.messages.battle.client.PvpDropPlayer;
import com.pragmatix.pvp.messages.battle.server.PvpSystemMessage;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import com.pragmatix.pvp.services.battletracking.PvpBattleStateEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Обработчик выходов из боя. Игрок: сдался, истекло время реконнекта, записан в читеры
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.12.12 15:10
 */
@Component
public class UnbindHandler extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {
        BattleParticipant battleParticipant = battleBuffer.getParticipant(profile.getId());
        BattleParticipant.State originalState = battleParticipant.getState();
        long battleId = battleBuffer.getBattleId();
        PvpDropPlayer pvpDropPlayer = (PvpDropPlayer) command;

        PvpSystemMessage.TypeEnum reason = getUnbindReasonMessage(battleId, pvpDropPlayer);
        if(originalState.endBattle()) {
            log.warn(String.format("battleId=%s: попытка 'выкинуть' участника из боя, который этот бой уже покинул [%s] %s", battleId, reason, battleParticipant));
            rollbackState(battleBuffer);
            return null;
        }

        pvpService.unbindFromBattle(battleParticipant, battleBuffer, reason, getUnbindState(battleId, pvpDropPlayer), getBanType(pvpDropPlayer), getBanNote(pvpDropPlayer));

        if(originalState.droppedFromBattle()) {
            // unbind from battle player have been dropped or end tirning
            rollbackState(battleBuffer);
            return null;
        }

        int liveTeams = pvpService.countLiveTeams(battleBuffer).size();
        if(liveTeams < 2) {
            // можем завершать бой
            if(log.isDebugEnabled()) {
                log.debug("battleId={}: liveTeams count is {}, send EndBattle action ...", battleId, liveTeams);
            }
            /**
             * {@link com.pragmatix.pvp.services.battletracking.handlers.EndBattleHandler}
             */
            return PvpBattleActionEnum.EndBattle;
        } else if(battleBuffer.getTurningPvpId() == battleParticipant.getPvpUserId()) {
            // если из боя выходит активный участник, и бой может быть продолжен - форсируем передачу хода
            if(log.isDebugEnabled()) {
                log.debug("battleId={}, unbind in turning profile, send StateTimeout action ...", battleId);
            }
            /**
             * {@link com.pragmatix.pvp.services.battletracking.handlers.ForcedEndTurnHandler}
             */
            return PvpBattleActionEnum.StateTimeout;
        }
        rollbackState(battleBuffer);
        return null;
    }

    /**
     * хук: если ничего не произошло, необходимо вернуть бой в пердыдущее состояние
     * а так как данный обработчик вызывается через Proxy - то оно (это состоянии) должно находиться в поле wrappedState
     */
    private void rollbackState(BattleBuffer battleBuffer) {
        PvpBattleStateEnum wrappedState = battleBuffer.getWrappedState();
        if(wrappedState != null) {
            battleBuffer.setBattleState(wrappedState);
        } else {
            log.error("wrappedState не должно быть null");
        }
    }

    protected PvpSystemMessage.TypeEnum getUnbindReasonMessage(long battleId, PvpDropPlayer command) {
        if(command != null) {
            if(command.reason == DropReasonEnum.I_AM_CHEATER) {
                return PvpSystemMessage.TypeEnum.PlayerCheater;
            } else if(command.reason == DropReasonEnum.SURRENDER) {
                return PvpSystemMessage.TypeEnum.PlayerSurrendered;
            } else {
                log.error("battleId:{} не предусмотренная причина выхода из боя [{}]", battleId, command.reason);
                return PvpSystemMessage.TypeEnum.PlayerDroppedByReconnectionTimeout;
            }
        } else {
            return PvpSystemMessage.TypeEnum.PlayerDroppedByReconnectionTimeout;
        }
    }

    protected BattleParticipant.State getUnbindState(long battleId, PvpDropPlayer command) {
        if(command != null) {
            if(command.reason == DropReasonEnum.I_AM_CHEATER) {
                return BattleParticipant.State.cheat;
            } else if(command.reason == DropReasonEnum.SURRENDER) {
                return BattleParticipant.State.surrendered;
            } else {
                log.error("battleId:{} не предусмотренная причина выхода из боя [{}]", battleId, command.reason);
                return BattleParticipant.State.error;
            }
        } else {
            return BattleParticipant.State.disconnect;
        }
    }

    protected int getBanType(PvpDropPlayer command) {
        if (command != null) {
            return command.banType;
        } else {
            return 0;
        }
    }

    protected String getBanNote(PvpDropPlayer command) {
        if (command != null) {
            return command.banNote;
        } else {
            return "";
        }
    }

}
