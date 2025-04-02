package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.battle.client.CountedCommandI;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.server.PvpRetryCommandRequestServer;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.06.11 13:50
 */
@Component
public class RequestRetryCommandHandler extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // по достижении 30 накоппленных штрафных секунд, принудительно передаем ход
    public int turnPenaltyLimit = 30000;

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battleBuffer) {

        if(battleBuffer.getTurnPenaltyTime() >= turnPenaltyLimit){
            return PvpBattleActionEnum.StateTimeout;
        }

        long pvpUserId = profile != null ? profile.getId() : battleBuffer.getTurningPvpId();
        BattleParticipant battleParticipant = battleBuffer.getParticipant(pvpUserId);

        CountedCommandI lastCommand = battleBuffer.getLastBufferedCommand();
        short requestCommandFrom = (short) (lastCommand != null ? lastCommand.getCommandNum() + 1 : 1);
        ConcurrentNavigableMap<Short, CountedCommandI> futureCommandBuffer = battleBuffer.getFutureCommandBuffer();
        Short firstFutureCommandNum = futureCommandBuffer.ceilingKey((short) (requestCommandFrom + 1));
        short[] commandNums;
        if(firstFutureCommandNum == null) {
            // в буфере "будущих" комманд нет кандидатов
            commandNums = new short[]{requestCommandFrom};
        } else {
            List<Short> list = new ArrayList<Short>();
            for(short i = requestCommandFrom; i < futureCommandBuffer.lastKey(); i++) {
                if(futureCommandBuffer.get(i) == null) {
                    list.add(i);
                }
            }
            commandNums = new short[list.size()];
            for(int i = 0; i < commandNums.length; i++) {
                commandNums[i] = list.get(i);
            }
        }
        if(commandNums.length > 0) {
            PvpRetryCommandRequestServer retryRequestCommand = new PvpRetryCommandRequestServer(battleBuffer.getBattleId(), (short) battleBuffer.getCurrentTurn().get(), commandNums);
            pvpService.sendCommand(battleParticipant, retryRequestCommand, false, battleBuffer.getBattleId());

            log.warn("to user [{}] send {}", PvpService.formatPvpUserId(battleParticipant.getPvpUserId()), retryRequestCommand);
        } else {
            if(log.isDebugEnabled()) {
                log.debug("battleId={}, there is nothing to request", battleBuffer.getBattleId());
            }
        }
        return null;
    }

    public int getTurnPenaltyLimit() {
        return turnPenaltyLimit;
    }

    public void setTurnPenaltyLimit(int turnPenaltyLimit) {
        this.turnPenaltyLimit = turnPenaltyLimit;
    }

}
