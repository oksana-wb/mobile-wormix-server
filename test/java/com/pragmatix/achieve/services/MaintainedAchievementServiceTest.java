package com.pragmatix.achieve.services;

import com.pragmatix.achieve.award.AchieveAward;
import com.pragmatix.achieve.award.WormixAchieveAward;
import com.pragmatix.achieve.dao.AchieveDao;
import com.pragmatix.achieve.domain.IAchievementName;
import com.pragmatix.achieve.common.AchieveUtils;
import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 10.10.11 15:06
 */
public class MaintainedAchievementServiceTest extends AbstractSpringTest {

    private MaintainedAchievementService service;

    @Resource
    private AchieveDao dao;

    @Resource // autowire multiple (in case there are > 1) : http://stackoverflow.com/a/35137185/693538
    void setService(List<MaintainedAchievementService> servicesAvailable) {
        service = servicesAvailable.get(0);
    }

    @Test
    public void testAwardPoints() {
        MaintainedAchievementService service = new MaintainedAchievementService();

        ArrayList<AchieveAward> awardGrid = new ArrayList<>();
        addAchieveAward(awardGrid, 0, 100, 2);
        addAchieveAward(awardGrid, 0, 500, 10);
        addAchieveAward(awardGrid, 0, 1000, 20);

        addAchieveAward(awardGrid, 1, 1000, 4);
        addAchieveAward(awardGrid, 1, 5000, 20);
        addAchieveAward(awardGrid, 1, 10000, 40);
        service.setAwardGrid(awardGrid);
        service.setBoolAwards(Collections.<AchieveAward>emptyList());
        service.setBoolAwards(Collections.emptyList());

        WormixAchievements achievements = new WormixAchievements("profileId");

        achievements.setAchievement(0, (short) 99);
        assertEquals(0, service.countAchievePoints(achievements));

        achievements.setAchievement(0, (short) 101);
        assertEquals(2, service.countAchievePoints(achievements));

        achievements.setAchievement(0, (short) 501);
        assertEquals(12, service.countAchievePoints(achievements));

        achievements.setAchievement(0, (short) 2000);
        assertEquals(32, service.countAchievePoints(achievements));

        achievements.setAchievement(0, (short) 2000);
        achievements.setAchievement(1, (short) 20000);
        assertEquals(32 + 64, service.countAchievePoints(achievements));

    }

    private void addAchieveAward(ArrayList<AchieveAward> awardGrid, int achieveIndex, int progress, int points) {
        AchieveAward achieveAward = new WormixAchieveAward();
        achieveAward.setAchievementEnum(WormixAchievements.AchievementName.valueOf(achieveIndex));
        achieveAward.setProgress(progress);
        achieveAward.setPoints(points);
        awardGrid.add(achieveAward);
    }

    @Test
    public void testAvaliableAwardPoints() {
        ProfileAchievements profileAchievements = softCache.get(WormixAchievements.class, "" + testerProfileId);
        int avaliableAwardPoints = service.getAvailableAwardPoints(profileAchievements);
        System.out.println("avaliableAwardPoints=" + avaliableAwardPoints);
    }

    @Test
    public void testLastAchievementAddedCorrectly() {
        IAchievementName achievement = WormixAchievements.AchievementName.valueOf(WormixAchievements.MAX_ACHIEVE_INDEX);
        ProfileAchievements profileAchievements = softCache.get(WormixAchievements.class, "" + testerProfileId, true);
        int initialPoints = service.countAchievePoints(profileAchievements);
        profileAchievements.setAchievement(achievement.getIndex(), (short) 32001);

        service.persist(profileAchievements);
        softCache.remove(WormixAchievements.class, "" + testerProfileId);

        profileAchievements = softCache.get(WormixAchievements.class, "" + testerProfileId, true);
        assertEquals((short)32001, profileAchievements.getAchievement(achievement.getIndex()));
        int expectedPoints = initialPoints +
                service.getAwardGridMap().get(achievement.getIndex()).stream()
                                         .mapToInt(AchieveAward::getPoints)
                                         .sum();
        assertEquals(expectedPoints, service.countAchievePoints(profileAchievements));
    }

    @Test
    public void __info__getMaxPossiblePoints() throws InterruptedException { // не тест - просто сверяем конфиги

        Thread.sleep(100);

        int totalPoints = 0;
        // ачивки с прогрессом
        Collection<List<AchieveAward>> values = service.getAwardGridMap().values();
        for (List<AchieveAward> achieveAwards : values) {
            for (AchieveAward award : achieveAwards) {
                totalPoints += award.getPoints();
                System.out.printf("%20s:\t%-5d\t=> +%d\tpoints\n", award.getAchievementEnum(), award.getProgress(), award.getPoints());
            }
        }
        assertEquals(8030, totalPoints);

        int boolPoints = 0;
        // флаговые ачивки
        for (AchieveAward award : service.getBoolAwardsMap().values()) {
            boolPoints += award.getPoints();
            System.out.printf("#%s\t=> +%d\tpoints\n", award.getBoolAchieveIndex(), award.getPoints());
        }

        assertEquals(750, boolPoints);

        assertEquals(8780, totalPoints + boolPoints);

        WormixAchievements full = new WormixAchievements("");
        for (int i = 0; i < WormixAchievements.MAX_ACHIEVE_INDEX; i++) {
            full.setAchievement(i, AchieveUtils.intToShort(65000));
        }
        for (int i = 0; i < WormixAchievements.MAX_BOOL_ACHIEVE_INDEX; i++) {
            full.setBoolAchievement(i, true);
        }
        assertEquals(8780, service.countAchievePoints(full));
    }

}
