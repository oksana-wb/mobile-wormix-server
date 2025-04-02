package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.client.PvpEndTurn;
import com.pragmatix.pvp.messages.battle.server.PvpSystemMessage;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.ReplayService;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

import static com.pragmatix.pvp.model.BattleParticipant.State.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.06.11 16:19
 */
@Component
public class EndTurnHandler extends ValidCommandHandler {

    @Resource
    private ReplayService replayService;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battle) {

        battle.setStartTurnTime(0);

        BattleParticipant inTurn = battle.getInTurn();
        PvpEndTurn endTurn = (PvpEndTurn) command;

        // игрок читерил в ходе боя
        if(endTurn.banType > 0){
            pvpService.unbindFromBattle(inTurn, battle, PvpSystemMessage.TypeEnum.PlayerCheater, cheat, endTurn.banType, endTurn.banNote);
            // не доверяем этому PvpEndTurn
            endTurn.forced = true;
        }

        // если соединение с игроком ещё активно
        if(inTurn.getState().canTurn()) {
            inTurn.setState(waitTransferTurn);
        }

        Set<Byte> liveTeams = pvpService.countLiveTeams(battle);

        battle.visitExceptTurning(participant -> {
            // ждем подтверждения об окончании хода только от тех участников которые еще в бою
            if(participant.getState().canTurn()) {
                participant.setState(waitEndTurnResponce);
            } else if(participant.getState().canAccept()) {
                participant.setState(droppedAndWaitEndTurnResponce);
            }
        });

        // корректная передача хода
        if(inTurn.inState(waitTransferTurn)) {
            liveTeams.remove(inTurn.getPlayerTeam());
        }

        replayService.onPvpEndTurn(battle, endTurn);

        // в живых не осталось никого (остальные участники отвалились по тем или иным причинам)
        if(liveTeams.size() == 0) {
            finishBattleImmediately(battle, endTurn);
            return null;
        } else if(inTurn.inState(commandTimeout) && battle.getCurrentCommandNum().get() == 1 && liveTeams.size() == 1) {
            // игрок которому передали ход, не прислал ни одной команды (для справки: PvpStartTurn имеет commandNum=1)
            finishBattleImmediately(battle, endTurn);
            return null;
        } else {
            super.handle(profile, command, action, battle);
            // в бою с боссом, если остался последний участник, окончание хода подтвердить будет не кому
            return battle.hasInStates(waitEndTurnResponce, droppedAndWaitEndTurnResponce) ? null : PvpBattleActionEnum.AllInState;
        }
    }


    /**
     * Проверяем на возможность немедленно завершить бой
     */
    protected void finishBattleImmediately(BattleBuffer battle, PvpEndTurn endTurn) {
        // применяем изменения в текущем ходе

        // удаляям из боя тех кто почил с миров во время хода
        pvpService.dropPlayesFromBattle(battle, endTurn);

        // сохраняем найденные реагенты
        pvpService.addReagents(battle, endTurn.collectedReagents);

        // сохраняем потраченное оружие
        pvpService.addItems(battle, endTurn.items);

        // добавляем команду в буфер, для дальнейшей реализации повтора боя
        battle.getCommandBuffer().add(endTurn);

        pvpService.finishBattle(battle);
    }

}
