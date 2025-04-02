package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.app.common.BanType;
import com.pragmatix.app.common.CheatTypeEnum;
import com.pragmatix.app.services.CheatersCheckerService;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.client.CountedCommandI;
import com.pragmatix.pvp.messages.battle.client.DropReasonEnum;
import com.pragmatix.pvp.messages.battle.client.PvpActionEx;
import com.pragmatix.pvp.messages.battle.client.PvpActionEx.ActionCmd;
import com.pragmatix.pvp.messages.battle.client.PvpDropPlayer;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.PvpBattleLog;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.ReplayService;
import com.pragmatix.pvp.services.battletracking.BattleStateTrackerFactory;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.util.stream.LongStream.concat;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.06.11 13:50
 */
@Component
public class ValidCommandHandler extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private CheatersCheckerService cheatersCheckerService;

    @Resource
    private ReplayService replayService;

    @Value("${ValidCommandHandler.debugFailOnUnexpectedExceptions:false}")
    private boolean debugFailOnUnexpectedExceptions = false;

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI cmd, PvpBattleActionEnum action, BattleBuffer battleBuffer) {

        CountedCommandI command = (CountedCommandI) cmd;
        List<CountedCommandI> commandBuffer = battleBuffer.getCommandBuffer();
        if(command.getCommandNum() == battleBuffer.getCurrentCommandNum().get() + 1) {

            if (command instanceof PvpActionEx) {
                if ( ! battleBuffer.getParticipant(profile.getId()).isEnvParticipant() ) {
                    CheatTypeEnum cheatType = consumeClientActions((PvpActionEx) command, battleBuffer);
                    if (cheatType.mustBeBanned()) {
                        BanType banType = BanType.BAN_FOR_PVP_HACK;
                        String banNote = banType.toString() + ": " + cheatType.name();
                        log.error("battleId={} cheat {} detected on turn={}, frame={} by [{}] => BAN cheater: {}",
                                battleBuffer.getBattleId(), cheatType, command.getTurnNum(), battleBuffer.getBattleLog().map(l -> l.getCurrentTurn().cheatFrame).orElse(((PvpActionEx) command).firstFrame), PvpService.formatPvpUserId(profile.getId()), banType);
                        /**
                         * {@link com.pragmatix.pvp.services.battletracking.handlers.UnbindHandler}
                         */
                        battleBuffer.handleEvent(profile, new PvpDropPlayer(battleBuffer.getInTurn().getPlayerNum(), battleBuffer.getBattleId(), DropReasonEnum.I_AM_CHEATER,
                                                                            banType.getType(), banNote));
                        return null;
                    } else if (cheatType.mustBeDiscarded()) {
                        // игрока выкидываем, записывая ему поражение по причине чита
                        log.error("battleId={} cheat {} detected on turn={}, frame={} by [{}] => drop cheater",
                                battleBuffer.getBattleId(), cheatType, command.getTurnNum(), battleBuffer.getBattleLog().map(l -> l.getCurrentTurn().cheatFrame).orElse(((PvpActionEx) command).firstFrame), PvpService.formatPvpUserId(profile.getId()));
                        /**
                         * {@link com.pragmatix.pvp.services.battletracking.handlers.UnbindHandler}
                         */
                        battleBuffer.handleEvent(profile, new PvpDropPlayer(battleBuffer.getInTurn().getPlayerNum(), battleBuffer.getBattleId(), DropReasonEnum.I_AM_CHEATER));
                        return null;
                    }
                }

                replayService.onPvpActionEx(battleBuffer, (PvpActionEx) command);
            }

            commandBuffer.add(command);
            pvpService.dispatchToParticipants(battleBuffer.getTurningPvpId(), battleBuffer, command);
            battleBuffer.getCurrentCommandNum().incrementAndGet();

            int commandNum = battleBuffer.getCurrentCommandNum().get();
            int turnNum = battleBuffer.getCurrentTurn().get();
            // начиная со второго хода commandNum=1 у серверной команды PvpStartTurn
            if(turnNum == 1 && commandNum > 1 || turnNum > 1 && commandNum > 2) {
                long penaltyTime = System.currentTimeMillis() - battleBuffer.getLastPvpActionExTime() - BattleStateTrackerFactory.COMMANDS_INTERVAL;
                if(penaltyTime > 0) {
                    battleBuffer.incTurnPenaltyTime(penaltyTime);
                }

                if(log.isTraceEnabled()) {
                    log.trace("battleId={} turnPenaltyTime={}", battleBuffer.getBattleId(), battleBuffer.getTurnPenaltyTime());
                }
            }
            battleBuffer.setLastPvpActionExTime(System.currentTimeMillis());
        }
        return null;
    }

    /**
     * Обрабатывает клиентские action'ы, содержащиеся в {@link PvpActionEx#ids}
     *
     * @see <a href="http://gitlab.pragmatix-corp.com/wormix/client/blob/experiments/src/ru/pragmatix/wormix/serialization/client/PvpActionExBinarySerializer.as#L114-135">PvpActionExBinarySerializer.as</a> - клиентский сериализатор
     */
    public CheatTypeEnum consumeClientActions(PvpActionEx cmd, BattleBuffer battleBuffer) {
        return battleBuffer.getBattleLog().map(battleLog -> {
            // если в данном режиме лог есть - валидируем
            PvpBattleLog.Turn turn = battleLog.getCurrentTurn();
            long lastFrame = 0;
            try {
                PrimitiveIterator.OfLong it = Arrays.stream(cmd.ids).iterator();
                while (it.hasNext()) {
                    // для каждого кадра:
                    final long frame = it.nextLong();
                    lastFrame = frame;

                    long actionsCount = it.nextLong();
                    List<Tuple2<ActionCmd, long[]>> actionsPerFrame = new ArrayList<>();
                    // 1. буферизуем все экшны с этого кадра
                    for (long i = 0; i < actionsCount; i++) {
                        long actionId = it.nextLong();

                        long paramsCount = ActionCmd.paramsCount(actionId);
                        long[] params = LongStream.generate(it::nextLong)
                                                  .limit(paramsCount)
                                                  .toArray(); // paramsCount раз считать long из потока action'ов и собрать в массив

                        ActionCmd actionCmd = ActionCmd.valueOf(actionId);
                        if (actionCmd != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("battleId={} Action: {}, params: {}", battleBuffer.getBattleId(), actionCmd, Arrays.toString(params));
                            }
                            actionsPerFrame.add(new Tuple2<>(actionCmd, params));
                        } // else - не интересующая нас [пока] команда, пропускаем
                    }

                    // 2. каждый из оставшихся отправляем на обработку и валидацию
                    actionsPerFrame.forEach(item -> cheatersCheckerService.validatePvpClientAction(battleLog, frame, item._1, item._2));
                }
            } catch (Exception e) {
                if (debugFailOnUnexpectedExceptions) {
                    throw e;
                } else {
                    log.error("battleId=" + battleBuffer.getBattleId() + ": Failed to consume client actions at frame " + lastFrame + " of " + cmd, e);
                }
            }
            return turn.getReason();
        }).orElse(
            // а если нет - просто возвращаем UNCHECKED
            CheatTypeEnum.UNCHECKED
        );
    }

    /**
     * Метод, обратный consumeClientActions: запаковывает набор клиентских action'ов в команду {@link PvpActionEx}
     *
     * Преимущественно для тестов
     * @param actions список пар клиентский action'ов - (команда, параметры)
     * @return команду {@link PvpActionEx}, содержащую переданные action'ы, принятие которой сервером сейчас будет корректным (в соответствии с состоянием {@code battleBuffer})
     */
    @SafeVarargs
    public static long[] packClientActions(long frame, Tuple2<ActionCmd, long[]>... actions) {
        // Пишем в поток последовательно:
        return concat(
            LongStream.of(
                frame,          // - фрейм
                actions.length  // - количество action'ов
            ),
            Arrays.stream(actions) // - и для каждого action'а:
                  .flatMapToLong(actionAndParams -> concat(
                      LongStream.of(actionAndParams._1.getCode()), // - код action'а
                      LongStream.of(actionAndParams._2))           // - параметры action'а: от 0 до +∞
                  )
        ).toArray();
    }

    private static boolean hasAction(Collection<Tuple2<ActionCmd, long[]>> actions, Predicate<ActionCmd> predicate) {
        return actions.stream().anyMatch(actionIs(predicate));
    }

    private static OptionalInt indexOfAction(List<Tuple2<ActionCmd, long[]>> actions, Predicate<ActionCmd> predicate) {
        Predicate<Tuple2<ActionCmd, long[]>> actionMatches = actionIs(predicate);
        return IntStream.range(0, actions.size())
                        .filter(i -> actionMatches.test(actions.get(i)))
                        .findFirst();
    }

    // для удобства заворачиваем предикат на команду в предикат на {команду, параметры}
    private static Predicate<Tuple2<ActionCmd, long[]>> actionIs(Predicate<ActionCmd> predicate) {
        return item -> predicate.test(item._1);
    }
}
