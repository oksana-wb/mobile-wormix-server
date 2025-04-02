package com.pragmatix.app.init;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.dao.CookiesDao;
import com.pragmatix.app.domain.BackpackItemEntity;
import com.pragmatix.app.domain.SocialIdEntity;
import com.pragmatix.app.domain.UserProfileEntity;
import com.pragmatix.app.domain.WormGroupsEntity;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.app.model.ProfileInitParams;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.*;
import com.pragmatix.app.services.rating.RankService;
import com.pragmatix.app.services.rating.RatingService;
import com.pragmatix.arena.coliseum.ColiseumService;
import com.pragmatix.arena.mercenaries.MercenariesService;
import com.pragmatix.clan.structures.ClanMemberStructure;
import com.pragmatix.clanserver.messages.request.QuitClanRequest;
import com.pragmatix.clanserver.services.ClanServiceImpl;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.gameapp.sessions.Connection;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.quest.QuestService;
import io.vavr.Tuple;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.WIPE;

/**
 * класс для создания профайла пользователя по умолчанию
 *
 * @author denis
 *         date 18.11.2009
 *         time: 16:44:05
 */
@Service
public class UserProfileCreator {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileCreator.class);

    @Value("#{profileInitParams}")
    private ProfileInitParams profileInitParams;

    @Value("${debug.vkontakteTest:false}")
    private boolean vkontakteTest = false;

    @Resource
    private DaoService daoService;

    @Resource
    private CookiesDao cookiesDao;

    @Resource
    private ProfileService profileService;

    /**
     * пустой список группы червей, тк при создании у игрока нету группы
     */
    public static final WormGroupsEntity EMPTY_WORM_GROUP = null;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private RatingService ratingService;

    @Resource
    private SoftCache softCache;

    @Resource
    private CraftService craftService;

    @Resource
    private TrueSkillService trueSkillService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private WeaponService weaponService;

    @Resource
    private ClanServiceImpl clanService;

    @Autowired(required = false)
    private ColiseumService coliseumService;

    @Autowired(required = false)
    private MercenariesService mercenariesService;

    @Resource
    private QuestService questService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private HeroicMissionService heroicMissionService;

    @Resource
    private Optional<DepositService> depositService;

    public Long assignLongIdToProfile(String profileSocialId, SocialServiceEnum socialNetId) {
        return daoService.doInTransaction(() -> {
            long createdProfileId = jdbcTemplate.queryForObject("SELECT nextval('profile_id_sequence')", Long.class);
            SocialIdEntity entity = new SocialIdEntity();
            entity.setStringId(profileSocialId);
            entity.setSocialNetId((short) socialNetId.getType());
            entity.setProfileId(createdProfileId);
            daoService.getSocialIdDao().insert(entity);
            return createdProfileId;
        });
    }

    public void assignStingIdToProfile(Long profileId, String profileSocialId, SocialServiceEnum socialNetId) {
        daoService.doInTransactionWithoutResult(() -> {
            SocialIdEntity entity = new SocialIdEntity();
            entity.setStringId(profileSocialId);
            entity.setSocialNetId((short) socialNetId.getType());
            entity.setProfileId(profileId);
            daoService.getSocialIdDao().insert(entity);
        });
    }

    public void reassignStingIdToNewProfile(Long profileId, String profileSocialId) {
        daoService.doInTransactionWithoutResult(() ->
                daoService.getSocialIdDao().reassignStingIdToNewProfile(profileId, profileSocialId));
    }

    /**
     * создает профайл пользователя и сохроняет его в БД
     *
     * @param socialId логин пользователя которого еще нет в БД
     * @return модель профайла
     */
    public UserProfile createUserProfile(Long socialId) {
        if(logger.isDebugEnabled()) {
            logger.debug("create userProfile by Long type {}", socialId);
        }
        //создаем профайл игрока
        UserProfileEntity userProfileEntity = new UserProfileEntity();
        userProfileEntity.setId(socialId);

        return daoService.doInTransaction(() -> createUserProfile(userProfileEntity));
    }

    public UserProfile createUserProfile(UserProfileEntity userProfileEntity) {
        ProfileInitParams profileInitParams = getProfileInitParams();

        userProfileEntity.setMoney(profileInitParams.getMoney());
        userProfileEntity.setRealmoney(profileInitParams.getRealMoney());
        userProfileEntity.setLevel(profileInitParams.getLevel());
        userProfileEntity.setBattlesCount(profileInitParams.getBattlesCount());

        //создаем ворма
        userProfileEntity.setAttack(profileInitParams.getAttack());
        userProfileEntity.setArmor(profileInitParams.getArmor());
        userProfileEntity.setExperience((short) 0);
        userProfileEntity.setLastBattleTime(new Date());
        userProfileEntity.setLastLoginTime(new Date(0));
        userProfileEntity.setLastSearchTime(null);

        //по умолчанию будут баксерами
        userProfileEntity.setRace(profileInitParams.getRace().getShortType());
        userProfileEntity.setName("");
        userProfileEntity.setRenameAct(profileInitParams.getRenameActions());
        userProfileEntity.setRenameVipAct((byte) 0);

        //сохраняем профайл в БД
        daoService.getUserProfileDao().insert(userProfileEntity);

        List<BackpackItemEntity> backpackList = createDefaultBackpack(userProfileEntity.getId());

        //создаем рюкзак для игрока
        UserProfile userProfile = profileService.newUserProfile(userProfileEntity, backpackList, EMPTY_WORM_GROUP, Tuple.of(0, RankService.INIT_RANK_VALUE));
        String profileStringId = profileService.getProfileStringId(userProfile.getId());
        userProfile.setProfileStringId(profileStringId);
        return userProfile;
    }

    public void wipeUserProfile(final UserProfile profile) {
        ProfileInitParams profileInitParams = getProfileInitParams();
        // удаляем игрока из суточного реестра
        dailyRegistry.clearFor(profile.getId());

        // обнуляем рейтинг и удаляем из топов
        ratingService.onWipe(profile);

        UserProfileStructure userProfileStructure = profileService.getUserProfileStructure(profile);
        ClanMemberStructure clanMemberWraper = userProfileStructure.clanMember;
        if(clanMemberWraper != null && clanMemberWraper.getClanMember() != null) {
            clanService.quitClan(new QuitClanRequest(), clanMemberWraper.getClanMember());
        }

        profile.setLevel((short) 1);
        profile.setMoney(profileInitParams.getMoney());
        profile.setRealMoney(profileInitParams.getRealMoney());
        profile.setBattlesCount(profileInitParams.getBattlesCount());

        if(vkontakteTest) {
            try {
                Connection connection = Connections.get();
                if(connection != null) {
                    // на тесте получаем уровень игрока из диалога подтверждения
                    Integer value = (Integer) connection.getStore().get("wipeProfileLevel");
                    if(value != null && value > 0 && value <= 30) {
                        profile.setLevel(value);
                    }
                    // а также можеи обнулить как положено
                    if(connection.getStore().containsKey("wipeProfile")) {
                        profile.setMoney(profileInitParams.getMoney());
                        profile.setRealMoney(profileInitParams.getRealMoney());
                        profile.setBattlesCount(profileInitParams.getBattlesCount());
                    }
                }
            } catch (Exception e) {
                logger.error(e.toString(), e);
            }
        }
        //обнуляем игрока
        profile.unsetName();
        profile.setAttack(profileInitParams.getAttack());
        profile.setArmor(profileInitParams.getArmor());
        profile.setExperience(0);
        profile.setLastBattleTime(System.currentTimeMillis());
        profile.setLastLoginTime(null);
        profile.setRace(profileInitParams.getRace());
        profile.setRaces(Race.setRace((profileInitParams.getRace())));
        profile.setSkins(ArrayUtils.EMPTY_BYTE_ARRAY);
        profile.setSelectRaceTime(0);
        profile.setHat((short) 0);
        profile.setKit((short) 0);
        profile.setStuff(new short[0]);
        profile.setTemporalStuff(new byte[0]);
        profile.setReactionRate(0);
        profile.setLoginSequence((byte) 0);
        profile.setCurrentMission((short) 0);
        profile.setCurrentNewMission((short) 0);
        profile.setLastSearchTime(null);
        profile.setRecipes(new short[0]);
        profile.setComebackedFriends((short) 0);
        profile.unsetName();
        profile.setRenameAct(profileInitParams.getRenameActions());
        profile.setVipExpiryTime(0);

        profile.initWormGroup(EMPTY_WORM_GROUP);
        List<BackpackItemEntity> backpack = createDefaultBackpack(profile.getId());
        profile.setBackpack(initBackpack(backpack));

        profile.setUserProfileStructure(null);

        craftService.wipeReagents(profile);
        trueSkillService.wipeTrueSkill(profile);
        heroicMissionService.onWipeProfile(profile);

        profileService.updateSync(() -> {
            daoService.getBackpackItemDao().deleteBackpack(profile.getId());
            daoService.getWormGroupDao().deleteWormGroups(profile.getId());
            daoService.getUserProfileDao().clearMeta(profile.getId());
            daoService.getUserProfileDao().clearRanks(profile.getId());
            weaponService.wipeBackpackConf(profile);
            if(coliseumService != null) coliseumService.wipeColiseumState(profile);
            if(mercenariesService != null) mercenariesService.wipeMercenariesState(profile);
            questService.wipeQuestsState(profile);
            cookiesDao.deleteById(profile.getId().intValue());
            depositService.ifPresent(service -> service.clearDeposits(profile));
        }, profile);

        softCache.remove(UserProfile.class, profile.getId());

        profileEventsService.fireEvent(WIPE, profile, null, null);
    }

    /**
     * создание рюкзака для пользователя
     *
     * @param profileId id профайла пользователя
     * @return коллекцию вещей в рюкзаке
     */
    public List<BackpackItemEntity> createDefaultBackpack(Long profileId) {
        ProfileInitParams profileInitParams = getProfileInitParams();
        Map<Integer, Integer> weapons = profileInitParams.getWeapons();
        List<BackpackItemEntity> backpack = new ArrayList<>(weapons.size());
        for(Map.Entry<Integer, Integer> pair : weapons.entrySet()) {
            Integer weaponId = pair.getKey();
            Integer count = pair.getValue();
            backpack.add(createBackpackItem(profileId, weaponId, weaponService.getWeapon(weaponId).increment(0, count)));
        }
        return backpack;
    }

    /**
     * Инициализация рюкзака из БД
     *
     * @param backpackItemEntities рюкзак из БД
     */
    public List<BackpackItem> initBackpack(List<BackpackItemEntity> backpackItemEntities) {
        ProfileInitParams profileInitParams = getProfileInitParams();
        Set<Integer> startWeapons = profileInitParams.getDefaultEndlessWeapons();
        List<BackpackItem> backpack = new ArrayList<>(backpackItemEntities.size() + startWeapons.size());
        for(int weaponId : startWeapons) {
            backpack.add(new BackpackItem(weaponId, -1, false));
        }
        //загружаем рюкзак из БД
        for(BackpackItemEntity backpackItemEntity : backpackItemEntities) {
            //кому раньше выдали оружие из стартового набора - пропускаем
            if(startWeapons.contains((int) backpackItemEntity.getWeaponId()))
                continue;
            backpack.add(new BackpackItem(backpackItemEntity));
        }
        return backpack;
    }

    /**
     * @param profileId для кого создать
     * @param weaponId  какое оружие
     * @param count     какое количество
     * @return BackpackItemEntity
     */
    public BackpackItemEntity createBackpackItem(Long profileId, int weaponId, int count) {
        BackpackItemEntity backpackItemEntity = new BackpackItemEntity();
        backpackItemEntity.setProfileId(profileId.intValue());
        backpackItemEntity.setWeaponId((short) weaponId);
        backpackItemEntity.setWeaponCount((short) count);
        backpackItemEntity.newly = true;
        return backpackItemEntity;
    }

    //====================== Getters and Setters =================================================================================================================================================

    public ProfileInitParams getProfileInitParams() {
        if(profileInitParams == null)
            throw new IllegalStateException("не заданы параметры создания нового игрока!");
        return profileInitParams;
    }

    public void setProfileInitParams(ProfileInitParams profileInitParams) {
        this.profileInitParams = profileInitParams;
    }

}
