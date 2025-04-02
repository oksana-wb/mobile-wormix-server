package com.pragmatix.achieve.messages.server;

import com.pragmatix.achieve.domain.IAchievementName;
import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.domain.WormixAchievements.AchievementName;
import com.pragmatix.achieve.messages.client.IncreaseAchievements;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Resize;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Результат увеличения дастижения
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.07.11 15:54
 * @see com.pragmatix.achieve.controllers.AchieveController#onIncreaseAchievement(IncreaseAchievements, UserProfile)
 */
@Command(13004)
public class IncreaseAchievementsResult implements SecuredResponse {
    /**
     * индексы достижений
     */
    public int[] achievementsIndex;

    /**
     * текущее значения соответствующих достижений
     */
    public int[] achievementsValues;

    /**
     * индексы достижений переступивших порог
     */
    public List<Integer> thresholdAchievementsIndex;

    /**
     * прежнее значения достижений переступивших порог
     */
    public List<Integer> thresholdAchievementsOldValues;

    /**
     * засчитанные(подтвержденные) флаговые достижения
     */
    public List<Integer> boolAchievements;

    /**
     * идентификатор увеличения лостижений в рамках сесcии
     */
    @Resize(TypeSize.UINT32)
    public long timeSequence;

    /**
     * количество вложенных очков выбора наград
     */
    public byte investedAwardPoints;

    @Ignore
    public List<GenericAwardStructure> awards;


    public IncreaseAchievementsResult() {
    }

    public IncreaseAchievementsResult(int[] achievementsIndex, int[] achievementsValues,
                                      List<Integer> thresholdAchievementsIndex, List<Integer> thresholdAchievementsOldValues,
                                      List<Integer> boolAchievements,
                                      long timeSequence, byte investedAwardPoints) {
        this.achievementsIndex = achievementsIndex;
        this.achievementsValues = achievementsValues;
        this.thresholdAchievementsIndex = thresholdAchievementsIndex;
        this.thresholdAchievementsOldValues = thresholdAchievementsOldValues;
        this.boolAchievements = boolAchievements;
        this.timeSequence = timeSequence;
        this.investedAwardPoints = investedAwardPoints;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        Map<Object, Integer> achievements = collectAchievements(achievementsIndex, achievementsValues);
        Map<Object, Integer> thresholdAchievementsOld = collectThresholdAchievements(thresholdAchievementsIndex, thresholdAchievementsOldValues);
        return "IncreaseAchievementsResult{" +
                "timeSequence=" + timeSequence +
                (investedAwardPoints > 0 ? ", investedAwardPoints=" + investedAwardPoints : "") +
                (!achievements.isEmpty() ? ", achievements=" + achievements : "") +
                (!thresholdAchievementsOld.isEmpty() ? ", thresholdAchievementsOldValues=" + thresholdAchievementsOld : "") +
                (boolAchievements != null && !boolAchievements.isEmpty() ? ", flags=" + boolAchievements : "") +
                '}';
    }

    public static Map<Object, Integer> collectThresholdAchievements(List<Integer> thresholdAchievementsIndex, List<Integer> thresholdAchievementsOldValues) {
        Map<Object, Integer> thresholdAchievementsOld = new TreeMap<>();
        if(thresholdAchievementsIndex != null)
            for(int i = 0; i < thresholdAchievementsIndex.size(); i++) {
                AchievementName achievementName = AchievementName.valueOf(thresholdAchievementsIndex.get(i));
                thresholdAchievementsOld.put(achievementName != null ? achievementName : thresholdAchievementsIndex.get(i), thresholdAchievementsOldValues.get(i));
            }
        return thresholdAchievementsOld;
    }

    public static Map<Object, Integer> collectAchievements(int[] achievementsIndex, int[] achievementsValues) {
        Map<Object, Integer> achievements = new TreeMap<>();
        for(int i = 0; i < achievementsIndex.length; i++) {
            AchievementName achievementName = AchievementName.valueOf(achievementsIndex[i]);
            achievements.put(achievementName != null ? achievementName : achievementsIndex[i], achievementsValues[i]);
        }
        return achievements;
    }
}
