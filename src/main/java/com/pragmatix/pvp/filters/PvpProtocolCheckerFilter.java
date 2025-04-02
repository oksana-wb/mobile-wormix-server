package com.pragmatix.pvp.filters;

import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.security.annotations.Filter;
import com.pragmatix.gameapp.security.annotations.InMessage;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.pvp.messages.battle.client.*;
import com.pragmatix.pvp.messages.battle.server.PvpSystemMessage;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.PvpBattleTrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * Фильтр для проверки входящих команд
 */
@Filter
public class PvpProtocolCheckerFilter {

    private static final Logger logger = LoggerFactory.getLogger(PvpProtocolCheckerFilter.class);

    public static final int MAX_STEP_SIZE = 30;

    @Resource
    private PvpBattleTrackerService pvpBattleTrackerService;

    private static final long minTimeBetweenChatMessages = 800;

    @InMessage(PvpActionEx.class)
    public boolean checkMessage(PvpActionEx msg, PvpUser profile) {
        if(!msg.secureResult || msg.battleId != profile.getBattleId()
                || msg.firstFrame < 0 || msg.lastFrame <= 0 || msg.lastFrame - msg.firstFrame > MAX_STEP_SIZE) {
            iAmCheater(profile, msg);
            return false;
        }
        return true;
    }

    @InMessage(PvpEndTurn.class)
    public boolean checkMessage(PvpEndTurn msg, PvpUser profile) {
        if(!msg.secureResult || msg.battleId != profile.getBattleId()) {
            iAmCheater(profile, msg);
            return false;
        }
        return true;
    }

    @InMessage(PvpDropPlayer.class)
    public boolean checkMessage(PvpDropPlayer msg, PvpUser profile) {
        byte playerNum = getPlayerNum(profile.getId(), profile.getBattleId());
        if(!msg.secureResult || msg.battleId != profile.getBattleId() || msg.playerNum < 0 || msg.playerNum != playerNum) {
            iAmCheater(profile, msg);
            return false;
        }
        return true;
    }

    @InMessage(PvpEndTurnResponse.class)
    public boolean checkMessage(PvpEndTurnResponse msg, PvpUser profile) {
        if(!msg.secureResult) {
            iAmCheater(profile, msg);
        }
        return msg.secureResult;
    }

    @InMessage(PvpRetryCommandRequestClient.class)
    public boolean checkMessage(PvpRetryCommandRequestClient msg, PvpUser profile) {
        if(!msg.secureResult) {
            iAmCheater(profile, msg);
        }
        return msg.secureResult;
    }

    @InMessage(PvpChatMessage.class)
    public boolean checkMessage(PvpChatMessage msg, PvpUser profile) {
        byte playerNum = getPlayerNum(profile.getId(), profile.getBattleId());
        if(!msg.secureResult || msg.battleId != profile.getBattleId() || msg.playerNum < 0 || msg.playerNum != playerNum) {
            iAmCheater(profile, msg);
            return false;
        }
        if(System.currentTimeMillis() - profile.getLastChatMessageTime() < minTimeBetweenChatMessages) {
            logger.warn("battleId={}, cmd [PvpChatMessage] in not valid: sent too often", profile.getBattleId());
            profile.setLastChatMessageTime(System.currentTimeMillis());
            return false;
        } else {
            profile.setLastChatMessageTime(System.currentTimeMillis());
            return true;
        }
    }

    private byte getPlayerNum(long pvpPlayerId, long battleId) {
        BattleBuffer battleBuffer = pvpBattleTrackerService.getBattle(battleId);
        if(battleBuffer != null) {
            BattleParticipant participant = battleBuffer.getParticipant(pvpPlayerId);
            if(participant != null) {
                return participant.getPlayerNum();
            }
        }
        return -1;

    }

    private void iAmCheater(PvpUser profile, PvpCommandI msg) {
        byte playerNum = getPlayerNum(profile.getId(), profile.getBattleId());
        logger.error("profile.playerNum={}: msg {} is not valid. Close connection with cheater {}", playerNum, msg, profile);

        //выходим из боя
        pvpBattleTrackerService.unbindCheaterFromBattle(profile, msg.getBattleId());

        //отправляем себе инфу о том, что нефиг читерить
        PvpSystemMessage pvpSystemMessage = new PvpSystemMessage(PvpSystemMessage.TypeEnum.PlayerCheater, playerNum, profile.getBattleId());
        Messages.toUser(pvpSystemMessage);

        Connections.closeConnectionDeferred();
    }

}
