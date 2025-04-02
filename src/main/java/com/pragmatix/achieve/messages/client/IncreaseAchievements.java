package com.pragmatix.achieve.messages.client;

import com.pragmatix.achieve.domain.IAchievementName;
import com.pragmatix.achieve.domain.WormixAchievements.AchievementName;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * Увеличить достиженя
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.07.11 14:58
 * @see com.pragmatix.achieve.controllers.AchieveController#onIncreaseAchievement(IncreaseAchievements, UserProfile)
 */
@Command(3003)
public class IncreaseAchievements extends SecuredCommand {

    public String sessionKey;

    /**
     * индексы достижений
     */
    public int[] achievementsIndex;

    /**
     * величины приращенй достижений соответствущих achievementsIndex
     */
    public int[] achievementsRise;

    /**
     * достижения как флаг (нет/есть)
     */
    public int[] boolAchievements = new int[0];

    /**
     * счетчик увеличения достижений
     */
    @Resize(TypeSize.UINT32)
    public long timeSequence;

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        Map<IAchievementName, Integer> achievements = new TreeMap<>();
        for(int i = 0; i < achievementsIndex.length; i++) {
            achievements.put(AchievementName.valueOf(achievementsIndex[i]), achievementsRise[i]);
        }
        return "IncreaseAchievements{" +
                "timeSequence=" + timeSequence +
                ", achievements=" + achievements +
                (ArrayUtils.isNotEmpty(boolAchievements) ? ", boolAchievements=" + Arrays.toString(boolAchievements) : "") +
                '}';
    }
}
