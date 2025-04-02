package com.pragmatix.pvp.services.battletracking;

import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.task.ExceptableRunnable;
import com.pragmatix.gameapp.threads.Execution;
import com.pragmatix.gameapp.threads.ExecutionContext;
import com.pragmatix.gameapp.threads.manager.ManagedThread;
import com.pragmatix.pvp.messages.battle.server.PvpSystemMessage;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.03.12 18:20
 */
public class PvpBattleTrackerTimeoutTask extends ManagedThread<PvpBattleTrackerService> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final AtomicInteger count = new AtomicInteger(0);

    private String name;

    /**
     * через 3 минуты в чат будет разослано предупреждение что игрок слишком долго ходит
     */
    public static final int turnWarningTimeoutInSeconds = 3 * 60;
    /**
     * через 4 минуты у игрока отберут ход
     */
    public static final int turnTimeoutInSeconds = 4 * 60;

    private ExecutionContext context;

    private ExceptableRunnable checkBattles;

    private PvpService pvpService;

    protected PvpBattleTrackerTimeoutTask(String name, PvpBattleTrackerService service, PvpService pvpService) {
        super(name + "#" + count.incrementAndGet(), service);
        this.name = name;
        this.pvpService = pvpService;
        this.context = new ExecutionContext(service.gameApp);
        checkBattles = new ExceptableRunnable() {
            @Override
            public Object run() {
                checkBattles();
                return null;
            }
        };
    }

    @Override
    public void run() {
        // Устанавливаем текущий контекст выполнения
        Execution.EXECUTION.set(context);
        try {
            // выполняем задачи если не был прерван данный поток
            // проверяем в цикле тк InterruptedException может перехватываться внутри сообщения
            while (!isInterrupted && !Thread.currentThread().isInterrupted()) {
                // Увеличиваем счетчик событий
                incrementTickCount();
                try {
                    Thread.sleep(100);
                    if(tickCount % 10 == 0) {
                        // запускаем проверку каждую секунду
                        // в секунду "работаем" 10 раз для корректной работы монитора потоков
                        context.doTask(checkBattles, null, null, false, false);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ex) {
                    log.error(ex.toString(), ex);
                }
            }
        } finally {
            //если поток был прерван то говорим об этом
            if(isInterrupted) {
                log.error("Thread [{}] was stopped with the monitor", getName());
            }
        }
    }

    private void checkBattles() {
        for(BattleBuffer battleBuffer : service.battles.values()) {
            try {
                if(battleBuffer.getStartBattleTime() > 0) {
                    int liveParticipants = 0;
                    int offlineParticipants = 0;
                    for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
                        if(!battleParticipant.isEnvParticipant() && battleParticipant.getState().canTurn()) {
                            liveParticipants++;
                            PvpUser user = pvpService.getUser(battleParticipant.getPvpUserId());
                            if(user.disconnectTime > 0) {
                                offlineParticipants++;
                            }
                        }
                    }
                    if(liveParticipants > 0) {
                        if(liveParticipants == offlineParticipants) {
                            if(!battleBuffer.paused) {
                                battleBuffer.pauseBattle();
                            } else if(System.currentTimeMillis() > battleBuffer.pauseTime + TimeUnit.MINUTES.toMillis(pvpService.maxPauseBattleInMinute)) {
                                battleBuffer.resumeBattle(0);
                                if(log.isDebugEnabled()) {
                                    log.debug("battleId={}: remove resumed battle by timeout", battleBuffer.getBattleId());
                                }
                                for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
                                    if(battleParticipant.getState().canTurn()) {
                                        /**
                                         * {@link com.pragmatix.pvp.services.battletracking.handlers.UnbindHandler}
                                         */
                                        battleBuffer.handleEvent(service.pvpService.getUser(battleParticipant.getPvpUserId()), PvpBattleActionEnum.Unbind);
                                    }
                                }
                            }
                        } else {
                            for(BattleParticipant battleParticipant : battleBuffer.getParticipants()) {
                                if(battleParticipant.getDisconnectTime() > 0 && battleParticipant.getState().canTurn() && System.currentTimeMillis() > battleParticipant.getDisconnectTime() + pvpService.reconnectTimeoutInSeconds * 1000L) {
                                    /**
                                     * {@link com.pragmatix.pvp.services.battletracking.handlers.UnbindHandler}
                                     */
                                    battleBuffer.handleEvent(service.pvpService.getUser(battleParticipant.getPvpUserId()), PvpBattleActionEnum.Unbind);
                                }
                            }
                        }
                    }
                }
                PvpBattleStateEnum state = battleBuffer.getBattleState();
                if(state == PvpBattleStateEnum.DropBattle) {
                    if(log.isDebugEnabled()) {
                        log.debug("battleId={}: drop battle", battleBuffer.getBattleId());
                    }
                    service.dropBattle(battleBuffer);
                } else if(!battleBuffer.paused) {
                    if(state.getStateTimeoutInSeconds() > 0 && System.currentTimeMillis() > battleBuffer.getLastChangeStateTime() + state.getStateTimeoutInSeconds() * 1000L) {
                        if(state == PvpBattleStateEnum.EndBattle) {
                            if(log.isDebugEnabled()) {
                                log.debug("battleId={}: remove finished battle by timeout", battleBuffer.getBattleId());
                            }
                            service.closeBattle(battleBuffer);
                        } else {
                            if(log.isDebugEnabled()) {
                                log.debug("battleId={}: timeout in [{}] state, sent timeout action for battle", battleBuffer.getBattleId(), state);
                            }
                            battleBuffer.handleAction(PvpBattleActionEnum.StateTimeout);
                        }
                    } else if(state.getIdleTimeoutInSeconds() > 0 && System.currentTimeMillis() > battleBuffer.getLastActivityTime() + state.getIdleTimeoutInSeconds() * 1000L) {
                        if(log.isDebugEnabled()) {
                            log.debug("battleId={}: long time wait command in [{}] state, sent IdleTimeout action", battleBuffer.getBattleId(), state);
                        }
                        battleBuffer.handleAction(PvpBattleActionEnum.IdleTimeout);

                    } else if(battleBuffer.getStartTurnTime() > 0 && System.currentTimeMillis() > battleBuffer.getStartTurnTime() + turnTimeoutInSeconds * 1000L) {
                        // если долго не передаёт ход (4 минуты) отбираем ход
                        if(log.isDebugEnabled()) {
                            log.debug("battleId={}: long time in turning, send StateTimeout action. turn started in {}", battleBuffer.getBattleId(), AppUtils.formatDateInSeconds((int)(battleBuffer.getStartTurnTime() / 1000L)));
                        }
                        battleBuffer.handleAction(PvpBattleActionEnum.StateTimeout);

                    } else if(battleBuffer.getStartTurnTime() > 0 && System.currentTimeMillis() > battleBuffer.getStartTurnTime() + turnWarningTimeoutInSeconds * 1000L) {
                        if(!battleBuffer.isSendLongTimeInTurningWarning()) {
                            // если долго не передаёт ход (3 минуты) рассылаем в чат предупреждение
                            PvpSystemMessage pvpSystemMessage = new PvpSystemMessage(PvpSystemMessage.TypeEnum.PlayerLongTimeInTurn, battleBuffer.getInTurn().getPlayerNum(), battleBuffer.getBattleId());
                            service.pvpService.dispatchToAll(battleBuffer, pvpSystemMessage, false);
                            battleBuffer.setSendLongTimeInTurningWarning(true);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("battleId=" + battleBuffer.getBattleId() + ": " + e.toString(), e);
            }
        }

    }

    @Override
    public ManagedThread<PvpBattleTrackerService> copy() {
        log.info("trying to clone {} ...", name);
        return new PvpBattleTrackerTimeoutTask(name, service, pvpService);
    }

}
