package com.pragmatix.app.controllers;

import com.google.gson.Gson;
import com.pragmatix.app.common.BattleState;
import com.pragmatix.app.common.Connection;
import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.domain.TrueSkillEntity;
import com.pragmatix.app.messages.server.PrivateChatMessage;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.model.BanItem;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.services.rating.RatingService;
import com.pragmatix.app.services.rating.SeasonService;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.app.settings.SimpleBattleSettings;
import com.pragmatix.arena.coliseum.ColiseumEntity;
import com.pragmatix.arena.coliseum.ColiseumService;
import com.pragmatix.arena.mercenaries.MercenariesEntity;
import com.pragmatix.arena.mercenaries.MercenariesService;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.intercom.messages.*;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.messages.handshake.client.RejectBattleOffer;
import com.pragmatix.pvp.messages.handshake.server.CallToBattle;
import com.pragmatix.pvp.services.PvpService;
import com.pragmatix.server.Server;
import com.pragmatix.sessions.IAppServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.*;

import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.START_PVP_BATTLE;
import static com.pragmatix.intercom.messages.GetProfileError.GetProfileErrorEnum.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 29.05.12 16:00
 */
@Controller
public class AppIntercomController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ProfileService profileService;

    @Resource
    private BanService banService;

    @Resource
    private BattleService battleService;

    @Value("#{battleAwardSettings.awardSettingsMap}")
    private Map<Short, SimpleBattleSettings> awardSettingsMap;

    @Resource
    private HeroicMissionService heroicMissionService;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private WeaponService weaponService;

    @Resource
    private SoftCache softCache;

    @Autowired(required = false)
    private ColiseumService coliseumService;

    @Autowired(required = false)
    private MercenariesService mercenariesService;

    @Resource
    private RestrictionService restrictionService;

    @Resource
    private AppParams appParams;

    @Resource
    private ArenaService arenaService;

    @Resource
    private RatingService ratingService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Autowired(required = false)
    private SeasonService seasonService;

    private Gson gson = new Gson();

    @OnMessage(value = GetProfileRequest.class, connections = {Connection.INTERCOM})
    public IntercomResponseI onGetProfileRequest(GetProfileRequest request, IAppServer source) {
        BanItem ban = banService.get(request.profileId);
        //проверяем не в бане ли игрок
        if(ban != null) {
            return new GetProfileError(request, PROFILE_IS_BANNED);
        }
        UserProfile profile = profileService.getUserProfile(request.profileId, false);
        if(profile != null && profile.isOnline()) {
            if(profile.version < appParams.getVersion()) {
                return new GetProfileError(request, INVALID_VERSION);
            }
            BattleWager battleWager = request.battleWager == BattleWager.WAGER_50_2x2_FRIENDS ? BattleWager.WAGER_50_2x2 : request.battleWager;
            if(arenaService.isArenaLocked(battleWager))
                return new GetProfileError(request, ARENA_IS_LOCKED);
            int dailyRating = dailyRegistry.getDailyRating(profile.getProfileId(), ratingService.wagerAggregator().apply(battleWager));
            int minDailyRating = dailyRegistry.getMinDailyRating(profile.getProfileId());

            GetProfileError.GetProfileErrorEnum errorEnum = isProfileFillTheBill(request, profile);
            if(errorEnum != null) {
                return new GetProfileError(request, errorEnum);
            }
            TrueSkillEntity trueSkill = profileService.getTrueSkillFor(profile);
            UserProfileStructure userProfileStructure = getProfileStructureForBattle(battleWager, profile);
            String[] auxMatchParams = null;
            if(battleWager == BattleWager.GLADIATOR_DUEL) {
                ColiseumEntity entity = coliseumService.coliseumEntity(profile);
                byte[] status = {entity.win, entity.draw, entity.defeat};
                auxMatchParams = new String[]{
                        status.getClass().getName(),
                        gson.toJson(status)
                };
            } else if(battleWager == BattleWager.MERCENARIES_DUEL) {
                MercenariesEntity entity = mercenariesService.mercenariesEntity(profile);
                int[] status = {entity.total_win, entity.total_draw, entity.total_defeat};
                auxMatchParams = new String[]{
                        status.getClass().getName(),
                        gson.toJson(status)
                };
            }
            short restrictionBlocks = RestrictionService.aggregateBlocks(restrictionService.getRestrictions(profile.getId()));

            GetProfileResponse getProfileResponse = new GetProfileResponse(request, userProfileStructure, trueSkill, dailyRating, minDailyRating, weaponService.getActiveBackpackConf(profile),
                    auxMatchParams, profile.version, restrictionBlocks, dailyRegistry.getBossWinAwardToken(profile.getId()));
            if(seasonService != null) {
                getProfileResponse.seasonsBestRank = seasonService.getSeasonsBestRank(profile);
            }
            if(BattleService.isSuperBossBattle(request.missionIds)) {
                getProfileResponse.heroicBossLevel = heroicMissionService.getHeroicMissionLevel(request.missionIds);
            }

            getProfileResponse.clientAddress = Sessions.getOpt(profile).map(s -> s.getConnection().getIP()).orElse("");

            if(profile.compareAndSetBattleState(BattleState.NOT_IN_BATTLE, BattleState.WAIT_START_BATTLE)) {
                profile.setBattleId(request.battleId);
                profile.pvpChangeStateTime = AppUtils.currentTimeSeconds();

                return getProfileResponse;
            } else if(profile.getId() == PvpService.getProfileId(request.battleCreatorPvpId)) {
                // запрашиваем собственный профиль и состояние не совпадает
                log.warn(String.format("[%s] state decync! current: %s, %s", profile.getId(), profile.getBattleState(), request));

                // отменяем бой в который нас уже успели позвать
                Messages.toServer(new RejectBattleOffer(profile.getSocialId(), profile.getId(), profile.getBattleId(), false), source, true);

                profile.setBattleState(BattleState.WAIT_START_BATTLE);
                profile.setBattleId(request.battleId);
                profile.pvpChangeStateTime = AppUtils.currentTimeSeconds();

                return getProfileResponse;
            } else {
                // вызывали на бой друга, а он в бою
                Messages.toUser(new PrivateChatMessage(PvpService.getProfileId(request.battleCreatorPvpId), PrivateChatMessage.PredefinedMessages.BattleRequest), profile);
                return new GetProfileError(request, BATTLE_STATE_MISMATCH);
            }
        } else {
            return new GetProfileError(request, CONNECTION_STATE_MISMATCH);
        }
    }

    public UserProfileStructure getProfileStructureForBattle(BattleWager battleWager, UserProfile profile) {
        if(battleWager == BattleWager.GLADIATOR_DUEL) {
            UserProfileStructure userProfileStructure = profileService.createUserProfileStructure(profile);
            userProfileStructure.wormsGroup = coliseumService.wormsGroup(profile);
            return userProfileStructure;
        } else if(battleWager == BattleWager.MERCENARIES_DUEL) {
            UserProfileStructure userProfileStructure = profileService.createUserProfileStructure(profile);
            userProfileStructure.wormsGroup = mercenariesService.wormsGroup(profile);
            userProfileStructure.backpack = mercenariesService.backpack(profile);
            return userProfileStructure;
        } else {
            return profileService.getUserProfileStructure(profile);
        }
    }

    @OnMessage(value = CompareAndSetBattleState.class, connections = {Connection.INTERCOM})
    public void onCompareAndSetBattleState(CompareAndSetBattleState request, IAppServer source) {
        UserProfile profile = profileService.getUserProfile(request.profileId, false);
        if(profile != null) {
            boolean changeStateResult = profileService.setBattleStateOutside(profile, request.updateState, request.expectState, request.battleId);
            if(log.isDebugEnabled()){
                log.debug("[{}] {} -> {} is {}", profile, request.expectState, request.updateState, changeStateResult);
            }
            if(changeStateResult) {
                if(request.updateState == BattleState.IN_BATTLE_PVP) {
                    profile.pvpBattleType = request.pvpBattleType;

                    String waitBattleTime = PvpService.formatTimeInSeconds(AppUtils.currentTimeSeconds() - profile.pvpChangeStateTime);
                    profileEventsService.fireProfileEventAsync(START_PVP_BATTLE, profile,
                            Param.battleId, request.battleId,
                            "pvpBattleType", request.pvpBattleType.name(),
                            "waitBattleTime", waitBattleTime
                    );
                }
                profile.pvpChangeStateTime = AppUtils.currentTimeSeconds();
            }
        } else {
            log.warn("profile not found! msg: {}", request);
        }
    }

    @OnMessage(value = EndPvpBattleRequest.class, connections = {Connection.INTERCOM})
    public IntercomResponseI onEndPvpBattleRequest(EndPvpBattleRequest request, IAppServer source) {
        final UserProfile profile = profileService.getUserProfile(request.profileId);
        if(profile != null) {
            if(request.result == PvpBattleResult.DRAW_SHUTDOWN) {
                // pvp сервер был остановлен
                if(!profile.inBattleState(BattleState.SIMPLE)) {
                    //и игрок не зашел в бой на main сервере, безусловно выводим его из боя
                    profileService.setBattleStateOutside(profile, BattleState.NOT_IN_BATTLE, profile.getBattleState(), profile.getBattleId());
                    profile.setBattleId(0);
                }
            } else {
                // регулярное завершение боя
                profileService.setBattleStateOutside(profile, BattleState.NOT_IN_BATTLE, BattleState.IN_BATTLE_PVP, 0);
            }

            List<GenericAwardStructure> award = new ArrayList<>();
            battleService.onEndPvpBattle(request, profile, award);
            profile.pvpChangeStateTime = AppUtils.currentTimeSeconds();

            if(request.needResponse) {
                return new EndPvpBattleResponse(request, request.battleId, award);
            }

            if(!profile.isOnline()) {
                profileService.updateSync(profile);
            }
        } else {
            if(request.needResponse) {
                return new EndPvpBattleResponse(request, request.battleId, Collections.emptyList());
            }
        }
        return null;
    }

    @OnMessage(value = CallToBattle.class, connections = {Connection.INTERCOM})
    public void onCallToBattle(CallToBattle request, IAppServer source) {
        UserProfile userProfile = profileService.getUserProfile(request.profileId, false);
        if(userProfile != null) {
            Messages.toUser(request, userProfile, Connection.MAIN);
        } else {
            log.warn("пришел вызов на бой [{}], но профиль [{}] не найден в кеше", request.battleId, request.profileId);
        }
    }

    @Null
    private GetProfileError.GetProfileErrorEnum isProfileFillTheBill(GetProfileRequest request, UserProfile profile) {
        if(request.hasLevel > 0 && profile.getLevel() < request.hasLevel) {
            return INSUFFICIENT_LEVEL;
        }
        profileService.getUserProfileStructure(profile);
        int needMoney = battleService.getBattleWagerDef(request.battleWager, profile.getTeamSize()).value;
        if(needMoney > 0 && profile.getMoney() < needMoney) {
            return NO_ENOUGH_MONEY;
        }
        if(request.battleWager == BattleWager.GLADIATOR_DUEL) {
            ColiseumEntity coliseumEntity = profile.getColiseumEntity();
            if(coliseumEntity == null || !coliseumService.isSeriesInProgress(coliseumEntity))
                return ARENA_IS_LOCKED;
        } else if(request.battleWager == BattleWager.MERCENARIES_DUEL) {
            MercenariesEntity mercenariesEntity = profile.getMercenariesEntity();
            if(mercenariesEntity == null || !mercenariesService.isSeriesInProgress(profile, mercenariesEntity))
                return MERCENARIES_BATTLE_NOT_ACCESSIBLE;
        }
        if(request.battleWager == BattleWager.WAGER_50_2x2 || request.battleWager == BattleWager.WAGER_50_2x2_FRIENDS) {
            if(profile.getActiveTeamMembersCountExceptSoclan() < 2) {
                return TEAM_IS_SMALL;
            }
        }
        if(BattleService.isSingleBossBattle(request.missionIds)) {
            SimpleBattleSettings battleSettings = awardSettingsMap.get(request.getMissionId());
            if(battleSettings == null) {
                log.error("AwardSettings not found for missionId={} in PvE_FRIEND battle", request.getMissionId());
                return ERROR;
            } else if(!battleService.validateBattlesCount(profile)) {
                return EXCEED_BATTLES;
            } else if(!battleService.validateNewMission(profile, request.getMissionId(), battleSettings)) {
                return MISSION_LOCKED;
            }
        } else if(BattleService.isSuperBossBattle(request.missionIds)) {
            if(!heroicMissionService.validateSuperBossMission(request.missionIds, request.mapId)) {
                log.error("HeroicBossBattle request invalid {} current states: {}", request, Arrays.toString(heroicMissionService.getHeroicMissionStates()));
                return ERROR;
            } else if(!battleService.validateBattlesCount(profile)) {
                return EXCEED_BATTLES;
            } else {
                for(short missionId : request.missionIds) {
                    SimpleBattleSettings battleSettings = awardSettingsMap.get(missionId);
                    if(battleSettings == null) {
                        log.error("AwardSettings not found for one of missionId={} in heroic battle: {}", missionId, request);
                        return ERROR;
                    } else if(!battleService.validateSuperBossMission(profile, missionId, battleSettings)) {
                        return MISSION_LOCKED;
                    }
                }
            }
        }
        return null;
    }

    @OnMessage(connections = {Connection.INTERCOM})
    public void onPvpServerStopped(PvpServerStopped request, IAppServer source) {
        Server.sysLog.info("PVP server stopped.");
        softCache.visit(UserProfile.class, (key, userProfile) -> userProfile.setLastProcessedPvpBattleId(0));
    }

}
