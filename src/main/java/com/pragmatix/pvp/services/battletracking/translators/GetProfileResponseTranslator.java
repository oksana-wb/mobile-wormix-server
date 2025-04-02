package com.pragmatix.pvp.services.battletracking.translators;

import com.google.gson.Gson;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.intercom.messages.GetProfileResponse;
import com.pragmatix.pvp.messages.PvpProfileStructure;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.pvp.services.battletracking.PvpBattleActionEnum;
import com.pragmatix.pvp.services.matchmaking.GroupHpService;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.11.12 9:45
 * @see com.pragmatix.pvp.services.battletracking.handlers.CallHandler
 */
public class GetProfileResponseTranslator implements TranslatePvpCommandI<GetProfileResponse> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Gson gson = new Gson();

    @Override
    public PvpBattleActionEnum translateCommand(GetProfileResponse cmd, PvpUser profile, BattleBuffer battle) {
        if(battle.isFinished()) {
            /**
             * {@link com.pragmatix.pvp.services.battletracking.handlers.RollbackHandler}
             */
            return PvpBattleActionEnum.CancelBattle;
        }
        BattleParticipant participant = battle.getParticipant(cmd.getSocialNetId(), cmd.getProfileId());
        if(participant != null) {
            String profileStringId = "";
            String profileName = "";
            if(profile != null) {
                profileStringId = profile.getProfileStringId();
                profileName = profile.getProfileName();
                profile.setMinDailyRating(cmd.minDailyRating);
                profile.clanId = cmd.profileStructure.getClanId();
            }
            UserProfileStructure profileStructure = cmd.profileStructure;

            WormStructure[] activeTeamMembers = GroupHpService.trimUnitsCountIfNeed(battle.getBattleType(), battle.getWager(), profileStructure);
            PvpProfileStructure pvpProfileStructure = new PvpProfileStructure(profileStructure, profileStringId, profileName, participant.getSocialNetId(), participant.getPlayerNum(),
                    participant.getPlayerTeam(), cmd.dailyRating, cmd.backpackConf, activeTeamMembers, cmd.seasonsBestRank);
            participant.setPvpProfileStructure(pvpProfileStructure);
            participant.teamSize = pvpProfileStructure.wormsGroup().length;
            participant.setGroupLevel(GroupHpService.calculateGroupLevel(battle.getBattleType(), pvpProfileStructure));
            participant.setLevel(GroupHpService.getMasterWorm(pvpProfileStructure).level);
            participant.setDailyRating(pvpProfileStructure.dailyRating);
            participant.battlesCount = cmd.battlesCount;
            participant.setTrueSkillMean(cmd.trueSkillMean);
            participant.setTrueSkillStandardDeviation(cmd.trueSkillStandardDeviation);
            try {
                if(ArrayUtils.isNotEmpty(cmd.auxMatchParams))
                    participant.setAuxMatchParams(gson.fromJson(cmd.auxMatchParams[1], Class.forName(cmd.auxMatchParams[0])));
            } catch (ClassNotFoundException e) {
                log.error(e.toString(), e);
            }
            participant.setVersion(cmd.version);
            participant.profileRankPoints = profileStructure.rankPoints;
            participant.bestRank = profileStructure.bestRank;
            participant.setRestrictionBlocks(cmd.restrictionBlocks);
            participant.bossWinAwardToken = cmd.bossWinAwardToken;
            participant.clientAddress = cmd.clientAddress;

            Long creatorPvpId = battle.getCreator().getId();
            participant.setState(creatorPvpId.equals(PvpService.getPvpUserId(cmd)) ? BattleParticipant.State.connectedAndHasProfile : BattleParticipant.State.waitConnect);
        }

        boolean hasNulls = hasNullStructs(battle.getParticipants());
        /**
         * если все структуры получены, в случае дружеского боя будет вызван
         * {@link com.pragmatix.pvp.services.battletracking.handlers.CallHandler}
         * или в случае боя на ставку
         * {@link com.pragmatix.pvp.services.battletracking.handlers.MatchmakeHandler}
         */
        return hasNulls ? null : PvpBattleActionEnum.AllInState;
    }


    private boolean hasNullStructs(Collection<BattleParticipant> participants) {
        for(BattleParticipant battleParticipant : participants) {
            if(battleParticipant.getPvpProfileStructure() == null && !battleParticipant.isEnvParticipant()) {
                return true;
            }
        }
        return false;
    }

}
