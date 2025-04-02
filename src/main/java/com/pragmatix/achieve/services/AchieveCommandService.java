package com.pragmatix.achieve.services;

import com.pragmatix.achieve.award.AchieveAward;
import com.pragmatix.achieve.common.AchieveUtils;
import com.pragmatix.achieve.common.GrantAwardResultEnum;
import com.pragmatix.achieve.dao.AchieveDao;
import com.pragmatix.achieve.domain.IAchievementName;
import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.achieve.messages.client.BuyResetBonusItems;
import com.pragmatix.achieve.messages.client.ChooseBonusItem;
import com.pragmatix.achieve.messages.client.GetAchievements;
import com.pragmatix.achieve.messages.client.IncreaseAchievements;
import com.pragmatix.achieve.messages.server.GetAchievementsResult;
import com.pragmatix.achieve.messages.server.IncreaseAchievementsResult;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.services.ProfileEventsService;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.intercom.service.MainServerAPI;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum.ACHIEVEMENTS;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.07.11 18:15
 */
@Component
public class AchieveCommandService {

    private static final Logger log = LoggerFactory.getLogger(AchieveCommandService.class);

    @Resource
    private SoftCache softCache;

    private Map<Class, MaintainedAchievementService> maintainedAchievementServicesMap;

    @Resource
    private AchieveDao achieveDao;

    @Resource
    private MainServerAPI mainAppService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Autowired(required = true)
    public void setMaintainedAchievementServices(Set<MaintainedAchievementService> maintainedAchievementServices) {
        maintainedAchievementServicesMap = new ConcurrentHashMap<>();
        for(MaintainedAchievementService service : maintainedAchievementServices) {
            // MaintainedAchievementService сласс прототип тоже подаётся в этот метод, и таким образом мы его исключаем
            // здесь небольшое противоречие в случае обслуживании нескольхих соц. сетей одновременно
            // т.к. у всех сервисов достижений вормикса идентичный achieveEntityClas
            if(service.getAchieveServiceKey() != null) {
                maintainedAchievementServicesMap.put(service.getAchieveEntityClass(), service);
            }
        }
    }

    public GetAchievementsResult getAchievements(final GetAchievements msg, final ProfileAchievements profileAchievements) {
        if(StringUtils.isEmpty(msg.profileId) || msg.profileId.equals(profileAchievements.getProfileId())) {
            return getAchievementsResult(profileAchievements, true);
        } else {
            ProfileAchievements achievementsEntity = getProfileAchievementsOrCreateNew(msg.profileId, profileAchievements.getClass());
            if(achievementsEntity != null) {
                return getAchievementsResult(achievementsEntity, false);
            } else {
                log.info("Achievements not found for profileId {}", msg.profileId);
                return new GetAchievementsResult(msg.profileId);
            }
        }
    }

    public ProfileAchievements getProfileAchievements(String profileId) {
        return softCache.get(WormixAchievements.class, profileId);
    }

    public GetAchievementsResult getAchievementsResult(ProfileAchievements userProfile, boolean fillTimeSequence) {
        int maxLen = userProfile.getAchievements().length + userProfile.getStatistics().length;
        List<Integer> achievementsIndex = new ArrayList<>(maxLen);
        List<Integer> achievementsValues = new ArrayList<>(maxLen);

        fillResultAchieveMap(achievementsIndex, achievementsValues, userProfile.getAchievements());
        fillResultAchieveMap(achievementsIndex, achievementsValues, userProfile.getStatistics());

        return new GetAchievementsResult(userProfile.getProfileId(), userProfile.getInvestedAwardPoints(),
                achievementsIndex, achievementsValues,
                userProfile.getTrueFlags(), fillTimeSequence ? userProfile.getTimeSequence() : 0);
    }

    private void fillResultAchieveMap(List<Integer> achievementsIndex, List<Integer> achievementsValues, short[] achievements) {
        for(int index = 0; index < achievements.length; index++) {
            int value = AchieveUtils.shortToInt(achievements[index]);
            if(value > 0) {
                achievementsIndex.add(index);
                achievementsValues.add(value);
            }
        }
    }

