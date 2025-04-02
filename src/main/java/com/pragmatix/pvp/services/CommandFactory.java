package com.pragmatix.pvp.services;

import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.intercom.messages.EndPvpBattleRequest;
import com.pragmatix.intercom.messages.GetProfileError;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.messages.PvpProfileStructure;
import com.pragmatix.pvp.messages.battle.server.EndPvpBattleResultStructure;
import com.pragmatix.pvp.messages.handshake.client.CreateBattleRequest;
import com.pragmatix.pvp.messages.handshake.server.BattleCreated;
import com.pragmatix.pvp.messages.handshake.server.BattleCreationFailure;
import com.pragmatix.pvp.messages.handshake.server.CallToBattle;
import com.pragmatix.pvp.messages.handshake.server.PreCalculatedPoints;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.11.12 11:11
 */
@Service
public class CommandFactory {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    CraftService craftService;

    public BattleCreated constructBattleCreated(BattleBuffer battle, BattleParticipant battleParticipant, int seed) {
        BattleCreated battleCreated = new BattleCreated();
        battleCreated.battleType = battle.getBattleType();
        battleCreated.battleId = battle.getBattleId();
        battleCreated.mapId = battle.getMapId();
        battleCreated.missionIds = battle.getMissionIds();
        battleCreated.questId = battle.getQuestId();
        battleCreated.wager = battle.getWager();
        battleCreated.reagentsForBattle = battle.getReagentsForBattle();
        battleCreated.seed = seed;
        battleCreated.participantStructs = fillParticipantsStructs(battle);

        PreCalculatedPoints winPoints = new PreCalculatedPoints(battleParticipant.preCalculatedPoints._1);
        PreCalculatedPoints defeatPoints = new PreCalculatedPoints(battleParticipant.preCalculatedPoints._2);
        PreCalculatedPoints drawPoints = new PreCalculatedPoints(battleParticipant.preCalculatedPoints._3);
        battleCreated.preCalculatedPoints = new PreCalculatedPoints[] {winPoints, defeatPoints, drawPoints};

        return battleCreated;
    }

    public CallToBattle constructCallToBattle(BattleBuffer battle, BattleParticipant battleParticipant) {
        CallToBattle callToBattle = new CallToBattle();
        callToBattle.profileId = battleParticipant.getProfileId();
        callToBattle.battleWager = battle.getWager();
        callToBattle.battleId = battle.getBattleId();
        callToBattle.mapId = battle.getMapId();
        callToBattle.missionIds = battle.getMissionIds();
        callToBattle.participantStructs = fillParticipantsStructs(battle);
        CreateBattleRequest createBattleRequest = battle.getCreateBattleRequest();
        if(createBattleRequest != null) {
            callToBattle.clientParams = createBattleRequest.clientParams;
        }
        return callToBattle;
    }

    private PvpProfileStructure[] fillParticipantsStructs(BattleBuffer battle) {
        List<BattleParticipant> participants = battle.getParticipants();
        if(battle.getBattleType() == PvpBattleType.PvE_FRIEND
                || battle.getBattleType() == PvpBattleType.PvE_PARTNER) {
            participants = new ArrayList<>(2);
            for(BattleParticipant battleParticipant : battle.getParticipants()) {
                if(!battleParticipant.isEnvParticipant()) {
                    participants.add(battleParticipant);
                }
            }
        }
        return participants.stream().map(BattleParticipant::getPvpProfileStructure).toArray(PvpProfileStructure[]::new);
    }

    //== BattleCreationFailure ==

    public BattleCreationFailure constructBattleCreationFailure(BattleBuffer battle, GetProfileError msg) {
        BattleCreationFailure.CallToBattleResultEnum error = BattleCreationFailure.CallToBattleResultEnum.SERVER_ERROR;
        if(msg.error == GetProfileError.GetProfileErrorEnum.CONNECTION_STATE_MISMATCH) {
            error = BattleCreationFailure.CallToBattleResultEnum.OFFLINE;
        } else if(msg.error == GetProfileError.GetProfileErrorEnum.BATTLE_STATE_MISMATCH) {
            error = BattleCreationFailure.CallToBattleResultEnum.BUSY;
        } else if(msg.error == GetProfileError.GetProfileErrorEnum.PROFILE_IS_BANNED) {
            error = BattleCreationFailure.CallToBattleResultEnum.BANNED;
        } else if(msg.error == GetProfileError.GetProfileErrorEnum.NO_ENOUGH_MONEY) {
            error = BattleCreationFailure.CallToBattleResultEnum.NO_ENOUGH_MONEY;
        } else if(msg.error == GetProfileError.GetProfileErrorEnum.INSUFFICIENT_LEVEL) {
            error = BattleCreationFailure.CallToBattleResultEnum.INSUFFICIENT_LEVEL;
        } else if(msg.error == GetProfileError.GetProfileErrorEnum.EXCEED_BATTLES) {
            error = BattleCreationFailure.CallToBattleResultEnum.EXCEED_BATTLES;
        } else if(msg.error == GetProfileError.GetProfileErrorEnum.MISSION_LOCKED) {
            error = BattleCreationFailure.CallToBattleResultEnum.MISSION_LOCKED;
        } else if(msg.error == GetProfileError.GetProfileErrorEnum.ARENA_IS_LOCKED) {
            error = BattleCreationFailure.CallToBattleResultEnum.ARENA_IS_LOCKED;
        } else if(msg.error == GetProfileError.GetProfileErrorEnum.TEAM_IS_SMALL) {
            error = BattleCreationFailure.CallToBattleResultEnum.TEAM_IS_SMALL;
        }

        return constructBattleCreationFailure(battle, msg.profileId, msg.socialNetId, error);
    }

