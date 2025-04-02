package com.pragmatix.app.services;

import com.pragmatix.app.settings.HeroicMissionState;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;

import static org.junit.Assert.assertNotSame;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.07.13 12:28
 */
public class HeroicMissionServiceTest extends AbstractSpringTest {

    @Resource
    private HeroicMissionService heroicMissionService;

    @Test
    public void nextTest() {
        int levelIndex = 1;
        int generateCount = 15;

        HeroicMissionState state;
        String lastMission;

        state = heroicMissionService.getHeroicMissionStates()[levelIndex];
        lastMission = state.getCurrentMission();
        System.out.println(state);
        for(int i = 0; i < generateCount; i++) {
            heroicMissionService.getDailyTask().runServiceTask();

            state = heroicMissionService.getHeroicMissionStates()[levelIndex];
            System.out.println(state);
            assertNotSame(lastMission, state.getCurrentMission());
            lastMission = state.getCurrentMission();
        }
    }

}
