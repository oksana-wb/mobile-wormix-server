package com.pragmatix.achieve;

import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.achieve.mappers.WormixAchievementsMapperImpl;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import com.pragmatix.testcase.JdbcUnitTest;

import java.time.LocalDate;
import java.util.Random;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.08.2016 11:49
 */
public class AchieveDaoTest extends JdbcUnitTest {

    @Test
    public void selectByIdTest() throws Exception {
        WormixAchievementsMapperImpl mapper = new WormixAchievementsMapperImpl();
        mapper.setJdbcTemplate(jdbcTemplate());
        mapper.setTransactionTemplate(transactionTemplate());
        mapper.init();

        String profileId = RandomStringUtils.randomAlphanumeric(10);
        WormixAchievements newWormixAchievements = new WormixAchievements(profileId);
        newWormixAchievements.setInvestedAwardPoints((byte) 1);
        newWormixAchievements.setTimeSequence(2);
        newWormixAchievements.userProfileId = new Random().nextInt(10_000);

        byte[] boolAchievements = newWormixAchievements.getBoolAchievements();
        for(int i = 0; i < boolAchievements.length; i++) {
            boolAchievements[i] = (byte) i;
        }

        short[] achievements = newWormixAchievements.getAchievements();
        for(int i = 0; i < achievements.length; i++) {
            achievements[i] = (short) i;
        }

        int[] statistics = newWormixAchievements.getStatistics();
        for(int i = 0; i < statistics.length; i++) {
            statistics[i] = i;
        }

        println(newWormixAchievements.mkString());
        mapper.insertAchievements(newWormixAchievements, 10);
        WormixAchievements wormixAchievements = mapper.selectAchievements(newWormixAchievements.getProfileId());
        println(wormixAchievements.mkString());
        compareEntities(newWormixAchievements, wormixAchievements);

        boolAchievements = newWormixAchievements.getBoolAchievements();
        for(int i = 0; i < boolAchievements.length; i++) {
            boolAchievements[i] = (byte) (10 + i);
        }
        println(newWormixAchievements.mkString());
        mapper.updateAchievements(newWormixAchievements, 20);
        wormixAchievements = mapper.selectAchievements(newWormixAchievements.getProfileId());
        println(wormixAchievements.mkString());
        compareEntities(newWormixAchievements, wormixAchievements);

        mapper.wipeAchievements(profileId, profileId + "_w" + LocalDate.now());
    }

    private void compareEntities(WormixAchievements newWormixAchievements, WormixAchievements wormixAchievements) {
        Assert.assertEquals(newWormixAchievements.getInvestedAwardPoints(), wormixAchievements.getInvestedAwardPoints());
        Assert.assertEquals(newWormixAchievements.userProfileId, wormixAchievements.userProfileId);
        Assert.assertEquals(newWormixAchievements.getTimeSequence(), wormixAchievements.getTimeSequence());
        Assert.assertArrayEquals(newWormixAchievements.getBoolAchievements(), wormixAchievements.getBoolAchievements());
        Assert.assertArrayEquals(newWormixAchievements.getStatistics(), wormixAchievements.getStatistics());
        Assert.assertArrayEquals(newWormixAchievements.getAchievements(), wormixAchievements.getAchievements());
    }

}