    private void fillResultAchieveMap(List<Integer> achievementsIndex, List<Integer> achievementsValues, int[] statistics) {
        for(int index = 0; index < statistics.length; index++) {
            int value = statistics[index];
            if(value > 0) {
                achievementsIndex.add(index + ProfileAchievements.STAT_FIRST_INDEX);
                achievementsValues.add(value);
            }
        }
    }

    public void setAchievements(String profileId, boolean cleanInvestedAwardPoints, int[] achievementsIndex, int[] achievementsValues, Map<Integer, Boolean> boolAchievements, Class<? extends ProfileAchievements> aClass) {
        ProfileAchievements profileAchievements = softCache.get(aClass, profileId);
        if(profileAchievements != null) {
            for(int i = 0; i < achievementsIndex.length; i++) {
                int index = achievementsIndex[i];
                int newValue = achievementsValues[i];
                profileAchievements.setAchievement(index, newValue);
            }
            if(cleanInvestedAwardPoints) {
                byte oldInvestedAwardPoints = profileAchievements.getInvestedAwardPoints();
                profileAchievements.setInvestedAwardPoints((byte) 0);
                profileEventsService.fireAchieveEventAsync(ACHIEVEMENTS, profileAchievements,
                        Param.eventType, "CLEAN_INVESTED_POINTS",
                        "investedAwardPoints", -oldInvestedAwardPoints
                );
            }
            for(Map.Entry<Integer, Boolean> entry : boolAchievements.entrySet()) {
                profileAchievements.setBoolAchievement(entry.getKey(), entry.getValue());
            }
            int achievePoints = getService(profileAchievements).countAchievePoints(profileAchievements);
            achieveDao.persist(profileAchievements, achievePoints);
        }
    }

    public IncreaseAchievementsResult increaseAchievements(final IncreaseAchievements msg, final ProfileAchievements profileAchievements) {
        // нужно выдать призы
        List<AchieveAward> needGrantAwards = new ArrayList<>();

        MaintainedAchievementService service = getService(profileAchievements);

        // массив новых значений
        int[] achievementsValues = new int[msg.achievementsIndex.length];
        // массив прежних значений
        Map<Integer, Integer> achievementsOldValues = new HashMap<>();
        // засчитанные флаговые достижения
        List<Integer> boolAchivs = new ArrayList<>(msg.boolAchievements.length);

        // достижения с прогрессом
        for(int i = 0; i < msg.achievementsIndex.length; i++) {
            int index = msg.achievementsIndex[i];
            int oldValue = profileAchievements.getAchievement(index);
            int newValue = oldValue + AchieveUtils.normalize(msg.achievementsRise[i]);
            achievementsValues[i] = newValue;
            achievementsOldValues.put(index, oldValue);

            IAchievementName achievementName = profileAchievements.getAchievementNameByIndex(index);
            if(achievementName == null || index > service.getMaxAchieveIndex() && !achievementName.isStat()) {
                log.error("достижение с индексом [{}] не зарегистрировано!", index);
                continue;
            }
            if(!achievementName.isStat()) {
                List<AchieveAward> achieveAwards = service.getAchieveAwards(index);
                if(achieveAwards != null) {
                    for(AchieveAward achieveAward : achieveAwards) {
                        // пересекли порог
                        if(oldValue < achieveAward.getProgress() && newValue >= achieveAward.getProgress()) {
                            // нужно выдать приз
                            needGrantAwards.add(achieveAward);
                        }
                    }
                }
            }
        }

        // достижения без прогресса, по факту
        int maxBoolAchieveIndex = Math.min(profileAchievements.getMaxBoolAchievementIndex(), service.getMaxBoolAchieveIndex());
        for(int boolAchievementIndex : msg.boolAchievements) {
            if(boolAchievementIndex < 0 || boolAchievementIndex > maxBoolAchieveIndex) {
                log.error("флаговое достижение с индексом [{}] не зарегистрировано!", boolAchievementIndex);
                continue;
            }
            if(!profileAchievements.haveBoolAchievement(boolAchievementIndex)) {
                boolAchivs.add(boolAchievementIndex);
                AchieveAward achieveAward = service.getAchieveAward(boolAchievementIndex);
                if(achieveAward != null) {
                    // нужно выдать приз
                    needGrantAwards.add(achieveAward);
                }
            } else {
                log.error("повторно выставляется флаговое достижение [{}]", boolAchievementIndex);
            }
        }

        IncreaseAchievementsResult increaseAchievementResult = null;

        // нужно выдать приз(ы)
        if(needGrantAwards.size() > 0) {
            mainAppService.grantAchieveAward(profileAchievements, msg, needGrantAwards);

            increaseAchievementResult = getIncreaseAchievementsResult(msg.achievementsIndex, msg.timeSequence, profileAchievements, needGrantAwards,
                    achievementsValues, achievementsOldValues, boolAchivs, GrantAwardResultEnum.OK);
        } else {
            // увеличиваем достижения с прогрессом
            for(int i = 0; i < achievementsValues.length; i++) {
                int index = msg.achievementsIndex[i];
                profileAchievements.setAchievement(index, achievementsValues[i]);
                achievementsValues[i] = profileAchievements.getAchievement(index);
            }
            // увеличиваем достижения без прогресса
            for(Integer boolAchiv : boolAchivs) {
                profileAchievements.setBoolAchievement(boolAchiv, true);
            }
            increaseAchievementResult = new IncreaseAchievementsResult(msg.achievementsIndex, achievementsValues,
                    null, null, boolAchivs,
                    msg.timeSequence, profileAchievements.getInvestedAwardPoints());
        }
        // запоминаем
        profileAchievements.setLastIncreaseAchievementsResult(increaseAchievementResult);
        return increaseAchievementResult;
    }

