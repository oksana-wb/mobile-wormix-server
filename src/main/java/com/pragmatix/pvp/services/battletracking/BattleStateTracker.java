package com.pragmatix.pvp.services.battletracking;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.PvpUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.11.12 15:57
 */
public class BattleStateTracker implements BattleStateTrackerI {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final int MAX_DEEP = 4;

    private Map<PvpBattleStateEnum, Map<PvpBattleActionEnum, BattleStateTrackerFactory.CellItem>> stateMachineRules = new ConcurrentHashMap<PvpBattleStateEnum, Map<PvpBattleActionEnum, BattleStateTrackerFactory.CellItem>>();

    private CommandTranslator commandTranslator;

    private PvpBattleStateEnum initState;

    public BattleStateTracker(Map<PvpBattleStateEnum, Map<PvpBattleActionEnum, BattleStateTrackerFactory.CellItem>> stateMachineRules, CommandTranslator commandTranslator, PvpBattleStateEnum initState) {
        this.stateMachineRules = stateMachineRules;
        this.commandTranslator = commandTranslator;
        this.initState = initState;
    }

    @Override
    public void handleAction(PvpBattleActionEnum action, BattleBuffer battleBuffer) {
        handleEvent(null, action, battleBuffer);
    }

    @Override
    public PvpBattleStateEnum getInitState() {
        return initState;
    }

    @Override
    public void handleEvent(PvpUser user, Object event, BattleBuffer battleBuffer) {
        handleEvent(user, event, battleBuffer, MAX_DEEP, null);
    }

    private void handleEvent(PvpUser user, Object event, BattleBuffer battleBuffer, int deep, PvpCommandI command) {
        PvpBattleActionEnum nextAction = null;
        PvpBattleStateEnum fromState = battleBuffer.getBattleState();
        try {
            if(fromState.isSynch()) {
                synchronized (battleBuffer) {
                    if(event instanceof PvpCommandI) {
                        nextAction = handleCommand(user, (PvpCommandI) event, battleBuffer);
                    } else {
                        nextAction = handleAction(user, command, (PvpBattleActionEnum) event, battleBuffer);
                    }
                }
            } else {
                if(event instanceof PvpCommandI) {
                    nextAction = handleCommand(user, (PvpCommandI) event, battleBuffer);
                } else {
                    nextAction = handleAction(user, command, (PvpBattleActionEnum) event, battleBuffer);
                }
            }

            // hadler может вернуть action которую нужно снова обработать
            // такой хук можно сдалать deep раз
            if(nextAction != null && (deep - 1) > 0) {
                PvpCommandI proxyCommand = event instanceof PvpCommandI ? (PvpCommandI) event : command;
                handleEvent(user, nextAction, battleBuffer, deep - 1, proxyCommand);
            }
        } catch (Exception e) {
            log.error("battleId=" + battleBuffer.getBattleId() + ": " + e.toString(), e);
            // возникла ошибка, пытаемся аварийно завершить бой
            handleAction(null, null, PvpBattleActionEnum.Desync, battleBuffer);
        }
    }

    private PvpBattleActionEnum handleCommand(PvpUser user, PvpCommandI command, BattleBuffer battleBuffer) {
        PvpBattleActionEnum action = commandTranslator.translateCommand(command, user, battleBuffer);
        // не все входящие комманды транслируются в Action. Например PvpEndTurnResponse ожидается от всех "врагов" и только тогда транслируется
        if(action != null) {
            return handleAction(user, command, action, battleBuffer);
        }
        return null;
    }

    private PvpBattleActionEnum handleAction(PvpUser user, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {
        PvpBattleStateEnum fromState = battleBuffer.getBattleState();
        if(log.isDebugEnabled()) {
            log.debug(String.format("#Start# %s (%s) %s", fromState, action, battleBuffer.dumpBattle()));
        }
        BattleStateTrackerFactory.CellItem cellItem = stateMachineRules.get(fromState).get(action);
        PvpBattleStateEnum toState = cellItem.state;
        if(toState != null) {
            // если toState==null значит след. состояние будет установлено в обработчике
            battleBuffer.setBattleState(toState);
        }
        PvpBattleActionEnum nextAction = cellItem.handler.handle(user, command, action, battleBuffer);
        // состояние может быть изменено в обработчике
        // пока это только PvpBattleStateEnum.WaitEndBattleConfirm - завершение боя и в UnbindHandler
        PvpBattleStateEnum deFactoState = battleBuffer.getBattleState();
        if(toState != deFactoState) {
            if(log.isTraceEnabled()) {
                log.trace("battleId={}: toState was {}", battleBuffer.getBattleId(), toState);
            }
            toState = deFactoState;
        }
        battleBuffer.setLastActivityTime(System.currentTimeMillis());
        if(fromState != toState) {
            // учет того, что состояние Synchronized используется для "обертки" других состояний
            if(toState == PvpBattleStateEnum.Synchronized && nextAction != null) {
                battleBuffer.setWrappedState(fromState);
            } else if(fromState == PvpBattleStateEnum.Synchronized && battleBuffer.getWrappedState() != null) {
                if(battleBuffer.getWrappedState() != toState) {
                    battleBuffer.setLastChangeStateTime(System.currentTimeMillis());
                }
                battleBuffer.setWrappedState(null);
            } else {
                battleBuffer.setLastChangeStateTime(System.currentTimeMillis());
            }
        }
        if(log.isDebugEnabled()) {
            log.debug(String.format("#End# %s -(%s)-> %s [%s] %s", fromState, action, toState, command, battleBuffer.dumpBattle()));
        }
        return nextAction;
    }

}
