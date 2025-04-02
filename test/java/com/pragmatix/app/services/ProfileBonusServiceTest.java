package com.pragmatix.app.services;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.messages.structures.login_awards.BonusDaysAward;
import com.pragmatix.app.messages.structures.login_awards.DailyBonusAward;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.rating.RatingService;
import com.pragmatix.app.settings.AppParams;
import com.pragmatix.app.settings.BonusPeriodSettings;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.clanserver.services.ClanSeasonService;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * Created: 27.04.11 10:21
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class ProfileBonusServiceTest extends AbstractSpringTest {

    @Resource
    ProfileBonusService profileBonusService;

    @Resource
    ClanSeasonService clanSeasonService;

    @Test
    public void addLevelUpWeaponsTest() {
        UserProfile profile = getProfile(testerProfileId);
        IntStream.rangeClosed(1, 30).forEach(level -> {
            profile.setLevel(level);
            profile.setBackpack(Collections.emptyList());
            profileBonusService.addLevelUpWeaponsAndAwards(profile, null);
            println(level + ": "+profile.getBackpack());
        });
    }

    @Test
    public void closeSeasonTest() throws InterruptedException {
        clanSeasonService.closeCurrentSeason();
    }

    @Test
    public void awardForTopClanTest() throws InterruptedException {
        UserProfile profile = getProfile(testerProfileId);
        profile.setTemporalStuff(new byte[0]);

        String note = "";
        int place = 1;
        int seasonRating = 11000;
        int medalCount = 10;
        Rank rank = Rank.LEADER;

        GenericAward genericAward = profileBonusService.fillClanGenericAward(medalCount, place, seasonRating, rank).get();

        AwardTypeEnum awardType =  AwardTypeEnum.TOP_CLAN;
        LoginAwardStructure loginAwardStructure = new LoginAwardStructure(awardType, profileBonusService.awardProfile(genericAward, profile, awardType, note), note);

        System.out.println(TemporalStuffService.toStringTemporalStuff(profile.getTemporalStuff()));

        System.out.println(loginAwardStructure);
    }

    @Test
    public void testLoginSequenceBonus() {
        final BonusPeriodSettings bonusPeriodSettings = new BonusPeriodSettings(1);
        bonusPeriodSettings.setStartBonusDay(new Date(0));
        bonusPeriodSettings.setEndBonusDay(new Date(1));
        AppParams appParams = new AppParams();
        appParams.setBonusPeriodSettings(List.of(bonusPeriodSettings));
        profileBonusService.setAppParams(appParams);

        UserProfile profile;
        DailyBonusAward dailyBonusStructure;
        boolean firstLogonToday;

        // заходил позавчера
        profile = createProfile(-2);
        dailyBonusStructure = profileBonusService.getDailyBonusAward(profile);
        firstLogonToday = dailyBonusStructure.getDailyBonusType().getType() > 0;
        assertTrue(firstLogonToday);
        assertEquals(50, profile.getMoney());
        assertEquals(1, profile.getLoginSequence());

        // заходил вчера
        profile = createProfile(-1);
        dailyBonusStructure = profileBonusService.getDailyBonusAward(profile);
        firstLogonToday = dailyBonusStructure.getDailyBonusType().getType() > 0;
        assertTrue(firstLogonToday);
        assertEquals(3, profile.getReactionRate());
        assertEquals(2, profile.getLoginSequence());

        // заходил сегодня
        profile = createProfile(0);
        dailyBonusStructure = profileBonusService.getDailyBonusAward(profile);
        assertNull(dailyBonusStructure);

        // заходил 4 дня подряд
        profile = createProfile(-1);
        profile.setLoginSequence((byte) 4);
        dailyBonusStructure = profileBonusService.getDailyBonusAward(profile);
        firstLogonToday = dailyBonusStructure.getDailyBonusType().getType() > 0;
        assertTrue(firstLogonToday);
        assertTrue(profile.getMoney() == 100 || profile.getRealMoney() == 1 || profile.getBattlesCount() == 5 + 2 || profile.getReactionRate() >= 4);
        assertEquals(5, profile.getLoginSequence());

        // заходит больше чем 5 дней подряд
        profile = createProfile(-1);
        profile.setLoginSequence((byte) 5);
        dailyBonusStructure = profileBonusService.getDailyBonusAward(profile);
        firstLogonToday = dailyBonusStructure.getDailyBonusType().getType() > 0;
        assertTrue(firstLogonToday);
        // счетчик ежедневных логинов сбрасываем на 1
        assertEquals(50, profile.getMoney());
        assertEquals(1, profile.getLoginSequence());

        // заходил позавчера, после серии непрерывных логинов
        profile = createProfile(-2);
        profile.setLoginSequence((byte) 4);
        dailyBonusStructure = profileBonusService.getDailyBonusAward(profile);
        firstLogonToday = dailyBonusStructure.getDailyBonusType().getType() > 0;
        assertTrue(firstLogonToday);
        assertEquals(50, profile.getMoney());
        assertEquals(1, profile.getLoginSequence());
    }

    @Test
    public void testBonusDay() {
        final BonusPeriodSettings bonusPeriodSettings = new BonusPeriodSettings(1);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        bonusPeriodSettings.setStartBonusDay(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, 2);
        bonusPeriodSettings.setEndBonusDay(cal.getTime());
        AppParams appParams = new AppParams();
        appParams.setBonusPeriodSettings(List.of(bonusPeriodSettings));
        profileBonusService.setAppParams(appParams);
        bonusPeriodSettings.setMoney(600);
        bonusPeriodSettings.setRealMoney(6);
        bonusPeriodSettings.setExactBattlesCount(15);

        // заходил позавчера
        UserProfile profile = createProfile(-2);
        BonusDaysAward bonusDaysInfo = profileBonusService.awardForBonusDays(profile);
        DailyBonusAward dailyBonusStructure = profileBonusService.getDailyBonusAward(profile);

        boolean firstLogonToday = dailyBonusStructure.getDailyBonusType().getType() > 0;
        assertTrue(firstLogonToday);
        assertEquals(600 + 50, profile.getMoney());
        assertEquals(6, profile.getRealMoney());
        assertEquals(15 + 5, profile.getBattlesCount());
        assertEquals(1, profile.getLoginSequence());

    }

    @Test
    public void testCompress() {
        List<GenericAwardStructure> awardStructures = new ArrayList<GenericAwardStructure>();
        for(int i = 0; i < 5; i++) {
            awardStructures.add(new GenericAwardStructure(AwardKindEnum.REAGENT, 1, 0));

            awardStructures.add(new GenericAwardStructure(AwardKindEnum.REAGENT, 2, 10));
            awardStructures.add(new GenericAwardStructure(AwardKindEnum.REAGENT, 3, 10));

            awardStructures.add(new GenericAwardStructure(AwardKindEnum.MONEY, 1000));
            awardStructures.add(new GenericAwardStructure(AwardKindEnum.REAL_MONEY, 10));
            awardStructures.add(new GenericAwardStructure(AwardKindEnum.REACTION_RATE, 8));
            awardStructures.add(new GenericAwardStructure(AwardKindEnum.BATTLES_COUNT, 1));

            awardStructures.add(new GenericAwardStructure(AwardKindEnum.WEAPON_SHOT, 2, 10));
            awardStructures.add(new GenericAwardStructure(AwardKindEnum.WEAPON_SHOT, 3, 11));

            awardStructures.add(new GenericAwardStructure(AwardKindEnum.TEMPORARY_STUFF, 12, 99));
        }

        System.out.println(awardStructures);
        System.out.println(awardStructures = profileBonusService.compress(awardStructures));
        for(GenericAwardStructure awardStructure : awardStructures) {
            if(awardStructure.awardKind == AwardKindEnum.REAGENT) {
                if(awardStructure.itemId == 0) {
                    assertEquals(5, awardStructure.count);
                }
                if(awardStructure.itemId == 10) {
                    assertEquals((2 + 3) * 5, awardStructure.count);
                }
            }
            if(awardStructure.awardKind == AwardKindEnum.MONEY) {
                assertEquals(1000 * 5, awardStructure.count);
            }
            if(awardStructure.awardKind == AwardKindEnum.REAL_MONEY) {
                assertEquals(10 * 5, awardStructure.count);
            }
            if(awardStructure.awardKind == AwardKindEnum.REACTION_RATE) {
                assertEquals(8 * 5, awardStructure.count);
            }
            if(awardStructure.awardKind == AwardKindEnum.BATTLES_COUNT) {
                assertEquals(5, awardStructure.count);
            }
            if(awardStructure.awardKind == AwardKindEnum.WEAPON_SHOT) {
                if(awardStructure.itemId == 10) {
                    assertEquals(2 * 5, awardStructure.count);
                }
                if(awardStructure.itemId == 11) {
                    assertEquals(3 * 5, awardStructure.count);
                }
            }

        }
    }

    private UserProfile createProfile(int lastLogin) {
        UserProfile profile = new UserProfile(1L);
        profile.setLevel(2);
        Calendar lastLoginCal = Calendar.getInstance();
        lastLoginCal.add(Calendar.DAY_OF_YEAR, lastLogin);
        profile.setLastLoginTime(lastLoginCal.getTime());
        return profile;
    }

}