    public IncreaseAchievementsResult getIncreaseAchievementsResult(int[] achievementsIndex, long timeSequence, ProfileAchievements profileAchievements, List<AchieveAward> needGrantAwards,
                                                                    int[] achievementsValues, Map<Integer, Integer> achievementsOldValues,
                                                                    List<Integer> boolAchivs, GrantAwardResultEnum grantAwardResult) {
        if(grantAwardResult != GrantAwardResultEnum.OK) {
            log.error("application did not confirm grant of awards {} and return error [{}]", needGrantAwards, grantAwardResult);
        }
        // перебираем массив новых значений
        for(int i = 0; i < achievementsValues.length; i++) {
            int index = achievementsIndex[i];
            // если нужно было выдать приз, но сервер не подтвердил операцию
            AchieveAward needGrantAward = isNeedGrantAwards(needGrantAwards, index);
            if(needGrantAward != null && grantAwardResult != GrantAwardResultEnum.OK) {
                // вернем клиенту значение наименьшего порога - 1
                achievementsValues[i] = needGrantAward.getProgress() - 1;
            }
            profileAchievements.setAchievement(index, achievementsValues[i]);
            achievementsValues[i] = profileAchievements.getAchievement(index);
        }

        int thresholdAchievementsSize = grantAwardResult == GrantAwardResultEnum.OK ? needGrantAwards.size() : 0;
        Set<Integer> thresholdAchievementsIndexSet = new HashSet<>();
        List<Integer> thresholdAchievementsIndex = new ArrayList<>(thresholdAchievementsSize);
        List<Integer> thresholdAchievementsOldValues = new ArrayList<>(thresholdAchievementsSize);
        if(grantAwardResult == GrantAwardResultEnum.OK) {
            for(AchieveAward achieveAward : needGrantAwards) {
                if(achieveAward.getAchievementEnum() != null) {
                    int index = achieveAward.getAchievementEnum().getIndex();
                    if(thresholdAchievementsIndexSet.add(index)) {
                        thresholdAchievementsIndex.add(index);
                        thresholdAchievementsOldValues.add(achievementsOldValues.get(index));
                    }
                }
            }
            // достижения без прогресса
            // засчитываем достижения только если сервер подтвердил выдачу награды
            for(Integer boolAchiv : boolAchivs) {
                profileAchievements.setBoolAchievement(boolAchiv, true);
            }
        } else {
            boolAchivs = new ArrayList<>();
        }
        // фиксируем в статистике изменение баланса очков достижений
        profileEventsService.fireAchieveEventAsync(ACHIEVEMENTS, profileAchievements,
                Param.eventType, "INCREASE_ACHIEVEMENTS",
                "timeSequence", timeSequence,
                "achievements", IncreaseAchievementsResult.collectAchievements(achievementsIndex, achievementsValues),
                "thresholdAchievementsOldValues", IncreaseAchievementsResult.collectThresholdAchievements(thresholdAchievementsIndex, thresholdAchievementsOldValues),
                "boolAchievements", boolAchivs
        );
        return new IncreaseAchievementsResult(achievementsIndex, achievementsValues, thresholdAchievementsIndex, thresholdAchievementsOldValues,
                boolAchivs, timeSequence, profileAchievements.getInvestedAwardPoints());
    }

