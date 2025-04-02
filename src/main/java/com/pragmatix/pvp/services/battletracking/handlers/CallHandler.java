package com.pragmatix.pvp.services.battletracking.handlers;

import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.task.TaskLock;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.handshake.server.CallToBattle;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import com.pragmatix.pvp.services.matchmaking.UserBattleProposal;
import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.11.12 10:26
 */
@Component
public class CallHandler extends AbstractHandler {

    @Override
    public PvpBattleActionEnum handle(PvpUser profile, PvpCommandI command, PvpBattleActionEnum action, BattleBuffer battle) {
        // профиль инициатора боя получен
        // все противники online, не в бою и значит теоритически могут принять бой
        TaskLock lock = new TaskLock();
        byte[] units = new byte[0];
        for(BattleParticipant battleParticipant : battle.getParticipants()) {
            if(battleParticipant.isEnvParticipant()){
                continue;
            }
            // для отражения характеристик участника в статистике боя
            battleParticipant.setBattleProposal(new UserBattleProposal(lock, battleParticipant, battle.getWager(), units, battle, pvpService));
            CallToBattle callToBattle = commandFactory.constructCallToBattle(battle, battleParticipant);
            if(!battleParticipant.getPvpUserId().equals(battle.getCreator().getId())) {
                Messages.toServer(callToBattle, battle.getCreator().getMainServer(), false);
            } else {
                pvpService.sendToUser(callToBattle, battle.getCreator(), battle.getBattleId());
            }
        }
        return null;
    }

}
