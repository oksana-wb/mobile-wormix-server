package com.pragmatix.pvp.services.matchmaking;

import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.messages.PvpProfileStructure;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyConf;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.04.13 12:52
 */
public class GroupMassService {

    public static byte[] getUnitsOf(BattleBuffer battle) {
        List<BattleParticipant> participants = battle.getParticipants();
        byte[] unitsLevels = new byte[4 * participants.size()];//4-ре максимально количество юнитов у игрока
        int i = 0;
        for(BattleParticipant participant : participants) {
            PvpProfileStructure profileStructure = participant.getPvpProfileStructure();

            for(WormStructure wormStructure : profileStructure.wormsGroup()) {
                unitsLevels[i] = wormStructure.level;
                i++;
            }
        }
        if(i < unitsLevels.length) {
            unitsLevels = Arrays.copyOf(unitsLevels, i);
        }
        return unitsLevels;
    }


}
