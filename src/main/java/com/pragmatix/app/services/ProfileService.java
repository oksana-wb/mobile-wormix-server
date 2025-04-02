package com.pragmatix.app.services;

import com.pragmatix.achieve.services.AchieveCommandService;
import com.pragmatix.app.common.*;
import com.pragmatix.app.dao.BackpackConfDao;
import com.pragmatix.app.dao.CookiesDao;
import com.pragmatix.app.dao.SocialIdDao;
import com.pragmatix.app.dao.TrueSkillDao;
import com.pragmatix.app.domain.BackpackItemEntity;
import com.pragmatix.app.domain.TrueSkillEntity;
import com.pragmatix.app.domain.UserProfileEntity;
import com.pragmatix.app.domain.WormGroupsEntity;
import com.pragmatix.app.init.StuffCreator;
import com.pragmatix.app.init.UserProfileCreator;
import com.pragmatix.app.init.WeaponsCreator;
import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.app.model.RentedItems;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.group.*;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.services.authorize.LoginService;
import com.pragmatix.app.services.rating.*;
import com.pragmatix.app.services.social.SocialUserIdMapService;
import com.pragmatix.arena.coliseum.ColiseumService;
import com.pragmatix.arena.mercenaries.MercenariesService;
import com.pragmatix.clan.structures.ClanMemberStructure;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.services.ClanSeasonService;
import com.pragmatix.clanserver.services.ClanService;
import com.pragmatix.clanserver.services.ClanServiceImpl;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.craft.dao.ReagentsDao;
import com.pragmatix.craft.domain.ReagentsEntity;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.gameapp.IGameApp;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.gameapp.services.TaskService;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.gameapp.social.service.facebook.FacebookService;
import com.pragmatix.notify.NotifyService;
import com.pragmatix.quest.QuestService;
import io.vavr.Function2;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.persistence.PersistenceException;
import javax.validation.constraints.Null;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.pragmatix.app.services.GroupService.MAX_TEAM_MEMBERS;
import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.EXTRA;

/**
 * User: denis
 * Date: 07.11.2009
 * Time: 18:54:06
 */
