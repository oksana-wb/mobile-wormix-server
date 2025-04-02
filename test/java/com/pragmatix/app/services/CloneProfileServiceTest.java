package com.pragmatix.app.services;

import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.quest.dao.QuestEntity;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.05.2017 14:49
 */
public class CloneProfileServiceTest extends AbstractSpringTest {

    @Resource
    CloneProfileService cloneProfileService;

    @Test
    public void cloneUserProfileProgressTest() throws Exception {
        IntStream.rangeClosed(1, 3).forEach(i -> {
            softCache.remove(UserProfile.class, testerProfileId);

            UserProfile destProfile = getProfile(testerProfileId);
            QuestEntity questEntity = questService.getQuestEntity(destProfile);
            List<String[]> moveProfileHistory = questEntity.q3().moveProfileHistory;
            moveProfileHistory.add(new String[]{LocalDateTime.now().toString(), "" + 1, "" + i, "" + i});
            questEntity.dirty = true;

            profileService.updateSync(destProfile);
        });

    }

    @Test
    public void cloneUserProfileTest() throws Exception {
        achieveService.getProfileAchievementsOrCreateNew("" + testerProfileId, WormixAchievements.class);
        achieveService.getProfileAchievementsOrCreateNew("" + (testerProfileId - 1), WormixAchievements.class);

        UserProfile profile = getProfile(testerProfileId - 1);

        UserProfile sourceProfile = getProfile(testerProfileId);
        weaponService.setBackpackConfs(sourceProfile, new short[]{1, 2, 3}, new short[]{3, 4, 5}, new short[]{6, 7, 8}, (byte) 1);
        weaponService.setHotkeys(sourceProfile, new short[]{11, 22, 33});

        cloneProfileService.cloneProfile(profile, SocialServiceEnum.vkontakte, testerProfileId, false, cloneProfileService.masterSecureToken);

        println(profile.getBackpackConfs());
    }

}