    public BattleCreationFailure constructBattleCreationFailure(BattleBuffer battle, PvpUser profile, BattleCreationFailure.CallToBattleResultEnum error) {
        long profileId = 0;
        byte socialNetId = 0;
        if(profile != null) {
            profileId = profile.getProfileId();
            socialNetId = profile.getSocialNetId();
        }
        return constructBattleCreationFailure(battle, profileId, socialNetId, error);
    }

    public BattleCreationFailure constructBattleCreationFailure(BattleBuffer battle, long profileId, int socialNetId, BattleCreationFailure.CallToBattleResultEnum error) {
        BattleCreationFailure battleCreationFailure = new BattleCreationFailure();
        battleCreationFailure.participant = profileId;
        battleCreationFailure.socialNetId = (byte) socialNetId;
        battleCreationFailure.battleId = battle != null ? battle.getBattleId() : 0;
        battleCreationFailure.callToBattleResult = error;
        return battleCreationFailure;
    }

    //== EndPvpBattleRequest ==

    public EndPvpBattleRequest constructEndPvpBattleRequest(BattleBuffer battle, BattleParticipant targetParticipant) {
        return constructEndPvpBattleRequest(battle, targetParticipant, 0, "");
    }

    public EndPvpBattleRequest constructEndPvpBattleRequest(BattleBuffer battle, BattleParticipant targetParticipant, int banType, String banNote) {
        EndPvpBattleRequest request = new EndPvpBattleRequest(targetParticipant);
        request.result = targetParticipant.battleResult;
        request.participantState = targetParticipant.getState();
        request.offlineTime = targetParticipant.offlineTime;
        EndPvpBattleResultStructure battleResultStructure = targetParticipant.battleResultStructure();
        request.ratingPoints = battleResultStructure.ratingPoints;
        request.rankPoints = battleResultStructure.rankPoints;
        request.battleId = battle.getBattleId();
        request.battleType = battle.getBattleType();
        request.wager = battle.getWager();
        request.missionIds = battle.getMissionIds();
        request.mapId = battle.getMapId();
        request.banType = (short) banType;
        request.banNote = banNote;
        request.questId = battle.getQuestId();

        // передаем потраченное оружие
        List<BackpackItemStructure> items = targetParticipant.getItems();
        if(!items.isEmpty()) {
            request.items = items.toArray(new BackpackItemStructure[items.size()]);
        } else {
            request.items = new BackpackItemStructure[0];
        }

        // PvE бой - передаем все реагенты
        if(battle.isPvE()) {
            request.collectedReagents = battle.getReagentsForBattle();
        } else {
            // пары собранныых реагентов - playerNun:reagentId
            // но моём ходу реагент может получить кто нить другой
            List<Byte> reagents = battle.getReagents();
            List<Byte> collectedReagents = new ArrayList<Byte>();
            for(int i = 0; i < reagents.size(); i += 2) {
                if(reagents.get(i) == targetParticipant.getPlayerNum()) {
                    try {
                        collectedReagents.add(reagents.get(i + 1));
                    } catch (IndexOutOfBoundsException e) {
                        log.error(e.toString(), e);
                        break;
                    }
                }
            }
            // передаем собранные реагенты
            request.collectedReagents = new byte[collectedReagents.size()];
            for(int i = 0; i < collectedReagents.size(); i++) {
                request.collectedReagents[i] = collectedReagents.get(i);
            }

            Byte droppedUnits = battle.droppedUnits.get(targetParticipant.getPlayerNum());
            request.droppedUnits = droppedUnits != null ? droppedUnits : 0;

            if(ArrayUtils.isNotEmpty(battle.participantsHealthInPercent))
                request.healthInPercent = battle.participantsHealthInPercent[targetParticipant.getPlayerNum()];
        }

        request.teamSize = targetParticipant.teamSize;

        request.newTrueSkillMean = targetParticipant.getNewTrueSkillMean();
        request.newTrueSkillStandardDeviation = targetParticipant.getNewTrueSkillStandardDeviation();

        return request;
    }

}