    //вернет приз с наименьшим порогом, если призов за одну ачивку было несколько
    @Null
    private AchieveAward isNeedGrantAwards(List<AchieveAward> needGrantAwards, int index) {
        for(AchieveAward needGrantAward : needGrantAwards) {
            if(needGrantAward.getAchievementEnum() != null && needGrantAward.getAchievementEnum().getIndex() == index) {
                return needGrantAward;
            }
        }
        return null;
    }

    public MaintainedAchievementService getService(ProfileAchievements userProfile) {
        return maintainedAchievementServicesMap.get(userProfile.getClass());
    }

    public ShopResultEnum giveBonusItem(ChooseBonusItem msg, final ProfileAchievements profileAchievements) {
        MaintainedAchievementService service = getService(profileAchievements);
        ShopResultEnum resultEnum;
        if(service.getAvailableAwardPoints(profileAchievements) <= 0) {
            resultEnum = ShopResultEnum.NOT_ENOUGH_MONEY;
        } else if(!service.checkItemRequirements(msg.itemId, profileAchievements)) {
            resultEnum = ShopResultEnum.MIN_REQUIREMENTS_ERROR;
        } else {
            GrantAwardResultEnum grantAwardResult = mainAppService.giveBonusItem(profileAchievements, msg, 10000 + msg.itemId);
            if(grantAwardResult != GrantAwardResultEnum.OK) {
                log.error("application did not confirm grant of awards [{}] and return error [{}]", msg.itemId, grantAwardResult);
                resultEnum = grantAwardResult == GrantAwardResultEnum.MIN_REQUIREMENTS_ERROR ? ShopResultEnum.MIN_REQUIREMENTS_ERROR : ShopResultEnum.ERROR;
            } else {
                profileAchievements.setInvestedAwardPoints((byte) (profileAchievements.getInvestedAwardPoints() + 1));
                resultEnum = ShopResultEnum.SUCCESS;
            }
        }
        return resultEnum;
    }

    public ShopResultEnum buyResetBonusItems(final BuyResetBonusItems msg, final ProfileAchievements profileAchievements) {
        ShopResultEnum resultEnum;
        if(profileAchievements.getInvestedAwardPoints() <= 0) {
            resultEnum = ShopResultEnum.MIN_REQUIREMENTS_ERROR;
        } else {
            resultEnum = mainAppService.buyResetBonusItems(profileAchievements, msg);
            if(resultEnum != ShopResultEnum.SUCCESS) {
                log.error("[{}] application did not confirm purchase reset of bonus items and return error [{}]", profileAchievements.getId(), resultEnum);
            } else {
                byte oldInvestedAwardPoints = profileAchievements.getInvestedAwardPoints();
                profileAchievements.setInvestedAwardPoints((byte) 0);
                profileEventsService.fireAchieveEventAsync(ACHIEVEMENTS, profileAchievements,
                        Param.eventType, "CLEAN_INVESTED_POINTS",
                        "investedAwardPoints", -oldInvestedAwardPoints
                );
            }
        }
        return resultEnum;
    }

