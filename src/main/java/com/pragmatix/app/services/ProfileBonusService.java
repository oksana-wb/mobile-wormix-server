package com.pragmatix.app.services;

import com.pragmatix.app.common.Locale;
import com.pragmatix.app.common.*;
import com.pragmatix.app.init.LevelCreator;
import com.pragmatix.app.init.StuffCreator;
import com.pragmatix.app.init.WeaponsCreator;
import com.pragmatix.app.messages.client.ILogin;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.messages.structures.login_awards.BonusDaysAward;
import com.pragmatix.app.messages.structures.login_awards.DailyBonusAward;
import com.pragmatix.app.model.*;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.services.events.*;
import com.pragmatix.app.services.rating.SeasonService;
import com.pragmatix.app.settings.*;
import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.clanserver.domain.Season;
import com.pragmatix.clanserver.services.ClanSeasonService;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.craft.domain.Reagent;
import com.pragmatix.craft.domain.ReagentsEntity;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.gameapp.sessions.Connections;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.pragmatix.app.model.Weapon.WeaponType.*;
import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.AWARD;
import static java.util.stream.Collectors.toList;

/**
 * Служебный класс для модификации свойст игрока
 * <p/>
 * Created: 26.04.11 18:59
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
@Service
public class ProfileBonusService {

    private static final Logger log = LoggerFactory.getLogger(ProfileBonusService.class);

    /**
     * выдаем приз за возвращенных игроков в количестве не более
     */
    public static final int MAX_COMEBACKED_FRIENDS = 30;

    public static int MISSION_AWARD_MAX_SHOT_COUNT = 10;

    @Autowired(required = false)
    private TopClanAwardParams topClanAwardParams;

    @Autowired(required = false)
    private SeasonService seasonService;

    @Resource
    private AppParams appParams;

    @Resource
    private DailyBonusAwardSettings dailyBonusAward;

    @Autowired(required = false)
    private ComebackBonusSettings comebackBonus;

    @Value("#{comebackCallerBonus}")
    private GenericAward comebackCallerBonus;

    @Resource
    private WeaponsCreator weaponsCreator;

    @Resource
    private WeaponService weaponService;

    @Resource
    private StuffService stuffService;

    @Resource
    private StuffCreator stuffCreator;

    @Resource
    private CraftService craftService;

    @Resource
    private StatisticService statisticService;

    @Value("${debug.awardLoginBonus.dailyBonus:false}")
    private boolean debugAwardLoginBonusDailyBonus = false;

    @Value("${debug.awardLoginBonus.bonusDays:false}")
    private boolean debugAwardLoginBonusBonusDays = false;

    @Value("${debug.awardLoginBonus.comebackedFriends:false}")
    private boolean debugAwardLoginBonusComebackedFriends = false;

    @Value("${debug.awardLoginBonus.comeback:false}")
    private boolean debugAwardLoginBonusComeback = false;

    @Value("${debug.awardLoginBonus.topClan:false}")
    private boolean debugAwardLoginBonusTopClan = false;

    @Value("${debug.awardLoginBonus.release147:false}")
    private boolean debugAwardLoginBonusRelease147 = false;

    @Autowired
    private List<GenericAward> genericAwards;

    @Autowired(required = false)
    private List<GenericAwardContainer> genericAwardContainers = new ArrayList<>();

    @Autowired(required = false)
    private List<GenericAwardFactory> genericAwardFactories = new ArrayList<>();

    @Value("${ClanSeasonService.enabled:true}")
    private boolean clanSeasonServiceEnabled = true;

    @Resource
    private ClanSeasonService clanSeasonService;

    @Resource
    private ProfileService profileService;

    @Resource
    private LevelCreator levelCreator;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private ProfileExperienceService profileExperienceService;

    @Resource
    ReactionRateService reactionRateService;

    @Resource
    SkinService skinService;

    @Resource
    private RacePriceSettings racePriceSettings;

    @Resource
    private BattleService battleService;

    // постоянное или комплексное оружие выдываемое за новый уровень
    private final Map<Integer, Map<Weapon, Integer>> levelUpWeapons = new HashMap<>();

    public ProfileBonusService() throws ParseException {
    }

    // вызывается из InitController
    public void init() {
        // необходимо чтобы распарсить строки в объекты
        for (GenericAward genericAward : genericAwards) {
            setAwardItems(genericAward.getAwardItemsStr(), genericAward.getAwardItems());
            genericAward.setReagentsMass(CraftService.parseReagentsMassString(genericAward.getReagentsMassStr()));
        }

        for (GenericAwardContainer genericAwardCotainer : genericAwardContainers) {
            for (GenericAward genericAward : genericAwardCotainer.getGenericAwards()) {
                setAwardItems(genericAward.getAwardItemsStr(), genericAward.getAwardItems());
                genericAward.setReagentsMass(CraftService.parseReagentsMassString(genericAward.getReagentsMassStr()));
            }
        }

        for (GenericAwardFactory genericAwardFactory : genericAwardFactories) {
            genericAwardFactory.init();
        }

        if (clanSeasonServiceEnabled) {
            for (TopClanAwardParams.Item itemParams : topClanAwardParams.items) {
                for (Rank rank : Rank.values()) {
                    itemParams.getItemId(rank).ifPresent(itemId -> {
                        Stuff topClanAwardItem = stuffService.getStuff(itemId);
                        if (topClanAwardItem == null) {
                            throw new IllegalStateException("Премиум предмет на закрытие сезона не найден! [" + itemId + "] " + itemParams);
                        }
                        if (!topClanAwardItem.isTemporal()) {
                            throw new IllegalStateException("Премиум предмет на закрытие сезона должен быть временным! [" + topClanAwardItem + "]");
                        }
                    });
                }
            }
            for (SeasonWeaponItem seasonWeaponItem : topClanAwardParams.seasonWeapons) {
                Weapon weapon = weaponService.getWeapon(seasonWeaponItem.weaponId);
                if (weapon == null) {
                    throw new IllegalStateException("Оружие на закрытие сезона не найдено! [" + seasonWeaponItem.weaponId + "] " + seasonWeaponItem);
                }
                if (!weapon.isConsumable()) {
                    throw new IllegalStateException("Оружие на закрытие сезона должен быть штучным! [" + weapon + "]");
                }
            }
        }
        // заполняем награды за уровень
        for (int level : levelCreator.getLevels().keySet()) {
            Level levelBean = levelCreator.getLevel(level);
            if (level > 1) {
                Map<Weapon, Integer> weapons = new TreeMap<>(levelUpWeapons.get(level - 1));
                for (AwardBackpackItem item : levelBean.getAward().getAwardItems()) {
                    if (item.getWeaponId() > 0) {
                        Weapon weapon = weaponsCreator.getWeapon(item.getWeaponId());
                        if (weapon.isType(INFINITE)) {
                            if (weapon.isInfinitely(item.getCount())) {
                                weapons.put(weapon, weapon.getInfiniteCount());
                            }
                        } else if (weapon.isType(COMPLEX)) {
                            weapons.merge(weapon, item.getCount(), Integer::sum);
                        }
                    }
                }
                levelUpWeapons.put(level, weapons);
            } else {
                levelUpWeapons.put(level, Collections.emptyMap());
            }
        }
    }

    /**
     * @param profile профиль игрока
     * @return LoginAwardStructure награду за возврат в игру после долгого отсутствия, если есть
     */
    @Null
    public LoginAwardStructure awardForComeback(UserProfile profile) {
        LoginAwardStructure result = null;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -comebackBonus.getAbsetDays());
        Date lastLoginTime = profile.getLastLoginTime();
        if (debugAwardLoginBonusComeback || (lastLoginTime != null && lastLoginTime.getTime() > 0 && lastLoginTime.before(cal.getTime()))) {
            result = new LoginAwardStructure();
            result.awardType = AwardTypeEnum.COMEBACK;
            //выдаем положенную в этом случае награду
            result.awards = awardProfile(comebackBonus, profile, AwardTypeEnum.COMEBACK, Param.lastLoginTime, lastLoginTime);
            // чтобы в базе проставилась новая дата логина
            profile.setDirty(true);
        }
        return result;
    }

    /**
     * @param profile профиль игрока
     * @return LoginAwardStructure награду за возвращенных в игру игроков, если таковые есть
     */
    @Null
    public LoginAwardStructure awardForComebackedFriends(UserProfile profile) {
        LoginAwardStructure result = null;
        if (debugAwardLoginBonusComebackedFriends && profile.getComebackedFriends() == 0) {
            profile.incComebackedFriends();
        }
        if (profile.getComebackedFriends() > 0) {
            result = new LoginAwardStructure();
            result.awardType = AwardTypeEnum.COMEBACKED_FRIEND;
            short realComebackedFrieds = profile.getComebackedFriends();
            // ограничеваем кол-во призов
            int comebackedFriends = realComebackedFrieds > MAX_COMEBACKED_FRIENDS ? MAX_COMEBACKED_FRIENDS : realComebackedFrieds;
            //выдаем положенную в этом случае награду по количеству возвращенных друзей
            List<GenericAwardStructure> awardStructureList = awardProfileSomeTimes(comebackCallerBonus, profile, result.awardType, comebackedFriends);
            profile.setComebackedFriends((short) 0);
            result.attach = String.valueOf(comebackedFriends);
            result.awards = awardStructureList;
        }
        return result;
    }

    /**
     * @param profile профиль игрока
     * @return LoginAwardStructure награду за призовое место клана
     */
    @Null
    public LoginAwardStructure awardForTopClan(UserProfile profile) {
        final AwardTypeEnum awardType = AwardTypeEnum.TOP_CLAN;
        // для тестов
        if (debugAwardLoginBonusTopClan) {
            try {
                Map<String, String> awardParams = (Map<String, String>) Connections.get().getStore().get(ILogin.DEBUG_LOGIN_AWARDS);
                int medalCount = Integer.parseInt(awardParams.get("awardForTopClan.medalCount"));
                int place = Integer.parseInt(awardParams.get("awardForTopClan.place"));
                Rank rank = Rank.valueOf(awardParams.get("awardForTopClan.rank"));
                GenericAward genericAward = fillClanGenericAward(medalCount, place, topClanAwardParams.minRating + 1, rank).get();
                String note = String.format("clanId=%s place=%s", profileService.getUserProfileStructure(profile).clanMember.getClanId(), place);
                List<GenericAwardStructure> awards = awardProfile(genericAward, profile, awardType, note);
                return new LoginAwardStructure(awardType, awards, note);
            } catch (Exception e) {
            }
        }

        if (clanSeasonService.isDiscard()) {
            return null;
        }

        Season currentSeason = clanSeasonService.getCurrentSeason();
        if (currentSeason == null) return null;

        int currentSeasonId = currentSeason.id;
        if (currentSeasonId == 1) return null;

        if (profile.isClosedClanSeasonAwardGranted(currentSeasonId - 1)) return null;

        LoginAwardStructure result = null;
        ClanSeasonService.AwardForClosedSeason award = clanSeasonService.getMembersAwardForClosedSeason(profileService.getSocialIdForClan(profile), (int) profile.getProfileId());
        if (!award.isEmpty()) {
            result = fillClanGenericAward(award.medalCount, award.place, award.seasonRating, award.rank).map(genericAward -> {
                String note = String.format("clanId=%s place=%s", award.clanId, award.place);
                List<GenericAwardStructure> awards = awardProfile(genericAward, profile, awardType, note);
                return new LoginAwardStructure(awardType, awards, note);
            }).orElse(null);
        }
        profile.setClosedClanSeasonAwardGranted(currentSeasonId - 1);

        return result;
    }

    public Optional<GenericAward> fillClanGenericAward(int medalCount, int place, int seasonRating, Rank rank) {
        if (seasonRating > topClanAwardParams.minRating) {
            GenericAward genericAward = new GenericAward();

            // выдаем призовые реагенты (medal(50),  prize_key(51), mutagen(52))
            Map<Byte, Integer> reagents = new HashMap<>(topClanAwardParams.reagents.size());
            for (byte reagentId : topClanAwardParams.reagents) {
                reagents.put(reagentId, medalCount);
            }
            if (!reagents.isEmpty()) {// если они есть
                genericAward.setReagents(reagents);
            }
            // выдаем реакцию и опыт
            genericAward.setReactionRate(medalCount * topClanAwardParams.reactionRatio); // reactionRatio = 15
            genericAward.setExperience(medalCount * topClanAwardParams.experienceRatio); // experienceRatio = 1

            // сезонное оружие берем или из конфигурации
            List<SeasonWeaponItem> seasonWeaponItems = topClanAwardParams.seasonWeapons;
            if (seasonWeaponItems.isEmpty()) {
                // или из текущего сезона
                seasonWeaponItems = seasonService.getCurrentSeasonWeaponItems();
            }
            for (SeasonWeaponItem weaponParams : seasonWeaponItems) {
                genericAward.addWeapon(weaponParams.weaponId, (int) (Math.round(medalCount * weaponParams.clanAwardParam1)) + (int) weaponParams.clanAwardParam2);
            }

            // выдаем шапки или артефакты лидеру и офицерам до окончания текущего сезона
            for (TopClanAwardParams.Item item : topClanAwardParams.items) {
                if (item.placeFrom <= place && place <= item.placeTo) {
                    item.getItemId(rank).ifPresent(itemId -> {
                        // выдаем шапку по 23:00 последнего дня текущего месяца
                        long expireTimeInSeconds = ZonedDateTime.now()
                                .plusMonths(1)
                                .withDayOfMonth(1)
                                .truncatedTo(ChronoUnit.DAYS)
                                .minusHours(1)
                                .toEpochSecond();
                        genericAward.addStuffUntilTime(itemId, (int) expireTimeInSeconds);
                    });
                    break;
                }
            }
            return Optional.of(genericAward);
        } else {
            return Optional.empty();
        }
    }

    /**
     * @param profile профиль игрока
     * @return BonusDaysAward бонусные дни, если есть
     */
    @Null
    public BonusDaysAward awardForBonusDays(UserProfile profile) {
        Date today = new Date();
        Date lastLogin = profile.getLastLoginTime() != null ? profile.getLastLoginTime() : new Date(0);

        for (var bonusPeriodSettings : appParams.getBonusPeriodSettings()) {
            if (bonusPeriodSettings.isValid() &&
                    (lastLogin.before(bonusPeriodSettings.getStartBonusDay()) || debugAwardLoginBonusBonusDays) &&
                    today.after(bonusPeriodSettings.getStartBonusDay()) && today.before(bonusPeriodSettings.getEndBonusDay()) &&
                    profile.getLevel() >= bonusPeriodSettings.getLevelMin() && profile.getLevel() <= bonusPeriodSettings.getLevelMax()
            ) {
                List<GenericAwardStructure> bonusDaysAward = awardProfile(bonusPeriodSettings, profile, AwardTypeEnum.BONUS_DAYS, "");
                String bonusMessage = bonusPeriodSettings.getBonusMessage(Locale.RU);
                String bonusMessageEn = bonusPeriodSettings.getBonusMessage(Locale.EN);
                if (profile.getLocale() == Locale.EN && bonusMessageEn != null && !bonusMessageEn.isEmpty()) {
                    bonusMessage = bonusMessageEn;
                }
                return new BonusDaysAward(bonusMessage, bonusDaysAward);
            }
        }

        return null;
    }

    //определит причитающийся ежедневный бонус
    @Null
    public DailyBonusAward getDailyBonusAward(UserProfile profile) {
        DailyBonusAward result = null;
        Date today = new Date();

        if (profile.getLoginSequence() == 0) {
            // зашел первый раз после добавления параметра loginSequence в базу
            profile.setLoginSequence((byte) 1);
        }

        boolean firstLogin = false;

        //если ещё не забрал свой ежедневный бонус
        if (!profile.isMatchPickUpDailyBonus(today) || debugAwardLoginBonusDailyBonus) {
            Calendar yesterdayCal = Calendar.getInstance();
            yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
            Date yesterday = yesterdayCal.getTime();

            Date lastLoginTime = profile.getLastLoginTime();
            Date lastLogin = lastLoginTime != null ? lastLoginTime : new Date(0);
            boolean firstLoginToday = lastLogin.getDate() != today.getDate();
            if (firstLoginToday || debugAwardLoginBonusDailyBonus) {
                byte pickUpSequence;
                // вошел в серию ежедневных бонусов и забрал вчерашний
                if (profile.isMatchPickUpDailyBonus(yesterday) || debugAwardLoginBonusDailyBonus) {
                    int maxCountedLoginSequence = dailyBonusAward.getMaxCountedLoginSequence(profile.getLevel());
                    // отслеживаем до maxCountedLoginSequence ежедневных событий
                    if (profile.getLoginSequence() + 1 <= maxCountedLoginSequence) {
                        // если ещё не упёрлись в конец максимальной недели - продвигаемся по ней
                        pickUpSequence = (byte) (profile.getLoginSequence() + 1);
                    } else {
                        // по достижении maxCountedLoginSequence => возвращаемся к началу ТЕКУЩЕЙ недели (WORMIX-4640)

                        int loginSequence = Math.min(profile.getLoginSequence(), maxCountedLoginSequence); // проверка на всякий случай (вдруг поменяли конфиги в сторону уменьшения, а у игрока в базе старое большое значение), чтобы не зациклиться на большом невалидном значении
                        pickUpSequence = dailyBonusAward.loginSeqToCurWeekStart(loginSequence);
                    }
                } else if (profile.getLoginSequence() > 1 && lastLogin.getDate() == today.getDate() - 1) {
                    // вчера не забрал бонус, но и последовательность логинов не прервал => скидываем на начало ТЕКУЩЕЙ недели (WORMIX-4640)
                    pickUpSequence = dailyBonusAward.loginSeqToCurWeekStart(profile.getLoginSequence());
                } else {
                    // серия если и была то прервалась, сбрасываем счетчик
                    pickUpSequence = (byte) 1;
                }
                profile.setLoginSequence(pickUpSequence);
                firstLogin = true;
            }

            // определяем награду за ежедневное посещение
            GenericAward dailyAward = dailyBonusAward.getAwardByLoginSeq(profile.getLoginSequence());
            if (dailyAward.getLevelMaxAward() != null && profile.getLevel() == levelCreator.getMaxLevel()) {
                dailyAward = dailyAward.getLevelMaxAward();
            }
            List<GenericAwardStructure> awardStructures = genericAwardToListOfGenericAwardStructure(dailyAward, profile);
            result = new DailyBonusAward(profile.getLoginSequence(), awardStructures);
            result.firstLogin = firstLogin;
            result.lastLogin = lastLoginTime;
        }
        return result;
    }

    public Tuple2<Short, Map<Byte, Integer>> grantBossBattleItemsAward(UserProfile profile, BossBattleWinAward bossBattleWinAward, List<GenericAwardStructure> award) {
        for (AwardBackpackItem awardBackpackItem : bossBattleWinAward.getAwardItems()) {
            if (awardBackpackItem.getWeaponId() > 0) {
                int maxShotCount = weaponService.getWeapon(awardBackpackItem.getWeaponId()).isSeasonal() ? 0 : MISSION_AWARD_MAX_SHOT_COUNT;
                int weaponCount = weaponService.addOrUpdateWeaponReturnCount(profile, awardBackpackItem.getWeaponId(), awardBackpackItem.getCount(), maxShotCount);
                award.add(new GenericAwardStructure(AwardKindEnum.WEAPON_SHOT, weaponCount, awardBackpackItem.getWeaponId()));
            } else if (awardBackpackItem.getStuffId() > 0) {
                int expireTimeInSeconds = AppUtils.currentTimeSeconds() + (int) TimeUnit.HOURS.toSeconds(awardBackpackItem.getExpireHours());
                stuffService.addStuff(profile, (short) awardBackpackItem.getStuffId(), awardBackpackItem.getExpireHours(), TimeUnit.HOURS, true, true);
                award.add(new GenericAwardStructure(AwardKindEnum.TEMPORARY_STUFF, expireTimeInSeconds, awardBackpackItem.getStuffId()));
            }
        }
        Tuple2<Short, Map<Byte, Integer>> rareItem_medals = craftService.tryHitRareItemOrReagentsInstead(profile, bossBattleWinAward.getRareAwardMassMap());
        Short rareItemId = rareItem_medals._1;
        if (rareItemId > 0) {
            stuffService.addStuff(profile, rareItemId);
            award.add(new GenericAwardStructure(AwardKindEnum.STUFF, 1, rareItemId));
        } else {
            Map<Byte, Integer> reagents = rareItem_medals._2;
            if (!reagents.isEmpty()) {
                reagents.forEach((reagent, count) -> craftService.addReagent(reagent, count, profile.getId()));
            }
        }
        return rareItem_medals;
    }

    public void fillBattleAward(int money, int realMoney, int bonusExp, int exp, List<GenericAwardStructure> award) {
        if (money != 0)
            award.add(new GenericAwardStructure(AwardKindEnum.MONEY, money));
        if (realMoney > 0)
            award.add(new GenericAwardStructure(AwardKindEnum.REAL_MONEY, realMoney));
        if (bonusExp > 0)
            award.add(new GenericAwardStructure(AwardKindEnum.BONUS_EXPERIENCE, bonusExp));
        if (exp > 0)
            award.add(new GenericAwardStructure(AwardKindEnum.EXPERIENCE, exp));
    }

    public void fillBattleAward(List<Byte> collectedReagents, List<GenericAwardStructure> award) {
        for (Byte collectedReagent : collectedReagents) {
            if (collectedReagent >= 0)
                award.add(new GenericAwardStructure(AwardKindEnum.REAGENT, 1, collectedReagent));
        }
    }

    public void fillBattleAward(byte[] collectedReagents, List<GenericAwardStructure> award) {
        for (byte collectedReagent : collectedReagents) {
            if (collectedReagent >= 0)
                award.add(new GenericAwardStructure(AwardKindEnum.REAGENT, 1, collectedReagent));
        }
    }

    /**
     * Начислит profile положенные награды заданные в award
     *
     * @param award   бин в котором заданы награды
     * @param profile профиль которому они будут начислены
     * @param times   начислить times раз
     * @return сжатый List<GenericAwardStructure> заполненный согласно настройкам заданным в award times
     */
    public List<GenericAwardStructure> awardProfileSomeTimes(GenericAward award, UserProfile profile, AwardTypeEnum awardType, int times) {
        Collection<GenericAwardProducer> awards = new ArrayList<>(times);
        for (int i = 0; i < times; i++) {
            awards.add(award);
        }
        return awardProfile(awards, profile, awardType);
    }

    public List<GenericAwardStructure> awardProfile(GenericAwardProducer award, UserProfile profile, AwardTypeEnum awardType, Object... extraStatParams) {
        BoostFactorProducer boostFactorProducer = new BoostFactorProducer(profile, profileService);
        List<GenericAwardStructure> awardStructures = awardProfile(award.getGenericAward(), profile, award.compressResult, boostFactorProducer);
        return awardAndStat(profile, awardType, awardStructures, extraStatParams);
    }

    public List<GenericAwardStructure> awardProfile(Collection<GenericAwardProducer> award, UserProfile profile, AwardTypeEnum awardType, Object... extraStatParams) {
        List<GenericAwardStructure> genericAwardStructures = new ArrayList<>();
        BoostFactorProducer boostFactorProducer = new BoostFactorProducer(profile, profileService);
        for (GenericAwardProducer genericAwardProducer : award) {
            List<GenericAwardStructure> awardStructures = awardProfile(genericAwardProducer.getGenericAward(), profile, genericAwardProducer.compressResult, boostFactorProducer);
            genericAwardStructures.addAll(awardStructures);
        }
        return awardAndStat(profile, awardType, genericAwardStructures, extraStatParams);
    }

    private List<GenericAwardStructure> awardAndStat(UserProfile profile, AwardTypeEnum awardType, List<GenericAwardStructure> genericAwardStructures, Object[] extraStatParams) {
        String note = "";
        if (ArrayUtils.isNotEmpty(extraStatParams)) {
            if (extraStatParams.length > 1) {
                HashMap<String, Object> statRow = new LinkedHashMap<>(extraStatParams.length / 2);
                ProfileEventsStatService.consumeParamPairs(extraStatParams, statRow);
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Object> entry : statRow.entrySet()) {
                    sb.append(", ").append(entry.getKey()).append("=").append(entry.getValue());
                }
                note = sb.toString().replaceFirst(", ", "");
            } else {
                note = extraStatParams[0].toString();
            }
        }
        statAward(profile, awardType, note, genericAwardStructures, extraStatParams);
        return genericAwardStructures;
    }

    /**
     * Начислит profile положенные награды заданные в award
     *
     * @param note    пояснения к награде
     * @param award   бин в котором заданы награды
     * @param profile профиль которому они будут начислены
     * @return List<GenericAwardStructure> заполненный согласно настройкам заданным в award
     */
    public List<GenericAwardStructure> awardProfile(GenericAward award, UserProfile profile, AwardTypeEnum awardType, String note) {
        BoostFactorProducer boostFactorProducer = new BoostFactorProducer(profile, profileService);
        List<GenericAwardStructure> genericAwardStructures = awardProfile(award, profile, award.compressResult, boostFactorProducer);
        statAward(profile, awardType, note, genericAwardStructures);
        return genericAwardStructures;
    }

    public List<GenericAwardStructure> genericAwardToListOfGenericAwardStructure(GenericAwardProducer award, UserProfile profile) {
        return awardProfile(award.getGenericAward(), null, award.compressResult, new BoostFactorProducer(profile, profileService));
    }

    public List<GenericAwardStructure> genericAwardToListOfGenericAwardStructure(Collection<GenericAwardProducer> award, UserProfile profile) {
        List<GenericAwardStructure> result = new ArrayList<>();
        BoostFactorProducer boostFactorProducer = new BoostFactorProducer(profile, profileService);
        for (GenericAwardProducer genericAwardProducer : award) {
            List<GenericAwardStructure> awardStructures = awardProfile(genericAwardProducer.getGenericAward(), null, genericAwardProducer.compressResult, boostFactorProducer);
            result.addAll(awardStructures);
        }
        return result;
    }

    public List<GenericAwardStructure> compress(List<GenericAwardStructure> result) {
        Map<GenericAwardStructure, GenericAwardStructure> map = new HashMap<>();
        for (GenericAwardStructure structure : result) {
            if (!map.containsKey(structure)) {
                map.put(structure, structure);
            } else {
                map.get(structure).count += structure.count;
            }
        }
        return new ArrayList<>(map.values());
    }

    private void statAward(UserProfile profile, AwardTypeEnum awardType, String note, List<GenericAwardStructure> awardStructures, Object... extraStatParams) {
        int money = 0;
        int realMoney = 0;
        List<String> items = new ArrayList<>();
        int stuffId = 0;
        int battles = 0;
        int experience = 0;
        int reaction = 0;
        int boostFactor = 1;
        Race race = null;
        int wagerWinAwardToken = 0;
        int bossWinAwardToken = 0;
        List<Integer> skins = new ArrayList<>();
        List<Byte> reagents = new ArrayList<>();
        List<String> weapons = new ArrayList<>();
        for (GenericAwardStructure awardStructure : awardStructures) {
            switch (awardStructure.awardKind) {
                case MONEY:
                    money += awardStructure.count;
                    break;
                case REAL_MONEY:
                    realMoney += awardStructure.count;
                    break;
                case STUFF:
                    items.add("" + awardStructure.itemId);
                    stuffId = awardStructure.itemId;
                    break;
                case TEMPORARY_STUFF:
                    items.add("" + awardStructure.itemId + " " + AppUtils.formatDateInSeconds(awardStructure.count));
                    stuffId = awardStructure.itemId;
                    break;
                case BATTLES_COUNT:
                    battles += awardStructure.count;
                    break;
                case EXPERIENCE:
                    experience += awardStructure.count;
                    break;
                case REACTION_RATE:
                    reaction += awardStructure.count;
                    break;
                case RACE:
                    race = Race.valueOf(awardStructure.itemId);
                    break;
                case SKIN:
                    skins.add(awardStructure.itemId);
                    break;
                case REAGENT:
                    for (int i = 0; i < awardStructure.count; i++) {
                        reagents.add((byte) awardStructure.itemId);
                    }
                    break;
                case WEAPON:
                    if (awardStructure.count == -1)
                        weapons.add("" + awardStructure.itemId);
                    break;
                case WEAPON_SHOT:
                    if (awardStructure.count > 0)
                        weapons.add("" + awardStructure.itemId + ":" + awardStructure.count);
                    break;
                case WAGER_AWARD_TOKEN:
                    wagerWinAwardToken += awardStructure.count;
                    break;
                case BOSS_AWARD_TOKEN:
                    bossWinAwardToken += awardStructure.count;
                    break;
            }
            boostFactor = Math.max(boostFactor, awardStructure.boostFactor);
        }
        if (awardType.statable) {
            // статистика в базе фиксирует только валюту и "бесконечные" предметы
            // но в качестве наград раздаются только "выстрелы" к оружию
            statisticService.awardStatistic(profile.getId(), money, realMoney, stuffId, awardType.getType(), note);
        }
        String[] backpack = null;
        if (!weapons.isEmpty()) {
            backpack = profileEventsService.fillBackpackTrimmed(profile);
        }
        Map<Reagent, Integer> reagentValues = null;
        if (!reagents.isEmpty()) {
            ReagentsEntity reagentsEntity = profile.getReagents();
            reagentValues = reagentsEntity != null ? reagentsEntity.getReagentValues() : null;
        }
        String temporalStuff = null;
        if (items.size() > 0) {
            temporalStuff = profile.getTemporalStuff().length > 0 ? TemporalStuffService.toStringTemporalStuff(profile.getTemporalStuff()) : null;
        }
        List<Race> races = null;
        if (race != null) {
            races = Race.toList(profile.getRaces());
        }
        byte[] profileSkins = null;
        if (skins != null) {
            profileSkins = profile.getSkins();
        }
        profileEventsService.fireProfileEventAsync(AWARD, profile,
                Param.eventType, awardType.name(),
                Param.money, money,
                Param.realMoney, realMoney,
                Param.items, items,
                Param.experience, experience,
                Param.reaction, reaction,
                Param.boostFactor, boostFactor > 1 ? boostFactor : null,
                Param.battles, battles,
                Param.wagerToken, wagerWinAwardToken,
                Param.bossToken, wagerWinAwardToken,
                Param.battles, battles,
                Param.reagents, ReagentsEntity.getReagentValues(reagents),
                Param.weapons, weapons,
                Param.race, race,
                Param.skins, skins,
                Param.note, ArrayUtils.isEmpty(extraStatParams) ? note : null,
                Param.extraParams, extraStatParams,
                Param.profile_reaction, reaction > 0 ? profile.getReactionRate() : 0,
                Param.profile_reactionLevel, reaction > 0 ? reactionRateService.getReactionLevel(profile.getReactionRate()) : 0,
                Param.profile_stuff, items.size() > 0 ? profile.getStuff() : null,
                Param.profile_temporalStuff, temporalStuff,
                Param.profile_backpack, backpack,
                Param.profile_reagents, reagentValues,
                Param.profile_races, races,
                Param.profile_skins, profileSkins
        );
    }

    // Выдать награды предусмотренные в наборах предметов
    public Tuple2<List<GenericAwardStructure>, Integer> issueBundle(UserProfile profile, GenericAwardStructure[] awards, int boostPeriodInDays) {
        List<GenericAwardStructure> items = new ArrayList<>();
        int realMoney = 0;
        for (GenericAwardStructure award : awards) {
            switch (award.awardKind) {
                case WEAPON:
                case WEAPON_SHOT:
                    int weaponId = award.itemId;
                    Weapon weapon = weaponService.getWeapon(weaponId);
                    if (weapon != null) {
                        if ((weapon.isSeasonal() || weapon.isConsumable()) && award.count > 0) {
                            new AddWeaponShotEvent(weaponId, award.count, -1, weaponService).runEvent(profile);
                            items.add(award);
                        } else {
                            if (!weaponService.isPresentInfinitely(profile, weaponId)) {
                                BackpackItem backpackItem = profile.getBackpackItemByWeaponId(weaponId);
                                if (backpackItem != null && weapon.isType(COMPLEX) && backpackItem.getCount() != 0 && ItemCheck.hasRealPrice(weapon)) {
                                    int realPrice = weapon.getRealprice() * Math.min(weapon.getInfiniteCount() - 1, weapon.complexWeaponLevel(backpackItem.getCount()));
                                    new AddRealMoneyEvent(realPrice).runEvent(profile);
                                    realMoney += realPrice;
                                }
                                new AddWeaponEvent(weaponId, weaponService).runEvent(profile);
                                items.add(award);
                            } else {
                                if (ItemCheck.hasRealPrice(weapon)) {
                                    int realPrice = weapon.getRealprice() * Math.max(1, weapon.getInfiniteCount());
                                    new AddRealMoneyEvent(realPrice).runEvent(profile);
                                    realMoney += realPrice;
                                }
                            }
                        }
                    }
                    break;
                case TEMPORARY_STUFF:
                case STUFF:
                    short stuffId = (short) award.itemId;
                    Stuff stuff = stuffService.getStuff(stuffId);
                    if (stuff != null) {
                        if (stuff.isBoost()) {
                            if (boostPeriodInDays <= 0) {
                                log.error("[{}] для набора содержащего бустер не указан период! {}", profile, Arrays.toString(awards));
                                return Tuple.of(Collections.emptyList(), realMoney);
                            }
                            new AddBoosterEvent(stuffId, (int) TimeUnit.DAYS.toSeconds(boostPeriodInDays), stuffService).runEvent(profile);
                            items.add(award);
                        } else if (stuff.isTemporal()) {
                            if (stuff.needLevel() > profile.getLevel()) {
                                log.error("[{}] предмет из Bundle не доступен по уровню! {} {} > {}", profile, stuff, stuff.needLevel(), profile.getLevel());
                                return Tuple.of(Collections.emptyList(), realMoney);
                            }
                            new AddTemporalStuffEvent(stuffId, 0, 0, true, false, stuffService).runEvent(profile);
                            items.add(award);
                        } else {
                            boolean setStuff = !stuff.isSticker();
                            if (!stuffService.isExist(profile, stuffId)) {
                                new AddStuffEvent(stuffId, stuffService, setStuff).runEvent(profile);
                                items.add(award);
                            } else {
                                if (ItemCheck.hasRealPrice(stuff)) {
                                    int realPrice = stuff.getRealprice();
                                    new AddRealMoneyEvent(realPrice).runEvent(profile);
                                    realMoney += realPrice;
                                }
                            }
                        }
                    }
                    break;
                case RACE:
                    Race race = Race.valueOf(award.itemId);
                    if (race != null) {
                        if (!RaceService.hasRace(profile, race)) {
                            boolean setRace = race.ordinal() > Race.valueOf(profile.getRace()).ordinal();
                            new AddRaceEvent(race).setRace(setRace).runEvent(profile);
                            items.add(award);
                        } else {
                            IItemRequirements itemRequirements = racePriceSettings.getPriceMap().get(race.type);
                            if (itemRequirements != null && ItemCheck.hasRealPrice(itemRequirements)) {
                                int realPrice = itemRequirements.needRealMoney();
                                new AddRealMoneyEvent(realPrice).runEvent(profile);
                                realMoney += realPrice;
                            }
                        }
                    }
                    break;
                case SKIN:
                    byte skinId = (byte) award.itemId;
                    SkinService.SkinMeta skinMeta = skinService.getSkinsMap().get(skinId);
                    if (skinMeta != null) {
                        if (RaceService.hasRace(profile, skinMeta.targetRace) && !skinService.haveSkin(profile, skinId)) {
                            boolean setSkin = profile.getRace() == skinMeta.targetRace.type && skinId > skinService.getSkin(profile);
                            new AddSkinEvent(skinService, skinId).setSkin(setSkin).runEvent(profile);
                            items.add(award);
                        } else {
                            if (ItemCheck.hasRealPrice(skinMeta)) {
                                int realPrice = skinMeta.needRealMoney();
                                new AddRealMoneyEvent(realPrice).runEvent(profile);
                                realMoney += realPrice;
                            }
                        }
                    }
                    break;
                case REAGENT:
                    new AddReagentEvent((byte) award.itemId, award.count, craftService).runEvent(profile);
                    items.add(award);
                    break;
                case MONEY:
                    new AddMoneyEvent(award.count, 1).runEvent(profile);
                    items.add(award);
                    break;
                case REAL_MONEY:
                    new AddRealMoneyEvent(award.count).runEvent(profile);
                    items.add(award);
                    break;
            }
        }
        return Tuple.of(items, realMoney);
    }

    private static class BoostFactorProducer {
        private UserProfile profile;
        private ProfileService profileService;

        private BoostFactorProducer(UserProfile profile, ProfileService profileService) {
            this.profile = profile;
            this.profileService = profileService;
        }

        public int getExpBoostValue() {
            return profileService.getBoostFactor(profile);
        }
    }

    /**
     * Начислит profile положенные награды заданные в award
     *
     * @param award   бин в котором заданы награды
     * @param profile профиль которому они будут начислены
     * @return List<GenericAwardStructure> заполненный согласно настройкам заданным в award
     */
    private List<GenericAwardStructure> awardProfile(GenericAward award, UserProfile profile, boolean compressResult, BoostFactorProducer boostFactorProducer) {
        List<GenericAwardStructure> result = new ArrayList<>();
        int profileLevel = profile != null ? profile.getLevel() : 1; // profile=null при вызове из genericAwardToListOfGenericAwardStructure, тут уже не можем делать никаких предположений

        if (award.getLevelMaxAward() != null && profileLevel == levelCreator.getMaxLevel()) {
            award = award.getLevelMaxAward();
        }
        if (award.getRace() != null) {
            result.add(new AddRaceEvent(award.getRace()).runEvent(profile));
        }
        for (int skinId : award.getSkins()) {
            GenericAwardStructure runEventResult = new AddSkinEvent(skinService, skinId).setSkin(award.isSetItem()).runEvent(profile);
            if (runEventResult != null)
                result.add(runEventResult);
        }
        for (AwardBackpackItem awardBackpackItem : award.getAwardItems()) {
            if (awardBackpackItem.getStuffId() > 0) {
                Stuff stuff = stuffService.getStuff((short) awardBackpackItem.getStuffId());
                if (stuff != null) {
                    if (stuff.isTemporal()) {
                        result.add(new AddTemporalStuffEvent(awardBackpackItem.getStuffId(), awardBackpackItem.getExpireHours(), awardBackpackItem.getExpireTimeInSeconds(), award.isSetItem(), true, stuffService).runEvent(profile));
                    } else {
                        result.add(new AddStuffEvent(awardBackpackItem.getStuffId(), stuffService).runEvent(profile));
                    }
                }
            }
        }
        int realMoney = getNumberRangeValue(award.getRealMoney(), award.getRealMoneyFrom(), award.getRealMoneyTo());
        if (realMoney > 0) {
            result.add(new AddRealMoneyEvent(realMoney).runEvent(profile));
            if (award.isAddExperience() && profileLevel < levelCreator.getMaxLevel())
                result.add(new AddExperienceEvent((int) Math.floor(realMoney * 100 / 4), 1, profileExperienceService).runEvent(profile));
        }
        int money = getNumberRangeValue(award.getMoney(), award.getMoneyFrom(), award.getMoneyTo());
        if (money > 0) {
            int boostFactor = award.isUseBooster() ? boostFactorProducer.getExpBoostValue() : 1;
            result.add(new AddMoneyEvent(money, boostFactor).runEvent(profile));
            if (award.isAddExperience() && profileLevel < levelCreator.getMaxLevel())
                result.add(new AddExperienceEvent((int) Math.floor(money / 4), 1, profileExperienceService).runEvent(profile));
        }
        if (award.getExperience() > 0) {
            int boostFactor = award.isUseBooster() ? boostFactorProducer.getExpBoostValue() : 1;
            result.add(new AddExperienceEvent(award.getExperience(), boostFactor, profileExperienceService).runEvent(profile));
        }
        int reactionRate = getNumberRangeValue(award.getReactionRate(), award.getReactionRateFrom(), award.getReactionRateTo());
        if (reactionRate > 0) {
            result.add(new AddReactionRateEvent(reactionRate).runEvent(profile));
        }
        if (award.getBattlesCount() > 0) {
            result.add(new AddBattlesCountEvent(battleService, award.getBattlesCount()).runEvent(profile));
        }
        if (award.getExactBattlesCount() > 0) {
            result.add(new SetBattlesCountEvent(award.getExactBattlesCount()).runEvent(profile));
        }
        if (award.getWagerWinAwardToken() > 0) {
            result.add(new AddWagerWinAwardTokenEvent(dailyRegistry, award.getWagerWinAwardToken()).runEvent(profile));
        }
        if (award.getBossWinAwardToken() > 0) {
            result.add(new AddBossWinAwardTokenEvent(dailyRegistry, award.getBossWinAwardToken()).runEvent(profile));
        }
        for (AwardBackpackItem awardBackpackItem : award.getAwardItems()) {
            if (awardBackpackItem.getWeaponId() > 0) {
                if (awardBackpackItem.getCount() > 0) {
                    GenericAwardStructure awardStructure = new AddWeaponShotEvent(awardBackpackItem.getWeaponId(), awardBackpackItem.getCount(), award.getMaxWeaponShotCount(), weaponService).runEvent(profile);
                    if (awardStructure.count > 0)
                        result.add(awardStructure);
                } else if (awardBackpackItem.getCount() == -1) {
                    GenericAwardStructure awardStructure = new AddWeaponEvent(awardBackpackItem.getWeaponId(), weaponService).runEvent(profile);
                    if (awardStructure.count == -1)
                        result.add(awardStructure);
                }
            }
        }
        if (StringUtils.isNoneEmpty(award.getSeasonWeapons())) {
            Arrays.stream(award.getSeasonWeapons().split(" "))
                    .map(s -> s.split(":")).forEach(ss -> {
                        int seasonWeapons = Integer.parseInt(ss[0]);
                        int count = Integer.parseInt(ss[1]);
                        for (int i = 0; i < seasonWeapons; i++) {
                            int[] weapons = seasonService.getCurrentSeasonWeaponsArr();
                            int weaponId = weapons[new Random().nextInt(weapons.length)];
                            GenericAwardStructure awardStructure = new AddWeaponShotEvent(weaponId, count, -1, weaponService).runEvent(profile);
                            result.add(awardStructure);
                        }
                    });
        }
        if (award.getReagentsMass().length > 0) {
            int reagentsCount = Math.max(1, award.getReagentsCount());
            int singleReagentCount = Math.max(1, getNumberRangeValue(award.getSingleReagentCount(), award.getSingleReagentCountFrom(), award.getSingleReagentCountTo()));
            for (int i = 0; i < reagentsCount; i++) {
                result.add(new AddReagentEvent(award.getReagentsMass(), singleReagentCount, craftService).runEvent(profile));
            }
        }
        if (CollectionUtils.isNotEmpty(award.getReagents().entrySet())) {
            for (Map.Entry<Byte, Integer> reagent_count : award.getReagents().entrySet()) {
                result.add(new AddReagentEvent(reagent_count.getKey(), reagent_count.getValue(), craftService).runEvent(profile));
            }
        }
        if(award.getRename() > 0){
            result.add(new AddRenameEvent(award.getRename()).runEvent(profile));
        }
        return compressResult ? compress(result) : result;
    }

    private int getNumberRangeValue(int value, int valueFrom, int valueTo) {
        if (value > 0)
            return value;
        else if (valueFrom > 0 && valueTo > valueFrom)
            return valueFrom + new Random().nextInt(valueTo - valueFrom + 1);
        else
            return 0;
    }

    public void setGenericAwardItems(String awardItemsAsString, List<GenericAwardStructure> awardItems) {
        if (awardItemsAsString == null || awardItemsAsString.trim().length() == 0) {
            return;
        }
        String[] strings = awardItemsAsString.trim().replaceAll("  ", " ").split(" ");
        for (String awardItemAsString : strings) {
            int itemId;
            int value = -1;
            if (awardItemAsString.contains(":")) {
                String[] ss = awardItemAsString.split(":");
                itemId = Integer.parseInt(ss[0]);
                value = Integer.parseInt(ss[1]);
            } else {
                itemId = Integer.parseInt(awardItemAsString);
            }
            if (ItemCheck.isWeapon(itemId)) {
                Weapon weapon = weaponsCreator.getWeapon(itemId);
                if (weapon == null) {
                    throw new IllegalArgumentException("weapon not found by id = " + itemId);
                }
                GenericAwardStructure award = new GenericAwardStructure();
                award.awardKind = value > 0 ? AwardKindEnum.WEAPON_SHOT : AwardKindEnum.WEAPON;
                award.itemId = itemId;
                award.count = value;

                awardItems.add(award);
            } else if (ItemCheck.isStuff(itemId)) {
                Stuff stuff = stuffCreator.getStuff((short) itemId);
                if (stuff == null) {
                    throw new IllegalArgumentException("stuff not found by id = " + itemId);
                }
                GenericAwardStructure award = new GenericAwardStructure();
                award.awardKind = stuff.isTemporal() ? AwardKindEnum.TEMPORARY_STUFF : AwardKindEnum.STUFF;
                award.itemId = itemId;
                if (stuff.isTemporal()) {
                    if (value > 0) {
                        award.count = value;
                    } else if (stuff.getExpireTime() > 0) {
                        award.count = stuff.getExpireTimeInHours();
                    } else {
                        throw new IllegalArgumentException("не указано время для временного предмета [" + itemId + "] в строке [" + awardItemsAsString + "]");
                    }
                }
                awardItems.add(award);
            } else {
                throw new IllegalArgumentException("illegal itemId [" + itemId + "]");
            }
        }
    }

    public void setAwardItems(String awardItemsAsString, List<AwardBackpackItem> awardItems) {
        if (awardItemsAsString == null || awardItemsAsString.trim().length() == 0) {
            return;
        }
        String[] strings = awardItemsAsString.trim().replaceAll("  ", " ").split(" ");
        for (String awardItemAsString : strings) {
            int itemId;
            int value = -1;
            if (awardItemAsString.contains(":")) {
                String[] ss = awardItemAsString.split(":");
                itemId = Integer.parseInt(ss[0]);
                value = Integer.parseInt(ss[1]);
            } else {
                itemId = Integer.parseInt(awardItemAsString);
            }
            if (ItemCheck.isWeapon(itemId)) {
                Weapon weapon = weaponsCreator.getWeapon(itemId);
                if (weapon == null) {
                    throw new IllegalArgumentException("не найдено оружие [" + itemId + "] в строке [" + awardItemsAsString + "]");
                } else if (weapon.isType(COMPLEX) && value != 1) {
                    throw new IllegalArgumentException("в наградах комплексное оружие [" + weapon + "] должно выдаваться по одному уровню за раз! Строка [" + awardItemsAsString + "]");
                }
                AwardBackpackItem awardBackpackItem = new AwardBackpackItem();
                awardBackpackItem.setWeaponId(itemId);
                awardBackpackItem.setCount(value);

                awardItems.add(awardBackpackItem);
            } else if (ItemCheck.isStuff(itemId)) {
                Stuff stuff = stuffCreator.getStuff((short) itemId);
                if (stuff == null) {
                    throw new IllegalArgumentException("не найден предмет [" + itemId + "] в строке [" + awardItemsAsString + "]");
                }
                AwardBackpackItem awardBackpackItem = new AwardBackpackItem();
                awardBackpackItem.setStuffId(itemId);
                if (stuff.isTemporal()) {
                    if (value > 0) {
                        awardBackpackItem.setExpireHours(value);
                    } else if (stuff.getExpireTime() > 0) {
                        awardBackpackItem.setExpireHours(stuff.getExpireTimeInHours());
                    } else {
                        throw new IllegalArgumentException("не указано время для временного предмета [" + itemId + "] в строке [" + awardItemAsString + "]");
                    }
                }

                awardItems.add(awardBackpackItem);
            } else {
                throw new IllegalArgumentException("не корректный id [" + itemId + "] в строке [" + awardItemAsString + "]");
            }
        }
    }

    private final Date release147 = new SimpleDateFormat("yyyy-MM-dd").parse("2018-01-20");

    // Оружие, выдаваемое за новые уровни. Если у игрока такого оружия нет, добавляем в арсенал
    // Релиз 1.47 Добавляем награду за текущий уровень, если он его взял за крайний месяц.
    public LoginAwardStructure addLevelUpWeaponsAndAwards(UserProfile profile, ThreadLocal<Boolean> needUpdate) {
        List<GenericAwardStructure> awards = new ArrayList<>();

        //==
        short release = (short) 147;
        if (profile.getReleaseAward() != release && profile.getLevelUpTime() != null && profile.getLevelUpTime().after(release147) || debugAwardLoginBonusRelease147) {
            GenericAward award = levelCreator.getLevel(profile.getLevel()).getAward().clone();
            award.getAwardItems().removeIf(awardBackpackItem -> {
                Weapon weapon = weaponsCreator.getWeapon(awardBackpackItem.getWeaponId());
                return weapon == null || !weapon.isType(CONSUMABLE);
            });

            awards.addAll(awardProfile(award, profile, AwardTypeEnum.LEVEL_UP_AWARDS, Param.note, "" + profile.getLevel()));
            if (needUpdate != null) {
                needUpdate.set(true);
            }
        }
        profile.setReleaseAward(release);
        //==

        levelUpWeapons.get(profile.getLevel()).entrySet().forEach(weapon_count -> {
            Weapon weapon = weapon_count.getKey();
            BackpackItem backpackItem = profile.getBackpackItemByWeaponId(weapon.getWeaponId());
            if (backpackItem == null || !weaponService.isPresentInfinitely(backpackItem)) {
                if (weapon.isType(INFINITE)) {
                    weaponService.addOrUpdateWeapon(profile, weapon.getWeaponId(), -1);
                    awards.add(new GenericAwardStructure(AwardKindEnum.WEAPON, -1, weapon.getWeaponId()));
                } else if (weapon.isType(COMPLEX)) {
                    int level = weapon_count.getValue();
                    int currLevel = backpackItem == null ? 0 : weapon.complexWeaponLevel(backpackItem.getCount());
                    if (level > currLevel) {
                        int count = level - currLevel;
                        weaponService.addOrUpdateWeapon(profile, weapon.getWeaponId(), count);
                        awards.add(new GenericAwardStructure(AwardKindEnum.WEAPON, count, weapon.getWeaponId()));
                    }
                }
            }
        });

        if (awards.size() > 0) {
            LoginAwardStructure result = new LoginAwardStructure();
            result.awardType = AwardTypeEnum.LEVEL_UP_WEAPONS;
            result.awards = awards;
            result.attach = "" + profile.getLevel();

            profileEventsService.fireProfileEventAsync(AWARD, profile,
                    Param.eventType, AwardTypeEnum.LEVEL_UP_WEAPONS.name(),
                    Param.weapons, awards.stream().map(item -> "" + item.itemId + (item.count == -1 ? "" : ":" + item.count)).collect(toList())
            );
            return result;
        } else {
            return null;
        }
    }

    public SimpleResultEnum pickUpDailyBonus(UserProfile profile, List<GenericAwardStructure> awards) {
        Date lastLoginTime = profile.getLastLoginTime();
        if (profile.isMatchPickUpDailyBonus(lastLoginTime) && !debugAwardLoginBonusDailyBonus) {
            // уже выдавали сегодня
            return SimpleResultEnum.ERROR;
        }

        Tuple2<Byte, Byte> weekAndDay = dailyBonusAward.toWeekAndDayNumbers(profile.getLoginSequence());
        byte week = weekAndDay._1;
        byte dayInWeek = weekAndDay._2;
        GenericAward dailyAward = dailyBonusAward.getAwardByWeekAndDay(week, dayInWeek);

        awards.addAll(
                awardProfile(dailyAward, profile, AwardTypeEnum.DAILY_BONUS, "" + week + "/" + dayInWeek)
        );

        profile.setPickUpDailyBonus(lastLoginTime);
        return SimpleResultEnum.SUCCESS;
    }

//====================== Getters and Setters =================================================================================================================================================

    public AppParams getAppParams() {
        return appParams;
    }

    public void setAppParams(AppParams appParams) {
        this.appParams = appParams;
    }

    public boolean isDebugAwardLoginBonusDailyBonus() {
        return debugAwardLoginBonusDailyBonus;
    }

    public boolean isDebugAwardLoginBonusBonusDays() {
        return debugAwardLoginBonusBonusDays;
    }

    public boolean isDebugAwardLoginBonusComebackedFriends() {
        return debugAwardLoginBonusComebackedFriends;
    }

    public boolean isDebugAwardLoginBonusComeback() {
        return debugAwardLoginBonusComeback;
    }

    public boolean isDebugAwardLoginBonusTopClan() {
        return debugAwardLoginBonusTopClan;
    }

}
