package com.pragmatix.app.services;

import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.achieve.services.AchieveCommandService;
import com.pragmatix.app.common.BanType;
import com.pragmatix.app.dao.BackpackConfDao;
import com.pragmatix.app.dao.WormGroupsDao;
import com.pragmatix.app.domain.*;
import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.group.MercenaryTeamMember;
import com.pragmatix.app.model.group.TeamMember;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.arena.coliseum.ColiseumEntity;
import com.pragmatix.arena.coliseum.ColiseumService;
import com.pragmatix.arena.mercenaries.MercenariesEntity;
import com.pragmatix.arena.mercenaries.MercenariesService;
import com.pragmatix.arena.mercenaries.messages.BackpackItemShortStruct;
import com.pragmatix.craft.domain.ReagentsEntity;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.intercom.service.AchieveServerAPI;
import com.pragmatix.intercom.service.MainServerAPI;
import com.pragmatix.intercom.structures.*;
import com.pragmatix.quest.QuestService;
import com.pragmatix.quest.dao.QuestEntity;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.pragmatix.app.dao.WormGroupsDao.teamMemberNamesFromVarchar;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 18.05.2017 14:31
 */
@Service
public class CloneProfileService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ProfileService profileService;

    @Resource
    private TrueSkillService trueSkillService;

    @Resource
    private WeaponService weaponService;

    @Autowired(required = false)
    private ColiseumService coliseumService;

    @Autowired(required = false)
    private MercenariesService mercenariesService;

    @Resource
    private QuestService questService;

    @Resource
    private CookiesService cookiesService;

    @Resource
    private CraftService craftService;

    @Resource
    private AchieveServerAPI achieveServerAPI;

    @Resource
    private AchieveCommandService achieveService;

    @Resource
    private MainServerAPI mainServerAPI;

    @Resource
    private StatisticService statisticService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private BanService banService;

    @Resource
    private BackpackConfDao backpackConfDao;

    @Resource
    private GroupService groupService;

    @Resource
    private DaoService daoService;

    @Value("${CloneProfileService.masterSecureToken:}")
    public String masterSecureToken;

    public static boolean cloneActionEnabled = true;

    @Value("${CloneProfileService.cloneActionEnabled:true}")
    public void setCloneActionEnabled(boolean cloneActionEnabled) {
        this.cloneActionEnabled = cloneActionEnabled;
    }

    @PostConstruct
    public void init() {
        if(StringUtils.isEmpty(masterSecureToken)) {
            masterSecureToken = RandomStringUtils.randomAlphanumeric(16);
        }
    }

    public boolean cloneProfile(UserProfile destProfile, SocialServiceEnum sourceServer, long sourceProfileId, boolean banSourceProfile, String secureToken) {
        String sourceServerName = sourceServer.name();
        String note = String.format("clone profile (%s:%s) => %s", sourceServerName, sourceProfileId, destProfile.getId());
        log.info(note + " ...");
        Optional<UserProfileIntercomStructure> userProfileIntercomStructureOpt = mainServerAPI.requestUserProfileIntercomStructure(destProfile, sourceServer, sourceProfileId, banSourceProfile, secureToken, note);
        if(userProfileIntercomStructureOpt.isPresent()) {
            UserProfileIntercomStructure result = userProfileIntercomStructureOpt.get();
            try {
                profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.EXTRA, destProfile,
                        Param.eventType, "tryCloneProfile",
                        "sourceServer", sourceServerName,
                        "sourceProfileId", sourceProfileId,
                        "secureToken", secureToken
                );
                long wipeStatisticId = statisticService.wipeStatistic(destProfile, note, "");

                List<String[]> moveProfileHistory = questService.getQuestEntity(destProfile).q3().moveProfileHistory;

                restoreFromStructure(destProfile, result);

                profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.EXTRA, destProfile,
                        Param.eventType, "cloneProfileSuccess",
                        "sourceServer", sourceServerName,
                        "sourceProfileId", sourceProfileId,
                        "wipeStatisticId", wipeStatisticId,
                        "secureToken", secureToken
                );
                if(banSourceProfile) {
                    banService.addToBanList(destProfile.getId(), BanType.PROFILE_MOVED_FROM, String.format("профиль был перенесен из %s:%s", sourceServer.name(), sourceProfileId));

                    QuestEntity questEntity = questService.getQuestEntity(destProfile);
                    questEntity.q3().moveProfileHistory.addAll(moveProfileHistory);
                    questEntity.q3().moveProfileHistory.add(new String[]{LocalDateTime.now().toString(), "" + sourceServer.getType(), "" + sourceProfileId, "" + wipeStatisticId});
                    questEntity.q3().moveProfileHistory.sort(Comparator.comparing((String[] ss) -> ss[0]).reversed());
                    questEntity.dirty = true;
                }
                log.info("clone profile ({}:{}) success", sourceServerName, sourceProfileId);
                Sessions.getOpt(destProfile).ifPresent(Session::close);
                return true;
            } catch (Exception e) {
                log.error(String.format("clone profile (%s:%s) error: %s", sourceServerName, sourceProfileId, e.toString()), e);
            }
        } else {
            log.warn("clone profile ({}:{}) failure. Result profile's structure is empty", sourceServerName, sourceProfileId);
        }
        return false;
    }

    public static String generateIntercomSecureToken() {
        return cloneActionEnabled ? RandomStringUtils.randomAlphanumeric(16) : "";
    }

    public UserProfileIntercomStructure dumpToStructure(UserProfile profile) {
        return newUserProfileBigStructure(profile);
    }

    public void restoreFromStructure(UserProfile profile, UserProfileIntercomStructure source) {
        UserProfileEntity entity = newUserProfileEntity(profile.getId(), source);
        WormGroupsEntity wormGroupEntities = newWormGroupsEntity(profile.getId().intValue(), source);
        Tuple2<Integer, Byte> rankValues = Tuple.of(source.rankPoints, source.bestRank);
        profile.init(entity, wormGroupEntities, rankValues);

        if(profile.getWormsGroup().length == 1) {
            if(profile.getLevel() >= 5) {
                groupService.addFreeMerchenary(profile);

                WormGroupsEntity wormGroupsEntity = daoService.getWormGroupDao().getWormGroupsByProfileId(profile.getProfileId());
                if(wormGroupsEntity == null) {
                    daoService.doInTransaction(() -> daoService.getWormGroupDao().insertWormGroups(profile));
                }
            } else {
                daoService.doInTransaction(() -> daoService.getWormGroupDao().deleteWormGroups(profile.getId()));
            }
        } else {
            WormGroupsEntity wormGroupsEntity = daoService.getWormGroupDao().getWormGroupsByProfileId(profile.getProfileId());
            if(wormGroupsEntity == null) {
                daoService.doInTransaction(() -> daoService.getWormGroupDao().insertWormGroups(profile));
            }
        }

        Map<Integer, Short> newWeapons = source.backpack.stream().collect(Collectors.toMap(i -> (int) i.weaponId, i -> i.count));
        profile.getBackpack().stream().filter(i -> !newWeapons.containsKey(i.getWeaponId())).forEach(BackpackItem::clean);
        profile.getBackpack().stream().filter(i -> newWeapons.containsKey(i.getWeaponId())).forEach(i -> i.setCount(newWeapons.get(i.getWeaponId())));
        profile.getBackpack().addAll(newWeapons.entrySet().stream()
                .filter(e -> profile.getBackpackItemByWeaponId(e.getKey()) == null)
                .map(e -> new BackpackItem(e.getKey(), e.getValue(), true))
                .collect(Collectors.toList())
        );

        TrueSkillEntity trueSkillEntity = trueSkillService.getTrueSkillFor(profile);
        source.trueSkill.merge(trueSkillEntity);

        BackpackConfEntity backpackConfEntity = weaponService.getBackpackConfEntityOrCreate(profile);
        source.backpackConf.merge(backpackConfEntity);

        ReagentsEntity reagentsEntity = craftService.getReagentsForProfile(profile.getId());
        for(int i = 0; i < source.reagents.length; i++) {
            reagentsEntity.setReagentValue((byte) i, source.reagents[i]);
        }
        reagentsEntity.setDirty(true);

        if(coliseumService != null) {
            ColiseumEntity coliseumEntity = coliseumService.coliseumEntity(profile);
            source.coliseum.merge(coliseumEntity, coliseumService.getDao());
        }

        if(mercenariesService != null) {
            MercenariesEntity mercenariesEntity = mercenariesService.mercenariesEntity(profile);
            source.mercenaries.merge(mercenariesEntity);
        }

        QuestEntity questEntity = questService.getQuestEntity(profile);
        source.quest.merge(questEntity);

        CookiesEntity cookiesEntity = cookiesService.getCookiesFor(profile);
        cookiesEntity.setValues(CookiesEntity.CookiesValuesType.fromJson(source.cookies));
        cookiesEntity.dirty = true;

        profile.setUserProfileStructure(null);
        profileService.getUserProfileStructure(profile);

        profile.setTeamMembersDirty(true);
        profile.setDirty(true);

        profileService.updateSync(profile);
        if(ArrayUtils.isNotEmpty(backpackConfEntity.getSeasonsBestRank())) {
            backpackConfDao.updateSeasonsBestRank(backpackConfEntity);
        }

        ProfileAchievements profileAchievements = achieveService.getProfileAchievementsOrCreateNew(profileService.getProfileAchieveId(profile.getProfileId()), WormixAchievements.class);
        if(source.achievements != null) {
            source.achievements.merge(profileAchievements);
        } else {
            ProfileAchieveStructure profileAchieveStructure = new ProfileAchieveStructure(new WormixAchievements(profileAchievements.getProfileId()));
            profileAchieveStructure.merge(profileAchievements);
        }
        profileService.findAndUpdateAchievements(profile);
    }

    private UserProfileIntercomStructure newUserProfileBigStructure(UserProfile profile) {
        TrueSkillEntity trueSkillEntity = trueSkillService.getTrueSkillFor(profile);
        BackpackConfEntity backpackConfEntity = weaponService.getBackpackConfEntity(profile);
        backpackConfEntity = backpackConfEntity == null ? new BackpackConfEntity(profile.getId()) : backpackConfEntity;
        ReagentsEntity reagentsEntity = craftService.getReagentsForProfile(profile.getId());
        ColiseumEntity coliseumEntity = coliseumService != null ? coliseumService.coliseumEntity(profile) : null;
        MercenariesEntity mercenariesEntity = mercenariesService != null ?  mercenariesService.mercenariesEntity(profile) : null;
        QuestEntity questEntity = questService.getQuestEntity(profile);
        CookiesEntity cookiesEntity = cookiesService.getCookiesFor(profile);

        UserProfileIntercomStructure result = new UserProfileIntercomStructure();
        result.id = profile.getId();
        result.name = profile.getName();
        result.money = profile.getMoney();
        result.realmoney = profile.getRealMoney();
        result.rating = profile.getRating();
        result.armor = (short) profile.getArmor();
        result.attack = (short) profile.getAttack();
        result.battlesCount = profile.getBattlesCount();
        result.level = (short) profile.getLevel();
        result.experience = (short) profile.getExperience();
        result.hat = profile.getHat();
        result.race = profile.getRace();
        result.races = profile.getRaces();
        result.selectRaceTime = profile.getSelectRaceTime();
        result.kit = profile.getKit();
        result.lastBattleTime = profile.getLastBattleTime();
        result.lastLoginTime = Optional.ofNullable(profile.getLastLoginTime()).map(Date::getTime).orElse(0L);
        result.stuff = profile.getStuff();
        result.temporalStuff = profile.getTemporalStuff();
        result.lastSearchTime = profile.lastSearchTime;
        result.loginSequence = profile.getLoginSequence();
        result.reactionRate = profile.getReactionRate();
        result.currentMission = profile.getCurrentMission();
        result.currentNewMission = profile.getCurrentNewMission();
        result.recipes = profile.getRecipes();
        result.comebackedFriends = profile.getComebackedFriends();
        result.locale = (short) profile.getLocale().getType();
        result.renameAct = profile.getRenameAct();
        result.renameVipAct = profile.getRenameVipAct();
        result.logoutTime = profile.getLogoutTime();
        result.pickUpDailyBonus = profile.getPickUpDailyBonus();
        result.skins = profile.getSkins();
        result.vipExpiryTime = profile.getVipExpiryTime();

        result.rankPoints = profile.getRankPoints();
        result.bestRank = profile.getBestRank();

        int[] wormsGroup = ArrayUtils.EMPTY_INT_ARRAY;
        TeamMember[] teamMembers = new TeamMember[0];
        for(int i = 0; i < profile.getWormsGroup().length; i++) {
            TeamMember teamMember = profile.getTeamMembers()[i];
            if(profile.getWormsGroup()[i] == profile.id) {
                wormsGroup = ArrayUtils.add(wormsGroup, profile.id);
                teamMembers = ArrayUtils.add(teamMembers, null);
            } else if(teamMember.getClass() == MercenaryTeamMember.class) {
                wormsGroup = ArrayUtils.add(wormsGroup, profile.getWormsGroup()[i]);
                teamMembers = ArrayUtils.add(teamMembers, teamMember);
            }
        }

        result.wormsGroup = wormsGroup;
        TeamMember[] finalTeamMembers = teamMembers;
        result.teamMembers = IntStream.rangeClosed(0, 6).mapToObj(i -> WormGroupsDao.toBytea(finalTeamMembers, i)).map(ByteArrayWrapStructure::new).collect(Collectors.toList());
        result.teamMemberNames = WormGroupsDao.teamMemberNamesToVarchar(WormGroupsDao.getNames(wormsGroup, teamMembers));
        result.extraGroupSlotsCount = profile.getExtraGroupSlotsCount();

        result.backpack = profile.getBackpack().stream().filter(BackpackItem::isNotEmpty).map(BackpackItemShortStruct::new).collect(Collectors.toList());

        result.reagents = reagentsEntity.getValues();
        result.trueSkill = new ProfileTrueSkillStructure(trueSkillEntity);
        result.backpackConf = new ProfileBackpackConfStructure(backpackConfEntity);
        result.coliseum = coliseumEntity != null ? new ProfileColiseumStructure(coliseumEntity) : null;
        result.mercenaries = mercenariesEntity != null ? new ProfileMercenariesStructure(mercenariesEntity) : null;
        result.quest = new ProfileQuestStructure(questEntity);
        result.cookies = CookiesEntity.CookiesValuesType.toJson(cookiesEntity.getValues());
        result.achievements = achieveServerAPI.getAchievements(profile);

        return result;
    }

    private UserProfileEntity newUserProfileEntity(Long profileId, UserProfileIntercomStructure source) {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setId(profileId);

        entity.name = source.getName();
        entity.money = source.getMoney();
        entity.realmoney = source.getRealmoney();
        entity.rating = source.getRating();
        entity.armor = source.getArmor();
        entity.attack = source.getAttack();
        entity.battlesCount = source.getBattlesCount();
        entity.level = source.getLevel();
        entity.experience = source.getExperience();
        entity.hat = source.getHat();
        entity.race = source.getRace();
        entity.races = source.getRaces();
        entity.selectRaceTime = source.getSelectRaceTime();
        entity.kit = source.getKit();
        entity.lastBattleTime = new Date(source.getLastBattleTime());
        entity.lastLoginTime = new Date(source.getLastLoginTime());
        entity.stuff = source.getStuff();
        entity.temporalStuff = source.getTemporalStuff();
        entity.lastSearchTime = new Date(source.lastSearchTime);
        entity.loginSequence = source.getLoginSequence();
        entity.reactionRate = source.getReactionRate();
        entity.currentMission = source.getCurrentMission();
        entity.currentNewMission = source.getCurrentNewMission();
        entity.recipes = source.getRecipes();
        entity.comebackedFriends = source.getComebackedFriends();
        entity.locale = source.getLocale();
        entity.renameAct = source.getRenameAct();
        entity.renameVipAct = source.getRenameVipAct();
        entity.logoutTime = new Date(source.getLogoutTime() * 1000L);
        entity.pickUpDailyBonus = source.getPickUpDailyBonus();
        entity.skins = source.getSkins();
        entity.vipExpiryTime = new Date(source.getVipExpiryTime() * 1000L);

        return entity;
    }

    private WormGroupsEntity newWormGroupsEntity(int profileId, UserProfileIntercomStructure source) {
        WormGroupsEntity entity = new WormGroupsEntity();
        entity.setProfileId(profileId);
        for(int i = 0; i < source.wormsGroup.length; i++) {
            byte[] meta = source.teamMembers.get(i).value;
            if(ArrayUtils.isEmpty(meta)) {
                entity.getTeamMembers()[i] = profileId;
                entity.setTeamMemberMeta(null, i);
            } else {
                entity.getTeamMembers()[i] = source.wormsGroup[i];
                entity.setTeamMemberMeta(meta, i);
            }
        }
        entity.setTeamMemberNames(source.teamMemberNames);
        entity.setTeamMemberNamesMap(teamMemberNamesFromVarchar(source.teamMemberNames));
        entity.setExtraGroupSlotsCount(source.extraGroupSlotsCount);

        return entity;
    }
}