    public int getAchievePoints(ProfileAchievements profileAchievements) {
        MaintainedAchievementService service = getService(profileAchievements);
        if(service != null) {
            return service.countAchievePoints(profileAchievements);
        } else {
            log.error("getService for {}: {} was null, cannot get achieve points", profileAchievements, profileAchievements.getClass());
            return 0;
        }
    }

    public boolean wipeAchievements(String profileId, Class<? extends ProfileAchievements> aClass) {
        SimpleDateFormat sdf = new SimpleDateFormat("'_w'yyMMddHHmm");
        ProfileAchievements profileAchievements = softCache.get(aClass, profileId, false);
        if(profileAchievements != null) {
            int achievePoints = getService(profileAchievements).countAchievePoints(profileAchievements);
            achieveDao.update(profileAchievements, achievePoints, aClass);
            softCache.remove(aClass, profileAchievements.getProfileId());
        }
        return achieveDao.wipe(profileId, profileId + sdf.format(new Date()), aClass);
    }

    public void findAndUpdateAchievements(String profileId){
        ProfileAchievements profileAchievements = getProfileAchievements(profileId);
        if(profileAchievements != null) {
            int achievePoints = getService(profileAchievements).countAchievePoints(profileAchievements);
            achieveDao.update(profileAchievements, achievePoints, WormixAchievements.class);
        } else {
            log.error("[{}] достижения не найдены!", profileId);
        }
    }

    public boolean setInvestedAwardPoints(String profileId, Class<? extends ProfileAchievements> aClass, byte newInvestedAwardPoints) {
        ProfileAchievements profileAchievements = softCache.get(aClass, profileId, false);
        if(profileAchievements != null) {
            byte oldInvestedAwardPoints = profileAchievements.getInvestedAwardPoints();
            profileAchievements.setInvestedAwardPoints(newInvestedAwardPoints);
            boolean result = newInvestedAwardPoints != oldInvestedAwardPoints;
            if(result) {
                log.warn(String.format("investedAwardPoints was setted directly for user [%s]: new value [%s], old value [%s]", profileId, newInvestedAwardPoints, oldInvestedAwardPoints));
                profileEventsService.fireAchieveEventAsync(ACHIEVEMENTS, profileAchievements,
                        Param.eventType, "SET_INVESTED_POINTS",
                        "investedAwardPoints", newInvestedAwardPoints - oldInvestedAwardPoints);
            }
            return result;
        }
        return false;
    }

    @Null
    public ProfileAchievements getProfileAchievementsOrCreateNew(String id, Class<? extends ProfileAchievements> aClass) {
        ProfileAchievements profile = softCache.get(aClass, id);
        // если это первый вход данного игрока, то создаем новый профайл
        if(profile == null) {
            profile = createProfileAchievements(id, aClass);
            if(profile != null) {
                int achievePoints = getService(profile).countAchievePoints(profile);
                boolean insertResult = achieveDao.persist(profile, achievePoints,false);
                if(!insertResult) {
                    log.error("[{}] Ошибка при сохранении нового профиля!", id);
                    return null;
                }
                //кешируем объект
                softCache.put(aClass, id, profile);
            } else {
                log.error("[{}] Error create achievements!", id);
            }
        }
        return profile;
    }

    public Optional<WormixAchievements> getProfileAchievementsOpt(String id) {
        Optional<WormixAchievements> wormixAchievements = Optional.ofNullable(softCache.get(WormixAchievements.class, id));
        if(!wormixAchievements.isPresent()){
            log.error("[{}] достижения не найдены!", id);
        }
        return wormixAchievements;
    }

    @Null
    public ProfileAchievements createProfileAchievements(String id, Class<? extends ProfileAchievements> aClass) {
        try {
            Constructor<? extends ProfileAchievements> constructor = aClass.getConstructor(String.class);
            ProfileAchievements achievementsEntity = constructor.newInstance(id);
            achievementsEntity.setNewly(true);
            return achievementsEntity;
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        return null;
    }

}