@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    @Resource
    private SocialService socialService;

    @Resource
    private SoftCache softCache;

    @Resource
    private UserProfileCreator userProfileCreator;

    @Resource
    private DaoService daoService;

    @Resource
    private TaskService taskService;

    @Resource
    private TrueSkillDao trueSkillDao;

    @Resource
    private BackpackConfDao backpackConfDao;

    @Autowired(required = false)
    private ColiseumService coliseumService;

    @Autowired(required = false)
    private MercenariesService mercenariesService;

    @Resource
    private QuestService questService;

    @Resource
    private ReagentsDao reagentsDao;

    @Resource
    private SocialUserIdMapService socialUserIdMap;

    @Resource
    private StuffService stuffService;

    @Resource
    private TrueSkillService trueSkillService;

    @Resource
    private ClanServiceImpl clanService;

    @Resource
    private SocialIdDao socialIdDao;

    @Resource
    private CookiesDao cookiesDao;

    @Resource
    private ClanSeasonService clanSeasonService;

    @Value("${ClanSeasonService.enabled:true}")
    private boolean clansEnabled = true;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Value("${ProfileService.vipBoostFactorValue:2}")
    private int vipBoostFactorValue = 2;

    @Autowired(required = false)
    private FacebookService facebookService;

    @Resource
    private NotifyService notifyService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private Optional<RankService> rankService;

    @Resource
    private SkinService skinService;

    @Resource
    private WeaponsCreator weaponsCreator;

    @Resource
    private StuffCreator stuffCreator;

    public static final String FACEBOOK_LOGIN_NOTIFICATION_KEY = "FACEBOOK_LOGIN";

    private Function2<UserProfile, Byte, Byte> skinProducer;

    @Resource
    private GroupService groupService;

    @Resource
    private RatingService ratingService;

    @Resource
    private DailyRatingService dailyRatingService;

    @Resource
    protected IGameApp gameApp;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AchieveCommandService achieveService;

    @Resource
    private CraftService craftService;

    // время в часах, через которое игрок может бесплатно сменить рассу
    private int selectRaceMinTimeInHours = 12;

    @Value("${masterAuthKey:}")
    private String masterAuthKey = "";

    @Value("${server.id:development}")
    private String serverId = "development";

    @Resource(name = "appRestTemplate")
    private RestTemplate restTemplate;

    @Value("${ProfileService.logLoginByReferrerUrl}")
    private String logLoginByReferrerUrl = "http://127.0.0.1:9911/wormix_{app}/{loginType}?ref_id={referrer}&profile_id={profileId}";

    public void init() {
        if(masterAuthKey == null || masterAuthKey.isEmpty())
            masterAuthKey = RandomStringUtils.randomAlphanumeric(24);

        skinProducer = (profile, race) -> skinService.getSkin(profile, race);
    }

    public Callable<Tuple3<ClanMemberStructure, Byte, Byte>> clanMember_rank_skin(UserProfile profile) {
        return () -> {
            Byte rank = rankService.filter(RankService::isEnabled).map(rankService -> rankService.getPlayerRealRank(profile)).orElse(RankService.INIT_RANK_VALUE);
            return Tuple.of(newClanMemberStructure(profile), rank, skinService.getSkin(profile));
        };
    }

    // Ассоциировать с профилем новый строковый Id в другой "соц сети"
    public void assignStingIdToProfile(UserProfile profile, String profileStringId, SocialServiceEnum socialNetId) {
        Long profileId = profile.getId();
        Long longId = socialUserIdMap.mapToNumberId(profileStringId);
        if(longId == null) {
            String currStringId = socialUserIdMap.mapToStringId(profileId, socialNetId);
            if(currStringId != null && !currStringId.equals(profileStringId)) {
                dissociateStingIdFromProfile(profile, socialNetId);
            }
            try {
                userProfileCreator.assignStingIdToProfile(profileId, profileStringId, socialNetId);
                socialUserIdMap.map(profileStringId, (short) socialNetId.getType(), profileId);
            } catch (DataIntegrityViolationException e) {
                log.warn("need dissociate: " + e.toString());
                if(dissociateStingIdFromProfile(profile, socialNetId)) {
                    assignStingIdToProfile(profile, profileStringId, socialNetId);
                }
            }
        } else if(!longId.equals(profileId)) {
            log.warn("С данным stringId [{}] в сети [{}] уже ассоциирован другой профиль [{}]! Привязываем его к текущему профилю [{}]", profileStringId, socialNetId, longId, profile.id);
            // привязываем этот profileStringId к новому профилю
            try {
                String currStringId = socialUserIdMap.mapToStringId(profileId, socialNetId);
                if(currStringId != null && !currStringId.equals(profileStringId)) {
                    dissociateStingIdFromProfile(profile, socialNetId);
                }
                try {
                    userProfileCreator.reassignStingIdToNewProfile(profileId, profileStringId);
                } catch (PersistenceException e) {
                    log.warn("need dissociate: " + e.toString());
                    dissociateStingIdFromProfile(profile, socialNetId);

                    userProfileCreator.reassignStingIdToNewProfile(profileId, profileStringId);
                }

                socialUserIdMap.map(profileStringId, (short) socialNetId.getType(), profileId);

                if(socialNetId == SocialServiceEnum.facebook) {
                    facebookService.sendNotification(profileStringId, notifyService.getLocalizeMessage(profile.getLocale(), FACEBOOK_LOGIN_NOTIFICATION_KEY));
                }
            } catch (DataIntegrityViolationException e) {
                log.warn(e.toString(), e);
            }
        }
    }

    // Отвязать профиль от указанной соц. сети
    public boolean dissociateStingIdFromProfile(final UserProfile profile, SocialServiceEnum socialNetId) {
        boolean result;
        final String profileSocialId = socialUserIdMap.getProfilesByNetAndLongIdMap().get(socialNetId).remove(profile.getId());
        if(profileSocialId != null) {
            socialUserIdMap.getProfilesByStringIdMap().remove(profileSocialId);
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    socialIdDao.dissociateStingIdFromProfile(profile.getId(), profileSocialId);
                }
            });
            String profileStringId = getProfileStringId(profile.getId());
            profile.setProfileStringId(profileStringId);
            if(log.isDebugEnabled()) {
                log.debug("[{}] обновлен строковый инедтификатор -> [{}]", profile, profileStringId);
            }
            result = true;
        } else {
            log.error("строковый id не найден для {} в соц. сети {}", profile, socialNetId);
            result = false;
        }
        return result;
    }

    public boolean validateLogin(boolean condition, String authKey) {
        return condition || authKey.equals(masterAuthKey);
    }

    public Long getProfileLongId(String profileStringId) {
        Long profileLongId = getProfileLongId(profileStringId, null, false);
        if(profileLongId == null) {
            try {
                return (long) Integer.parseInt(profileStringId);
            } catch (NumberFormatException e) {
            }
        }
        return profileLongId;
    }

    /**
     * Получить id игрока типа long используя id типа String
     *
     * @param profileStringId строковый id игрока
     * @param createNew       создать новый id типа long и поставить его в соответствие со строковым id
     * @return соответствующий id типа long
     */
    public Long getProfileLongId(String profileStringId, SocialServiceEnum socialNetId, boolean createNew) {
        Long longId = socialUserIdMap.mapToNumberId(profileStringId);

        if(longId == null && createNew) {
            try {
                longId = userProfileCreator.assignLongIdToProfile(profileStringId, socialNetId);
            } catch (DataIntegrityViolationException e) {
                longId = daoService.getSocialIdDao().selectByStringId(profileStringId).getId();
            }

            socialUserIdMap.map(profileStringId, (short) socialNetId.getType(), longId);
        }
        return longId;
    }

    public String getProfileStringId(Long profileLongId) {
        String stringId = socialUserIdMap.mapToStringId(profileLongId);
        return stringId == null ? "" : stringId;
    }

    /**
     * Закрузить из кеша профиль пользователя по его uid
     *
     * @param uid идентификатор социальной сети
     * @return UserProfile
     */
    public UserProfile getUserProfile(Object uid) {
        if(uid == null) {
            return null;
        }

        Long profileId = getProfileLongId(uid);

        UserProfile profile = null;
        if(profileId != null) {
            profile = softCache.get(UserProfile.class, profileId);
        }
        return profile;
    }

    @Null
    public Long getProfileLongId(Object uid) {
        Long profileId = null;

        if(uid instanceof Long) {
            //  uid long в чистом виде
            profileId = (Long) uid;
        } else if(uid instanceof String) {
            // патаемся найти соответствие в мапе id-шников String->Long
            profileId = socialUserIdMap.mapToNumberId(uid);
            if(profileId == null) {
                // предпологаем что в строке id типа Integer
                try {
                    profileId = (long) Integer.parseInt((String) uid);
                } catch (NumberFormatException e) {
                }
            }
        } else {
            log.error("unknown uid type! [{}]", uid.getClass());
        }
        return profileId;
    }

    /**
     * Загрузить из базы (из кеша) или создать новый профиль
     *
     * @param id profileId
     * @return (UserProfile, isNewProfile)
     */
    public Tuple2<UserProfile, Boolean> getProfileOrCreate(Long id, String[] params) {
        //загружаем из soft кеша т.к сильная ссылка будет сохронена на уровне платформы
        UserProfile userProfile = softCache.get(UserProfile.class, id);

        // если это первый вход данного игрока, то создаем новый профайл
        if(userProfile == null) {
            userProfile = userProfileCreator.createUserProfile(id);
            //кешируем объект
            softCache.put(UserProfile.class, id, userProfile);

            final String referrer = LoginService.getParam(params, ILogin.REFERRER_PARAM_NAME);
            // клиент указал по какой ссылке он установил игру
            if(!referrer.isEmpty()) {
                taskService.addTransactionTask(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                        daoService.getUserProfileDao().updateReferrer(id, referrer);
                    }
                });
            }
            return Tuple.of(userProfile, true);
        } else {
            return Tuple.of(userProfile, false);
        }
    }

    /**
     * Создаст группу червей профайла для отправки на клиент
     *
     * @param profile профайл для которого необходимо вернуть группу червей
     * @return список червей группы
     */
    public WormStructure[] createWormGroupStructures(UserProfile profile) {
        Collection<UserProfile> friendProfiles = preloadFriendProfiles(profile);

        //собираем группу червей
        List<WormStructure> wormStructures = new ArrayList<>(MAX_TEAM_MEMBERS);

        int clanId = getClanId(profile);
        for(int i = 0; i < profile.getWormsGroup().length; i++) {
            int teamMemberId = profile.getWormsGroup()[i];
            TeamMember teamMember = null;
            if(profile.getId() == teamMemberId) {
                // свой профиль
                wormStructures.add(new WormStructure(TeamMemberType.Himself, profile, skinProducer));
            } else if(isMercenary(teamMemberId)) {
                // наёмник
                teamMember = profile.getFriendTeamMember(teamMemberId);
                MercenaryBean mercenaryBean = groupService.getMercenariesConf().get(teamMemberId);
                if(mercenaryBean != null) {
                    if(teamMember != null && teamMember instanceof MercenaryTeamMember) {
                        wormStructures.add(new WormStructure(mercenaryBean, (MercenaryTeamMember) teamMember));
                    } else {
                        log.error(String.format("[%s] FriendTeamMember not found for mercenary [%s] [%s]", profile, teamMemberId, teamMember));
                    }
                } else {
                    log.error(String.format("MercenaryBean not found by id [%s]", teamMemberId));
                }
            } else {
                teamMember = profile.getFriendTeamMemberNullable(teamMemberId);
                UserProfile friendProfile = find(teamMemberId, friendProfiles);
                if(friendProfile != null) {
                    if(teamMember != null) {
                        if(teamMember instanceof FriendTeamMember) {
                            wormStructures.add(new WormStructure(friendProfile, (FriendTeamMember) teamMember));
                        } else if(teamMember instanceof SoclanTeamMember) {
                            // соклановец
                            TeamMemberType clanMemberType;
                            if(clanId > 0 && clanId == getClanId(friendProfile)) {
//                                if(friendProfile.getLevel() > profile.getLevel()) {
//                                    clanMemberType = TeamMemberType.InaccessibleClanMember;
//                                } else {
                                    clanMemberType = TeamMemberType.SoclanMember;
//                                }
                            } else {
                                clanMemberType = TeamMemberType.OtherClanMember;
                            }
                            // на случай, если соклановец стал недоступен, сразу деактивируем его на будущее
                            boolean soClanActive = teamMember.isActive() && clanMemberType.isActive();
                            if(soClanActive != teamMember.isActive()) {
                                teamMember.setActive(soClanActive);
                                profile.setTeamMembersDirty(true);
                            }
                            WormStructure wormStructure = new WormStructure(clanMemberType, friendProfile, (SoclanTeamMember) teamMember, skinProducer);
                            // обрезаем крафтовые/ачивочные шапку и артефакт
                            groupService.trimSoclanStuff(wormStructure);
                            wormStructures.add(wormStructure);
                        } else {
                            log.error(String.format("Wrong type of TeamMember [%s]", teamMember));
                        }
                    } else {
                        // нужно склонировать друга (ленивая инициализация)
                        teamMember = new FriendTeamMember(friendProfile, skinProducer);
                        profile.getTeamMembers()[i] = teamMember;
                        profile.setTeamMembersDirty(true);

                        wormStructures.add(new WormStructure(friendProfile, (FriendTeamMember) teamMember));
                    }
                }
            }
        }

        return wormStructures.toArray(new WormStructure[wormStructures.size()]);
    }

    private Collection<UserProfile> preloadFriendProfiles(UserProfile profile) {
        List<Long> wormsGroup = new ArrayList<>(MAX_TEAM_MEMBERS);
        for(int teamMemberId : profile.getWormsGroup()) {
            if(teamMemberId > 0) {
                wormsGroup.add((long) teamMemberId);
            }
        }
        Collection<UserProfile> comradeProfiles = loadProfiles(wormsGroup, false);
        if(wormsGroup.size() > comradeProfiles.size()) {
            // не все профили удалось загрузить
            log.error("Can't load {} UserProfile(s) for userProfile: {}", wormsGroup.size() - comradeProfiles.size(), profile.getId());
        }
        return comradeProfiles;
    }

    @Null
    private UserProfile find(int teamMemberId, Collection<UserProfile> comradeProfiles) {
        for(UserProfile comradeProfile : comradeProfiles) {
            if(comradeProfile.getId() == teamMemberId) {
                return comradeProfile;
            }
        }
        return null;
    }

    /**
     * Создаст структуру рофайла для отправки на клиент
     *
     * @param profile профайл для которого необходимо создать структуру
     * @return созданную структуру
     */
    public UserProfileStructure createUserProfileStructure(UserProfile profile) {
        if(!profile.isStubProfile()) {
            UserProfileStructure userProfileStructure = new UserProfileStructure(profile, createWormGroupStructures(profile), newClanMemberStructure(profile));
            userProfileStructure.rentedItems = new RentedItems(profile.getVipExpiryTime(), weaponsCreator.getInfinityWeaponsWithPrice(), stuffCreator.getVipStuff());
            return userProfileStructure;
        } else {
            // создаем структуру "заглушку"
            UserProfileStructure structure = new UserProfileStructure();
            structure.id = profile.getId();
            structure.profileStringId = profile.getProfileStringId();
            structure.wormsGroup = new WormStructure[0];
            structure.backpack = ArrayUtils.EMPTY_INT_ARRAY;
            structure.stuff = new short[0];
            structure.recipes = new short[0];
            structure.clanMember = clansEnabled ? new ClanMemberStructure(null) : null;
            structure.rentedItems = null;
            return structure;
        }
    }

    public ClanMemberStructure newClanMemberStructure(UserProfile profile) {
        if(!clansEnabled) {
            return null;
        } else if(clanSeasonService.isDiscard()) {
            return new ClanMemberStructure(null);
        } else {
            final short socialId = getSocialIdForClan(profile);
            final Long profileId = profile.getId();
            ClanMember clanMember = clanService.getClanMember(socialId, profileId.intValue());
            return new ClanMemberStructure(clanMember);
        }
    }

    public ClanMemberStructure newClanMemberStructure(long profileId) {
        if(!clansEnabled) {
            return null;
        } else if(clanSeasonService.isDiscard()) {
            return new ClanMemberStructure(null);
        } else {
            short socialId = getSocialIdForClan(getUserProfile(profileId));
            ClanMember clanMember = clanService.getClanMember(socialId, (int) profileId);
            return new ClanMemberStructure(clanMember);
        }
    }

    /**
     * непосредственно узнать придадлежность профиля к клану
     */
    public int getClanId(UserProfile profile) {
        short socialId = getSocialIdForClan(profile);
        ClanMember clanMember = clanService.getClanMember(socialId, profile.getId().intValue());
        if(clanMember != null) {
            Integer clanId = clanMember.getClanId();
            return clanId != null ? clanId : 0;
        }
        return 0;
    }

    /**
     * вернет структуру рофайла для отправки на клиент
     *
     * @param profile профайл для которого необходимо создать структуру
     * @return созданную структуру
     */
    public UserProfileStructure getUserProfileStructure(UserProfile profile) {
        if(profile.getUserProfileStructure() == null) {
            UserProfileStructure profileStructure = createUserProfileStructure(profile);
            profile.setUserProfileStructure(profileStructure);
            return profileStructure;
        } else {
            return profile.getUserProfileStructure();
        }
    }

    /**
     * вернет список структур рофайлов для отправки на клиент
     *
     * @param profiles список профайлов по которым нужно создать список структур
     * @return список структур рофайлов для отправки на клиент
     */
    public List<UserProfileStructure> getUserProfileStructures(Collection<UserProfile> profiles) {
        return profiles.stream().map(this::getUserProfileStructure).collect(Collectors.toList());
    }

    public void updateAsync(final UserProfile profile) {
        taskService.addTransactionTask(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                try {
                    update(profile);
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }
            }
        });
    }

    public void updateSync(UserProfile profile) {
        updateSync(null, profile);
    }

    public void updateSync(final Runnable task, final UserProfile profile) {
        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    if(task != null)
                        task.run();
                    update(profile);
                }
            });
        } catch (DataIntegrityViolationException | TransactionException e) {
            log.error(e.toString(), e);
        }
    }

    public void findAndUpdateAchievements(UserProfile profile){
        achieveService.findAndUpdateAchievements(getProfileAchieveId(profile.getProfileId()));
    }

    /**
     * обновит профайл в БД
     */
    private void update(final UserProfile profile) {
        synchronized (profile) {
            boolean origDirty = profile.isDirty();
            //сначало обновляем основную инфу
            daoService.getUserProfileDao().updateProfile(profile);

            if(profile.isTeamMembersDirty()) {
                daoService.getWormGroupDao().updateWormGroups(profile);
            }

            //далее обновляем список предметов
            Set<Integer> deletedWeapons = new HashSet<>();
            for(BackpackItem backpackItem : profile.getBackpack()) {
                if(backpackItem.isNewly()) {
                    // если есть что сохранять
                    if(backpackItem.isNotEmpty()) {
                        daoService.getBackpackItemDao().createBackpackItem(profile.getId(), backpackItem.getWeaponId(), backpackItem.getCount());
                        backpackItem.setNewly(false);
                    }
                } else if(backpackItem.isDirty()) {
                    if(backpackItem.isNotEmpty()) {
                        daoService.getBackpackItemDao().update(profile.getId(), backpackItem.getWeaponId(), backpackItem.getCount());
                    } else {
                        daoService.getBackpackItemDao().deleteBackpackItem(profile.getId(), backpackItem.getWeaponId());
                        deletedWeapons.add(backpackItem.getWeaponId());
                    }
                }
                backpackItem.setDirty(false);
            }
            // удаляем оружие из рюкзака
            if(deletedWeapons.size() > 0 && profile.deleteBackpackItems(deletedWeapons)) {
                // пересоздаем рюкзак отправляемый клиентам
                getUserProfileStructure(profile).backpack = UserProfileStructure.fillBackpack(profile);
            }

            reagentsDao.persist(profile.getReagents());
            trueSkillDao.persist(profile.getTrueSkillEntity());
            backpackConfDao.persist(profile.getBackpackConfs());
            if(coliseumService != null) coliseumService.persistEntity(profile.getColiseumEntity());
            if(mercenariesService != null) mercenariesService.persistEntity(profile.getMercenariesEntity());
            questService.persistEntity(profile.getQuestEntity());
            cookiesDao.persist(profile.getCookiesEntity());

            try {
                Session session = Sessions.get(profile);
                if(session != null) {
                    Short flashVersion = (Short) session.getStore().get(UserProfile.FLASH_VERSION);
                    if(flashVersion != null && flashVersion > 0 && origDirty) {
                        String updateStatQuery = "UPDATE stat.profile_stat SET flash_version = 999 WHERE profile_id=123;" +
                                "INSERT INTO stat.profile_stat (profile_id, flash_version) " +
                                "SELECT 123, 999 WHERE NOT EXISTS (SELECT 1 FROM stat.profile_stat WHERE profile_id=123);";
                        jdbcTemplate.update(updateStatQuery
                                .replaceAll("999", String.valueOf(flashVersion))
                                .replaceAll("123", profile.getId().toString())
                        );
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    //=========================== Пакетная загрузка профилей ===================================================================

    /**
     * Метод для пакетной загрузки профайлов. Грузит все профайлы из зараметра ids
     *
     * @param ids                    список id которые нужно загрузить
     * @param preloadComradeProfiles загружать ли в кеш профайлы членов комманд
     * @return список загруженных профайлов
     */
    public Collection<UserProfile> loadProfiles(Collection ids, boolean preloadComradeProfiles) {
        return loadProfiles(entityLoader, ids, -1, preloadComradeProfiles);
    }

    public Map<Long, UserProfile> loadProfilesInMap(Collection ids, boolean preloadComradeProfiles) {
        Map<Long, UserProfile> result = new HashMap<>();
        Collection<UserProfile> userProfiles = loadProfiles(entityLoader, ids, -1, preloadComradeProfiles);
        for(UserProfile userProfile : userProfiles) {
            result.put(userProfile.getId(), userProfile);
        }
        return result;
    }

    private Collection<UserProfile> loadProfiles(IUserProfileEntitiesLoader entitiesLoader, Collection ids, int limit, boolean preloadComradeProfiles) {
        List<UserProfile> result = new ArrayList<>();
        // если в списоке всего один id
        if(ids.size() == 1) {
            UserProfile profile = softCache.get(UserProfile.class, ids.iterator().next());
            if(profile != null) {
                result.add(profile);
            }
        } else {
            List<Object> needLoad = new ArrayList<>();
            loadFromCache(ids, result, needLoad);
            // если из базы нужно загрузить единственный профайл
            if(needLoad.size() == 1) {
                UserProfile profile = softCache.get(UserProfile.class, needLoad.get(0));
                if(profile != null) {
                    result.add(profile);
                }
            } else if(needLoad.size() > 1) {
                List<UserProfileEntity> userProfileEntities = entitiesLoader.loadUserProfileEntities(needLoad, limit);
                if(userProfileEntities == null || userProfileEntities.size() == 0) {
                    log.warn("Can't load  UserProfileEntities by ids: {}", needLoad);
                } else {
                    // заполняем профайлы оружием и коммандой
                    List<UserProfile> loaded = populateProfiles(userProfileEntities, preloadComradeProfiles);
                    result.addAll(loaded);
                    addAllInCache(loaded);
                }
            }
        }
        return result;
    }

    public void addAllInCache(List<UserProfile> profiles) {
        for(UserProfile profile : profiles) {
            softCache.put(UserProfile.class, profile.getId(), profile, false);
        }
    }

    /**
     * загружает из только из кеша и помещает в needLoad если в кеше нет
     *
     * @param ids      профили
     * @param result   список загруженных
     * @param needLoad список которые нужно грузить с базы
     */
    private void loadFromCache(Collection ids, Collection<UserProfile> result, List<Object> needLoad) {
        for(Object id : ids) {
            loadFromCache(id, result, needLoad);
        }
    }

    /**
     * загружает из только из кеша и помещает в  needLoad если в кеше нет
     *
     * @param id       профиль
     * @param result   список загруженных
     * @param needLoad список которые нужно грузить с базы
     */
    private void loadFromCache(Object id, Collection<UserProfile> result, List<Object> needLoad) {
        UserProfile profile = softCache.get(UserProfile.class, id, false);
        if(profile != null) {
            result.add(profile);
        } else {
            needLoad.add(id);
        }
    }

    /**
     * изменить состояние профиля извне (командой с другого сервера)
     * при успехе также выставляется присланный battleId
     */
    public boolean setBattleStateOutside(UserProfile profile, BattleState updateState, BattleState expectState, long battleId) {
        if(profile != null) {
            if(profile.compareAndSetBattleState(expectState, updateState)) {
                if(log.isDebugEnabled()) {
                    log.debug("[{}] battleId={} battleState {} -> {}", profile, battleId, expectState, updateState);
                }
                profile.setBattleId(battleId);
                return true;
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("[{}] battleId={} failure change battleState new={}, expected={}, current={}", profile, battleId, updateState, expectState, profile.getBattleState());
                }
            }
        }
        return false;
    }

    public TrueSkillEntity getTrueSkillFor(UserProfile profile) {
        return trueSkillService.getTrueSkillFor(profile);
    }

    public short getSocialIdForClan(UserProfile profile) {
        return 0;
    }

    public short getShortSocialIdFor(UserProfile profile) {
        if(profile != null) {
            SocialServiceEnum socialNet = SocialServiceEnum.valueOf(profile.getSocialId());
            if(socialNet == SocialServiceEnum.indefinite && socialUserIdMap.isMapToAllStringIds()) {
                return socialUserIdMap.getMobileMappedPlatform(profile.getProfileId()).getShortType();
            } else {
                return (short) getSocialIdFor(profile.getSocialId()).getType();
            }
        } else {
            return -1;
        }
    }

    private SocialServiceEnum getSocialIdFor(byte socialNetId) {
//        SocialServiceEnum socialNet = SocialServiceEnum.valueOf(socialNetId);
//        SocialServiceEnum defaultSocialServiceId = (SocialServiceEnum) socialService.getDefaultSocialServiceId();
//        if(socialNet == SocialServiceEnum.indefinite && defaultSocialServiceId == null) {
//            throw new IllegalStateException("ну указана социальная сеть по умолчанию 'SocialService.defaultSocialServiceId'");
//        }
//        return socialNet != SocialServiceEnum.indefinite ? socialNet : defaultSocialServiceId;
        return SocialServiceEnum.steam;
    }

    public SocialServiceEnum getDefaultSocialId() {
//        SocialServiceEnum defaultSocialServiceId = (SocialServiceEnum) socialService.getDefaultSocialServiceId();
//        if(defaultSocialServiceId == null) {
//            throw new IllegalStateException("ну указана социальная сеть по умолчанию 'SocialService.defaultSocialServiceId'");
//        }
//        return defaultSocialServiceId;
        return SocialServiceEnum.steam;
    }

    public ReagentsEntity getReagents(UserProfile profile) {
        return craftService.getReagentsForProfile(profile.getId());
    }

    interface IUserProfileEntitiesLoader {
        List<UserProfileEntity> loadUserProfileEntities(Collection ids, int limit);
    }

    private final IUserProfileEntitiesLoader entityLoader = (ids, limit) -> daoService.getUserProfileDao().getProfilesByIds(ids);

    /**
     * Создает список профайлов из списка пофайл ентити
     *
     * @param userProfileEntities    список entity профайлов игроков
     * @param preloadComradeProfiles подгружать ли в кеш профили членов комманд
     * @return список профайлов игроков
     */
    public List<UserProfile> populateProfiles(List<UserProfileEntity> userProfileEntities, boolean preloadComradeProfiles) {
        if(userProfileEntities == null || userProfileEntities.size() == 0) {
            return Collections.emptyList();
        } else {
            Set<Long> idSet = new HashSet<>(userProfileEntities.size());
            for(UserProfileEntity entity : userProfileEntities) {
                idSet.add(entity.getId());
            }

            // грузим оружие игроков одним запросом
            Map<Integer, List<BackpackItemEntity>> backpacksMap = daoService.getBackpackItemDao().getBackpackByProfileIds(idSet);
            // грузим комманды игроков одним запросом
            // отдельным запросом подгружаем профайлы членов комманд если выставлен параметр preloadComradeProfiles
            Map<Long, WormGroupsEntity> groupsMap = daoService.getWormGroupDao().getWormGroupsByProfileIds(idSet, preloadComradeProfiles);
            Map<Long, Tuple2<Integer, Byte>> rankValuesMap = daoService.getUserProfileDao().selectRankValues(idSet);

            // создаем UserProfile из UserProfileEntity
            List<UserProfile> profiles = new ArrayList<>(userProfileEntities.size());
            for(UserProfileEntity entity : userProfileEntities) {
                List<BackpackItemEntity> backpack = backpacksMap.get(entity.getId().intValue());
                backpack = backpack != null ? backpack : new ArrayList<>();

                UserProfile userProfile = newUserProfile(entity, backpack, groupsMap.get(entity.getId()), rankValuesMap.get(entity.getId()));
                String profileStringId = getProfileStringId(userProfile.getId());
                userProfile.setProfileStringId(profileStringId);
                profiles.add(userProfile);
            }
            return profiles;
        }
    }

    public UserProfile newUserProfile(UserProfileEntity userProfileEntity, List<BackpackItemEntity> backpackItemEntities, WormGroupsEntity wormGroupEntities, @Null Tuple2<Integer, Byte> rankValues) {
        if(rankValues == null)
            rankValues = Tuple.of(0, RankService.INIT_RANK_VALUE);
        UserProfile profile = new UserProfile(userProfileEntity, wormGroupEntities, rankValues);
        profile.setBackpack(userProfileCreator.initBackpack(backpackItemEntities));
        stuffService.removeExpiredStuff(profile, null, false);
        stuffService.removeOldTempStuff(profile);
        return profile;
    }

    public int getBoostFactor(UserProfile profile) {
        int vipBoostFactor = profile.isVipActive() ? vipBoostFactorValue : 1;
        return Math.max(vipBoostFactor, stuffService.getBoostValue(profile, BoostFamily.MultiplyExperience));
    }

    public int getExtraReagentCount(UserProfile profile) {
        return stuffService.getBoostValue(profile, BoostFamily.ExtraReagent);
    }

    public boolean isVipActive(UserProfile profile) {
        return stuffService.getBoostValue(profile, BoostFamily.Vip) > 0;
    }

    public Date getVipExpireDate(UserProfile profile) {
        return stuffService.getBoostExpireDate(profile, BoostFamily.Vip);
    }

    //============================ Пакетная загрузка профилей (конец)=================================================================

    /**
     * не создавать UserProfile напрямую а только из этого метода чтобы проинициализировать рюкзак
     *
     * @param entity профайл игрока загруженый из БД
     * @return модель профайла игрока
     */
    public UserProfile initProfile(UserProfileEntity entity) {
        List<BackpackItemEntity> backpack = daoService.getBackpackItemDao().getBackpackByProfileId(entity.getId());
        WormGroupsEntity group = daoService.getWormGroupDao().getWormGroupsByProfileId(entity.getId());
        Tuple2<Integer, Byte> rankValues = daoService.getUserProfileDao().selectRankValues(entity.getId());

        UserProfile userProfile = newUserProfile(entity, backpack, group, rankValues);
        String profileStringId = getProfileStringId(userProfile.getId());
        userProfile.setProfileStringId(profileStringId);
        return userProfile;
    }

    @Null
    public UserProfile getUserProfile(Long profileId) {
        return softCache.get(UserProfile.class, profileId);
    }

    public Optional<UserProfile> getUserProfileOpt(Long profileId) {
        return Optional.ofNullable(softCache.get(UserProfile.class, profileId));
    }

    @Null
    public UserProfile getUserProfile(int profileId) {
        return softCache.get(UserProfile.class, (long) profileId);
    }

    public UserProfile getUserProfile(Long profileId, boolean loadIfNeeded) {
        return softCache.get(UserProfile.class, profileId, loadIfNeeded);
    }

    public UserProfile getUserProfile(Integer profileId, boolean loadIfNeeded) {
        return softCache.get(UserProfile.class, Long.valueOf(profileId), loadIfNeeded);
    }

    public String getProfileSocialId(Long profileLongId) {
        String stringId = socialUserIdMap.mapToStringId(profileLongId);
        return stringId == null || stringId.isEmpty() ? String.valueOf(profileLongId) : stringId;
    }

    public String getProfileAchieveId(Long profileLongId) {
        String profileSocialId = getProfileSocialId(profileLongId);
        if(profileSocialId.contains("#")) {
            //allStringId
            String[] ss = profileSocialId.split("#");
            Set<Integer> mobilesNet = Arrays.stream(SocialServiceEnum.values()).filter(SocialServiceEnum::isMobileOS).map(SocialServiceEnum::getType).collect(Collectors.toSet());
            for(int i = 0; i < ss.length; i++) {
                if(mobilesNet.contains(Integer.valueOf(ss[i]))) {
                    return ss[i + 1];
                }
            }
        } else {
            return profileSocialId;
        }
        return String.valueOf(profileLongId);
    }

    public String getProfileSocialId(UserProfile profile) {
        String profileStringId = profile.getProfileStringId();
        return profileStringId != null && !profileStringId.isEmpty() ? profileStringId : String.valueOf(profile.getId());
    }

    public boolean selectRaceAndSkin(UserProfile profile, Race race, byte skinId) {
        boolean vipActive = isVipActive(profile);
        if(Race.hasRace(profile.getRaces(), race) || vipActive) {
            if(profile.getRace() != race.getShortType()) {
                // выбираем другую рассу
                if(profile.getSelectRaceTime() == 0
                        || AppUtils.currentTimeSeconds() > profile.getSelectRaceTime() + TimeUnit.HOURS.toSeconds(selectRaceMinTimeInHours)
                        || vipActive
                        ) {
                    profile.setRace(race);
                    if(vipActive) {
                        profileEventsService.fireProfileEventAsync(EXTRA, profile,
                                Param.eventType, "SELECT_RACE",
                                Param.race, race,
                                Param.vipExpireDate, getVipExpireDate(profile)
                        );
                    }
                    profile.setSelectRaceTime(AppUtils.currentTimeSeconds());
                    // выбираем также и скин
                    return skinService.setActiveSkin(profile, race, skinId);
                } else {
                    log.error("время для бесплатной смены расы ещё не пришло! Доступно будет после {}", AppUtils.formatDateInSeconds(profile.getSelectRaceTime() + (int) TimeUnit.HOURS.toSeconds(selectRaceMinTimeInHours)));
                    return false;
                }
            } else {
                // выбираем только скин
                return skinService.setActiveSkin(profile, race, skinId);
            }
        } else {
            log.error("раса [{}] у игрока отсутствует! в наличии: {}", race, Race.toList(profile.getRaces()));
            return false;
        }
    }

    public int getSelectRaceTimeLeft(UserProfile profile) {
        return profile.getSelectRaceTime() + (int) TimeUnit.HOURS.toSeconds(selectRaceMinTimeInHours) - AppUtils.currentTimeSeconds();
    }

    public int getNextSelectRaceTime(UserProfile profile) {
        return profile.getSelectRaceTime() > 0 ?
                profile.getSelectRaceTime() + (int) TimeUnit.HOURS.toSeconds(selectRaceMinTimeInHours) :
                0;
    }

    public void logLoginByReferrer(final long profileId, final String referrer, final LoginType loginType) {
        if(referrer.isEmpty()) return;
        taskService.addSimpleTask(() -> {
            try {
                restTemplate.getForObject(logLoginByReferrerUrl, String.class, serverId, loginType, referrer, profileId);
            } catch (LinkageError e) {
            } catch (Exception e) {
                log.warn("ProfileService.logLoginByReferrer: " + e.toString());
            }
        });
    }

    public boolean validateRename(int teamMemberId, String newName, UserProfile profile) {
        String oldName;
        if(teamMemberId == profile.getProfileId()) {
            // попытка переименовать себя
            oldName = profile.getName();
        } else {
            // попытка переименовать наёмника
            getUserProfileStructure(profile); // убеждаемся, что UserProfileStructure для данного профиля создан
            TeamMember teamMember = profile.getFriendTeamMemberNullable(teamMemberId);
            if(teamMember != null) {
                oldName = teamMember.getName();
                if(!teamMember.canBeRenamed()) {
                    log.error("попытка переименовать члена команды [{}:{}], который не является наёмником", teamMemberId, teamMember);
                    return false;
                }
            } else {
                log.error("попытка переименовать профиль [{}], который не находится в команде игрока [{}]", teamMemberId, profile);
                return false;
            }
        }
        if(Objects.equals(oldName, newName)) {
            log.error("игрок пытается переименовать юнита с тем же именем");
            return false;
        }
        return true;
    }

    /**
     * Переименование своего профиля
     *
     * @param profile свой профиль
     * @param newName новое имя
     * @return обновлённый профиль
     */
    public UserProfile setName(final UserProfile profile, String newName) {
        profile.setName(newName);
        ratingService.onRename(profile);

        if(profile.getClanId() > 0) {
            ClanMember clanMember = clanService.getClanMember(profile.getId().intValue());
            if(clanMember != null && !clanMember.name.equals(newName)){
                clanMember.name = newName;
                clanMember.setDirty(true);

                clanService.getDao().updateClanMember(clanMember);
            }
        }

        return profile;
    }

    public UserProfile setVipExpiryTime(final UserProfile profile, int vipExpiryTime) {
        if(profile.getVipExpiryTime() != vipExpiryTime) {
            profile.setVipExpiryTime(vipExpiryTime);
            // меняем время VIP в дневном ТОП-е
            for(DailyRatingData dailyRatingData : dailyRatingService.getDailyRatingByWager().values()) {
                RatingProfileStructure ratingProfileStructure = dailyRatingData.dailyTopMap.get(profile.getId());
                if(ratingProfileStructure != null) {
                    ratingProfileStructure.vipExpiryTime = vipExpiryTime;
                }
            }
            // меняем время VIP в глобальном ТОП-е
            OldRatingService oldRatingService = ((RatingServiceMobileImpl) this.ratingService).getRatingService();
            RatingProfileStructure ratingProfileStructure = oldRatingService.getRatingProfileStructure(profile);
            if(ratingProfileStructure != null) {
                ratingProfileStructure.vipExpiryTime = vipExpiryTime;
            }
        }
        return profile;
    }

    public boolean clearName(UserProfile profile) {
        return clearName((int) profile.getProfileId(), profile);
    }

    public boolean clearName(int teamMemberId, UserProfile profile) {
        String newName = "";
        String oldName = "";
        String member = "";

        if(!validateRename(teamMemberId, newName, profile)) {
            return false;
        }

        if(teamMemberId == profile.getProfileId()) {
            // сбрасываем имя себе
            oldName = profile.getName();
            member = "himself";
            setName(profile, newName);
        } else {
            // сбрасываем имя члену команды
            oldName = profile.getWormStructure(teamMemberId).name;
            member = "" + teamMemberId;
            setTeamMemberName(teamMemberId, newName, profile);
        }

        profileEventsService.fireProfileEventAsync(EXTRA, profile,
                Param.eventType, "clearName",
                "member", member,
                "oldName", oldName
        );
        return true;
    }

    /**
     * Переименование члена команды
     * NB: предварительно должен обязательно быть вызван метод validateRename с теми же аргументами, который, в частности, проверит существование TeamMember'а
     *
     * @param teamMemberId id члена команды
     * @param newName      новое имя
     * @param profile      профиль игрока, осуществляющего переименование члена своей команды
     * @return обновлённого члена команды
     */
    public TeamMember setTeamMemberName(int teamMemberId, String newName, UserProfile profile) {
        WormStructure wormStructure = profile.getWormStructure(teamMemberId);
        TeamMember teamMember = profile.getFriendTeamMember(teamMemberId);
        if(wormStructure != null) {
            wormStructure.name = newName;
        }
        teamMember.setName(newName);
        profile.setTeamMembersDirty(true);
        return teamMember;
    }

    private boolean isMercenary(int teamMemberId) {
        return teamMemberId < 0;
    }

    public boolean isClansEnabled() {
        return clansEnabled;
    }

}
