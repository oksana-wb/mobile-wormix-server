package com.pragmatix.app.services;

import com.pragmatix.app.init.LevelCreator;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;
import javax.annotation.Resources;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 19.01.2018 13:09
 */
public class ProfileExperienceServiceTest extends AbstractSpringTest {

    @Resource
    LevelCreator levelCreator;

    @Resource
    ProfileExperienceService experienceService;

    @Test
    public void onLevelUp() {
        UserProfile profile = getProfile(testerProfileId);
        profile.setLevel(1);
        profile.setBattlesCount(0);
        profile.setExperience(levelCreator.getLevel(1).getNextLevelExp() + 5);
        profile.setMoney(0);
        profile.setRealMoney(0);
        profileService.getReagents(profile).clean();
        profile.setBackpack(new ArrayList<>());

        experienceService.onLevelUp(profile, levelCreator.getLevel(1), levelCreator.getLevel(2));

        println("battles:" + profile.getBattlesCount());
    }
}