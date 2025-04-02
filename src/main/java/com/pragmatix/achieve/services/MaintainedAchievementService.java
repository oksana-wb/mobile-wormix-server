package com.pragmatix.achieve.services;

import com.pragmatix.achieve.award.AchieveAward;
import com.pragmatix.achieve.common.AchieveUtils;
import com.pragmatix.achieve.common.BonusItem;
import com.pragmatix.achieve.dao.AchieveDao;
import com.pragmatix.achieve.domain.ProfileAchievements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Null;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.07.11 18:22
 */
public class MaintainedAchievementService {

    private static final Logger log = LoggerFactory.getLogger(MaintainedAchievementService.class);

    private AchieveServiceKey achieveServiceKey;

    private Class<? extends ProfileAchievements> achieveEntityClass;

    private Map<Integer, List<AchieveAward>> awardGridMap;

    private int maxAchieveIndex;

    private Map<Integer, AchieveAward> boolAwardsMap;

    private int maxBoolAchieveIndex;

    private AchieveDao dao;

    /**
     * Призовое оружие (шапки)
     * ключ - id элемента
     * знвчение - уровень, на котором доступен выбор
     */
    private Map<Integer, BonusItem> bonusItems;

    public AchieveServiceKey getAchieveServiceKey() {
        return achieveServiceKey;
    }

    public void setAchieveServiceKey(AchieveServiceKey achieveServiceKey) {
        this.achieveServiceKey = achieveServiceKey;
    }

    public Class<? extends ProfileAchievements> getAchieveEntityClass() {
        return achieveEntityClass;
    }

    public void setAchieveEntityClass(Class<? extends ProfileAchievements> achieveEntityClass) {
        this.achieveEntityClass = achieveEntityClass;
    }

    @Null
    public List<AchieveAward> getAchieveAwards(int achieveIndex) {
        List<AchieveAward> achieveAwards = awardGridMap.get(achieveIndex);
        if(achieveAwards != null && !achieveAwards.isEmpty()) {
            return achieveAwards;
        } else {
            log.error("AchieveAwards not found (or empty) by index [{}] for {}", achieveIndex, achieveEntityClass);
            return null;
        }
    }

    @Null
    public AchieveAward getAchieveAward(int boolAchieveIndex) {
        AchieveAward achieveAward = boolAwardsMap.get(boolAchieveIndex);
        if(achieveAward != null) {
            return achieveAward;
        } else {
            log.error("AchieveAward not found (or empty) by bool index [{}] for {}", boolAchieveIndex, achieveEntityClass);
            return null;
        }
    }

    public void setAwardGrid(List<AchieveAward> awardGrid) {
        awardGridMap = new HashMap<>();
        maxAchieveIndex = 0;
        for(AchieveAward achieveAward : awardGrid) {
            int index = achieveAward.getAchievementEnum().getIndex();
            List<AchieveAward> list = awardGridMap.get(index);
            if(list == null) {
                list = new ArrayList<AchieveAward>();
                awardGridMap.put(index, list);
            }
            list.add(achieveAward);
            maxAchieveIndex = Math.max(index, maxAchieveIndex);
        }
    }

    public void setBoolAwards(List<AchieveAward> awards) {
        boolAwardsMap = new HashMap<Integer, AchieveAward>();
        maxBoolAchieveIndex = 0;
        for(AchieveAward achieveAward : awards) {
            int index = achieveAward.getBoolAchieveIndex();
            boolAwardsMap.put(index, achieveAward);
            maxBoolAchieveIndex = Math.max(index, maxBoolAchieveIndex);
        }
    }

    /**
     * Метод подсчитает и вернет количество призовых очков (всего) на остове значений достижений
     *
     * @param achievements достижения
     * @return количество призовых очков
     */
    public int countAchievePoints(ProfileAchievements achievements) {
        int count = 0;
        int lastAchieveIndex = Math.min(achievements.getAchievements().length - 1, getMaxAchieveIndex()); // count ONLY achievements that were defined in xml
        for(int achieveIndex = 0; achieveIndex <= lastAchieveIndex; achieveIndex++) {
            int achieveValue = AchieveUtils.shortToInt(achievements.getAchievements()[achieveIndex]);
            if(achieveValue > 0) {
                List<AchieveAward> achieveAwards = awardGridMap.get(achieveIndex);
                if (achieveAwards == null) {
                    log.error("Achievement #{} has no awards configured! Config bug or inconsistent profile [{}]", achieveIndex, achievements.getProfileId());
                    continue;
                }
                for(AchieveAward achieveAward : achieveAwards) {
                    if(achieveValue >= achieveAward.getProgress()) {
                        count += achieveAward.getPoints();
                    } else {
                        break;
                    }
                }
            }
        }

        // флаговые достижения
        int lastBoolAchieveIndex = Math.min(achievements.getMaxBoolAchievementIndex(), getMaxBoolAchieveIndex()); // count ONLY achievements that were defined in xml
        for(int boolAchieveIndex = 0; boolAchieveIndex <= lastBoolAchieveIndex; boolAchieveIndex++) {
            AchieveAward achieveAward = boolAwardsMap.get(boolAchieveIndex);
            if(achievements.haveBoolAchievement(boolAchieveIndex)) {
                if (achieveAward == null) {
                    log.error("Boolean achievement #{} has no award configured! Config bug or inconsistent profile [{}]", boolAchieveIndex, achievements.getProfileId());
                } else {
                    count += achieveAward.getPoints();
                }
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("achievePoints={}, investedAwardPoints={}", count, achievements.getInvestedAwardPoints());
        }
        return count;
    }

    void persist(ProfileAchievements profileAchievements){
       dao.persist(profileAchievements, countAchievePoints(profileAchievements));
    }

    /**
     * Перевести призовые очки в "очки выбора"
     */
    public int getAwardPoints(int achievePoints) {
        return achievePoints / 1000;
    }

    /**
     * вернет сколькими "очками выбора" располагает игрок
     *
     * @param achievements достижения игрока
     * @return всего заработанных "очков выбора"
     */
    public int getDeservedAwardPoints(ProfileAchievements achievements) {
        return getAwardPoints(countAchievePoints(achievements));
    }

    /**
     * вернет количество доступных "очков выбора"
     *
     * @param achievements достижения игрока
     * @return достуно "очков выбора"
     */
    public int getAvailableAwardPoints(ProfileAchievements achievements) {
        return getAwardPoints(countAchievePoints(achievements)) - achievements.getInvestedAwardPoints();
    }

    public boolean checkItemRequirements(int itemId, ProfileAchievements achievements) {
        return bonusItems.get(itemId) != null && bonusItems.get(itemId).level <= achievements.getInvestedAwardPoints() + 1;
    }

    public Map<Integer, BonusItem> getBonusItems() {
        return bonusItems;
    }

    public void setBonusItems(Map<Integer, BonusItem> bonusItems) {
        this.bonusItems = bonusItems;
    }

    public Map<Integer, List<AchieveAward>> getAwardGridMap() {
        return awardGridMap;
    }

    public Map<Integer, AchieveAward> getBoolAwardsMap() {
        return boolAwardsMap;
    }

    public int getMaxAchieveIndex() {
        return maxAchieveIndex;
    }

    public int getMaxBoolAchieveIndex() {
        return maxBoolAchieveIndex;
    }
}

