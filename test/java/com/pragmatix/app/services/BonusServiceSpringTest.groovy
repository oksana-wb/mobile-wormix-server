package com.pragmatix.app.services;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.init.LevelCreator;
import com.pragmatix.app.messages.client.GetDailyBonus;
import com.pragmatix.app.messages.server.GetDailyBonusResult;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.login_awards.DailyBonusAward;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.rating.SeasonService
import com.pragmatix.app.settings.ComebackBonusSettings;
import com.pragmatix.app.settings.DailyBonusAwardSettings;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.testcase.AbstractSpringTest;
import org.apache.commons.lang3.StringUtils
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class BonusServiceSpringTest extends AbstractSpringTest {

    private UserProfile profile;

    @Resource
    ProfileBonusService profileBonusService;

    @Resource
    private DailyBonusAwardSettings dailyBonusAwardSettings;

    @Resource
    private SeasonService seasonService;

    @Resource
    private LevelCreator levelCreator;

    @Autowired(required = false)
    private ComebackBonusSettings comebackBonus;

    @Before
    public void prepareProfile() {
        profile = getProfile(testerProfileId);
        profile.money = 0;
        profile.loginSequence = 0;
        profile.@pickUpDailyBonus = 0;

        // ставим lastLoginTime "позавчера", чтобы это никак не влияло на вычисление бонуса
        profile.lastLoginTime = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2));
        profileService.updateSync(profile)
        // но ставим comebackBonus.absentDays на подольше, чтобы не мешался под ногами
        comebackBonus?.absetDays = 21
    }

    @Test
    public void firstDayBonusTest() throws Exception {
        loginMain();

        assertEquals(1, enterAccount.loginAwards.size());
        DailyBonusAward bonus;
        bonus = new DailyBonusAward(enterAccount.loginAwards[0]);
        assertEquals(1, bonus.loginSequence);
        assertEquals(AwardKindEnum.MONEY, bonus.dailyBonusType);
        assertEquals(50, bonus.dailyBonusCount);

        assertEquals(0, profile.money);

        disconnectMain();
        loginMain();

        assertEquals(1, enterAccount.loginAwards.size());
        bonus = new DailyBonusAward(enterAccount.loginAwards[0]);
        assertEquals(1, bonus.loginSequence);
        assertEquals(AwardKindEnum.MONEY, bonus.dailyBonusType);
        assertEquals(50, bonus.dailyBonusCount);

        GetDailyBonusResult getDailyBonusResult;
        getDailyBonusResult = requestMain(new GetDailyBonus(), GetDailyBonusResult.class);
        assertEquals(SimpleResultEnum.SUCCESS, getDailyBonusResult.result);
        assertEquals(50, profile.money);
        assertEquals(bonus.awards, getDailyBonusResult.awards)
        assertTrue(profile.isMatchPickUpDailyBonus(new Date()));

        disconnectMain();
        loginMain();

        assertEquals(0, enterAccount.loginAwards.size());
        assertEquals(50, profile.money);
    }

    /*    @Test
    public void bonusMissionsTest() {
        profileBonusService.getDailyBonusAward().clear();
        GenericAward award = new GenericAward();
        award.setBattlesCount(2);
        profileBonusService.getDailyBonusAward().put((byte) 1, award);

        // заходим первый раз
        profile.setBattlesCount(0);
        DailyBonusAward bonus = profileBonusService.getDailyBonusAward(profile);
        assertEquals(1, bonus.getLoginSequence());
        assertEquals(AwardKindEnum.BATTLES_COUNT, bonus.getDailyBonusType());
        assertEquals(2, bonus.getDailyBonusCount());

        assertEquals(7, profile.getBattlesCount());

        battleService.checkBattleCount(profile);

        assertEquals(7, profile.getBattlesCount());

        disconnect(profile);
    }
    */

    @Test
    public void twoDaysBonusTest() {
        // заходим первый раз
        DailyBonusAward bonus = getDailyBonusAward(profile);
        assertEquals(1, bonus.loginSequence);
        assertEquals(AwardKindEnum.MONEY, bonus.dailyBonusType);
        assertEquals(50, bonus.dailyBonusCount);

        // забираем
        def returnedAwards = []
        assertEquals(SimpleResultEnum.SUCCESS, profileBonusService.pickUpDailyBonus(profile, returnedAwards));
        assertEquals(50, profile.money);
        assertEquals(bonus.awards, returnedAwards)
        profile.money = 0

        disconnect(profile);

        setLastLoginAs(-1);

        // заходим 2-ой день подряд
        profile = getProfile(profile.id);
        bonus = getDailyBonusAward(profile);
        assertEquals(2, bonus.loginSequence);
        assertEquals(AwardKindEnum.REACTION_RATE, bonus.dailyBonusType);
        assertEquals(5, bonus.dailyBonusCount);

        disconnect(profile);

        setLastLoginAs(-2);

        // прерываем серию
        profile = getProfile(profile.id);
        bonus = getDailyBonusAward(profile);
        assertEquals(1, bonus.loginSequence);
        assertEquals(AwardKindEnum.MONEY, bonus.dailyBonusType);
        assertEquals(50, bonus.dailyBonusCount);

        disconnect(profile);
    }

    @Test
    public void eightDaysLowLevelBonusTest() {
        // пусть игрок начального уровня, т.е. ему доступна только первая неделя
        profile.level = dailyBonusAwardSettings.getWeekSetting(2).needLevel - 1

        println "-- заходим первый раз --"
        DailyBonusAward bonus = getDailyBonusAward(profile);
        profile.lastLoginTime = new Date()
        assertEquals(1, bonus.loginSequence);
        assertEquals(AwardKindEnum.MONEY, bonus.dailyBonusType);
        assertEquals(50, bonus.dailyBonusCount);

        println "  забираем награду 1-го дня"
        assertEquals(SimpleResultEnum.SUCCESS, profileBonusService.pickUpDailyBonus(profile, []));
        assertEquals(50, profile.money);

        disconnect(profile);

        // заходим каждый день до конца первой недели
        (2..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${day}-й день подряд --"
            bonus = testAtWeekAndDay(1, day)

            disconnect(profile);
        }

        setLastLoginAs(-1);

        println "-- заходим 8-й день подряд --"
        // должны снова получить награду за первый день первой недели (на вторую нас пока по уровню не пускают)
        profile = getProfile(profile.id);
        bonus = getDailyBonusAward(profile);
        assertEquals(1, bonus.loginSequence);
        assertEquals(AwardKindEnum.MONEY, bonus.dailyBonusType);
        assertEquals(50, bonus.dailyBonusCount);

        disconnect(profile);

    }

    @Test
    public void eightDaysEnoughLevelBonusTest() {
        // пусть игрок среднего уровня: уже доступна вторая неделя
        profile.level = dailyBonusAwardSettings.getWeekSetting(2).needLevel

        println "-- заходим первый раз --"
        DailyBonusAward bonus = getDailyBonusAward(profile);
        profile.lastLoginTime = new Date()
        assertEquals(1, bonus.loginSequence);
        assertEquals(AwardKindEnum.MONEY, bonus.dailyBonusType);
        assertEquals(50, bonus.dailyBonusCount);

        println "  забираем награду 1-го дня"
        assertEquals(SimpleResultEnum.SUCCESS, profileBonusService.pickUpDailyBonus(profile, []));
        assertEquals(50, profile.money);

        disconnect(profile);

        // заходим каждый день до конца первой недели
        (2..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${day}-й день подряд --"
            bonus = testAtWeekAndDay(1, day)

            disconnect(profile);
        }

        setLastLoginAs(-1);

        println "-- заходим 8-й день подряд --"
        // должны получить награду 8-го дня, т.е. первого дня второй недели
        bonus = testAtWeekAndDay(2, 1)

        disconnect(profile);

    }

    @Test
    public void fifteenDaysLowLevelTest() {
        // пусть игрок такого уровня, что ему доступна только первая и вторая неделя
        profile.level = dailyBonusAwardSettings.getWeekSetting(3).needLevel - 1

        println "-- заходим первый раз --"
        DailyBonusAward bonus = getDailyBonusAward(profile);
        profile.lastLoginTime = new Date()
        assertEquals(1, bonus.loginSequence);
        assertEquals(AwardKindEnum.MONEY, bonus.dailyBonusType);
        assertEquals(50, bonus.dailyBonusCount);

        println "  забираем награду 1-го дня"
        assertEquals(SimpleResultEnum.SUCCESS, profileBonusService.pickUpDailyBonus(profile, []));
        assertEquals(50, profile.money);

        disconnect(profile);

        // заходим каждый день до конца первой недели
        (2..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${day}-й день подряд --"
            bonus = testAtWeekAndDay(1, day)

            disconnect(profile);
        }

        // заходим каждый день до конца второй недели
        (1..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${7+day}-й день подряд --"
            bonus = testAtWeekAndDay(2, day)

            disconnect(profile);
        }

        setLastLoginAs(-1);
        println "-- заходим 15-й день подряд --"
        // должны снова получить награду за первый день ВТОРОЙ недели: на третью нас пока по уровню не пускают, но и на начало не сбрасывают (WORMIX-4640)
        bonus = testAtWeekAndDay(2, 1)

        disconnect(profile);
    }

    @Test
    public void fifteenDaysEnoughLevelTest() {
        // пусть игрок такого уровня, что ему доступна и третья
        profile.level = dailyBonusAwardSettings.getWeekSetting(3).needLevel

        println "-- заходим первый раз --"
        DailyBonusAward bonus = getDailyBonusAward(profile);
        profile.lastLoginTime = new Date()
        assertEquals(1, bonus.loginSequence);
        assertEquals(AwardKindEnum.MONEY, bonus.dailyBonusType);
        assertEquals(50, bonus.dailyBonusCount);

        println "  забираем награду 1-го дня"
        assertEquals(SimpleResultEnum.SUCCESS, profileBonusService.pickUpDailyBonus(profile, []));
        assertEquals(50, profile.money);

        disconnect(profile);

        // заходим каждый день до конца первой недели
        (2..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${day}-й день подряд --"
            bonus = testAtWeekAndDay(1, day)

            disconnect(profile);
        }

        // заходим каждый день до конца второй недели
        (1..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${7+day}-й день подряд --"
            bonus = testAtWeekAndDay(2, day)

            disconnect(profile);
        }

        setLastLoginAs(-1);
        println "-- заходим 15-й день подряд --"
        // должны получить награду 15-го дня, т.е. первого дня третьей недели
        bonus = testAtWeekAndDay(3, 1)

        disconnect(profile);
    }

    @Test
    public void twentyTwoDaysLowLevelTest() {
        // пусть игрок такого уровня, что ему доступна только 1-3 недели, но не четвёртая
        profile.level = dailyBonusAwardSettings.getWeekSetting(4).needLevel - 1

        println "-- заходим первый раз --"
        DailyBonusAward bonus = getDailyBonusAward(profile);
        profile.lastLoginTime = new Date()
        assertEquals(1, bonus.loginSequence);
        assertEquals(AwardKindEnum.MONEY, bonus.dailyBonusType);
        assertEquals(50, bonus.dailyBonusCount);

        println "  забираем награду 1-го дня"
        assertEquals(SimpleResultEnum.SUCCESS, profileBonusService.pickUpDailyBonus(profile, []));
        assertEquals(50, profile.money);

        disconnect(profile);

        // заходим каждый день до конца первой недели
        (2..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${day}-й день подряд --"
            bonus = testAtWeekAndDay(1, day)

            disconnect(profile);
        }

        // заходим каждый день до конца второй недели
        (1..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${7+day}-й день подряд --"
            bonus = testAtWeekAndDay(2, day)

            disconnect(profile);
        }

        // заходим каждый день до конца третьей недели
        (1..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${14+day}-й день подряд --"
            bonus = testAtWeekAndDay(3, day)

            disconnect(profile);
        }

        setLastLoginAs(-1);
        println "-- заходим 22-й день подряд --"
        // должны снова получить награду за первый день ТРЕТЬЕЙ недели: на 4-ю нас пока по уровню не пускают, но и на начало не сбрасывают (WORMIX-4640)
        bonus = testAtWeekAndDay(3, 1)

        disconnect(profile);
    }

    @Test
    public void twentyTwoDaysHighLevelTest() {
        // пусть игрок такого уровня, что ему доступно всё (заодно проверяем отработку levelMaxAward)
        profile.level = levelCreator.maxLevel

        println "-- заходим первый раз --"
        DailyBonusAward bonus = getDailyBonusAward(profile);
        profile.lastLoginTime = new Date()
        assertEquals(1, bonus.loginSequence);
        assertEquals(AwardKindEnum.MONEY, bonus.dailyBonusType);
        assertEquals(50, bonus.dailyBonusCount);

        println "  забираем награду 1-го дня"
        assertEquals(SimpleResultEnum.SUCCESS, profileBonusService.pickUpDailyBonus(profile, []));
        assertEquals(50, profile.money);

        disconnect(profile);

        // заходим каждый день до конца первой недели
        (2..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${day}-й день подряд --"
            bonus = testAtWeekAndDay(1, day)

            disconnect(profile);
        }

        // заходим каждый день до конца второй недели
        (1..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${7+day}-й день подряд --"
            bonus = testAtWeekAndDay(2, day)

            disconnect(profile);
        }

        // заходим каждый день до конца третьей недели
        (1..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${14+day}-й день подряд --"
            bonus = testAtWeekAndDay(3, day)

            disconnect(profile);
        }

        setLastLoginAs(-1);
        println "-- заходим 22-й день подряд --"
        // должны получить награду 22-го дня, т.е. первого дня 4-й недели
        bonus = testAtWeekAndDay(4, 1)

        disconnect(profile);
    }

    @Test
    public void monthHighLevelTest() {
        // пусть игрок такого уровня, что ему доступно всё (заодно проверяем отработку levelMaxAward)
        profile.level = levelCreator.maxLevel

        println "-- заходим первый раз --"
        DailyBonusAward bonus = getDailyBonusAward(profile);
        profile.lastLoginTime = new Date()
        assertEquals(1, bonus.loginSequence);
        assertEquals(AwardKindEnum.MONEY, bonus.dailyBonusType);
        assertEquals(50, bonus.dailyBonusCount);

        println "  забираем награду 1-го дня"
        assertEquals(SimpleResultEnum.SUCCESS, profileBonusService.pickUpDailyBonus(profile, []));
        assertEquals(50, profile.money);

        disconnect(profile);

        // заходим каждый день до конца первой недели
        (2..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${day}-й день подряд --"
            bonus = testAtWeekAndDay(1, day)

            disconnect(profile);
        }

        // заходим каждый день до конца второй недели
        (1..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${7+day}-й день подряд --"
            bonus = testAtWeekAndDay(2, day)

            disconnect(profile);
        }

        // заходим каждый день до конца третьей недели
        (1..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${14+day}-й день подряд --"
            bonus = testAtWeekAndDay(3, day)

            disconnect(profile);
        }

        // заходим каждый день до конца четвёртой недели
        (1..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${21+day}-й день подряд --"
            bonus = testAtWeekAndDay(4, day)

            disconnect(profile);
        }

        setLastLoginAs(-1);
        println "-- заходим 29-й день подряд --"
        // дальше двигаться некуда, должны вернуться на начало 4й недели и оттуда продолжить
        bonus = testAtWeekAndDay(4, 1)

        disconnect(profile);
        setLastLoginAs(-1);
        println "-- заходим 30-й день подряд --"
        bonus = testAtWeekAndDay(4, 2)

        disconnect(profile);
    }

    @Test
    public void splitDaysBonusTest() {
        // пусть игрок среднего уровня: уже доступна вторая неделя
        profile.level = dailyBonusAwardSettings.getWeekSetting(2).needLevel

        println "-- заходим первый раз --"
        DailyBonusAward bonus = getDailyBonusAward(profile);
        profile.lastLoginTime = new Date()
        assertEquals(1, bonus.loginSequence);
        assertEquals(AwardKindEnum.MONEY, bonus.dailyBonusType);
        assertEquals(50, bonus.dailyBonusCount);

        println "  забираем награду 1-го дня"
        assertEquals(SimpleResultEnum.SUCCESS, profileBonusService.pickUpDailyBonus(profile, []));
        assertEquals(50, profile.money);

        disconnect(profile);

        // заходим каждый день до конца первой недели
        (2..7).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${day}-й день подряд --"
            bonus = testAtWeekAndDay(1, day)

            disconnect(profile);
        }
        // заходим три дня второй недели
        (1..3).each { int day ->
            setLastLoginAs(-1);

            println "-- заходим ${7+day}-й день подряд --"
            bonus = testAtWeekAndDay(2, day)

            disconnect(profile);
        }

        setLastLoginAs(-1);

        // заходим и ещё один день, но НЕ забираем награду
        println "-- заходим 11-й день подряд --"
        profile = getProfile(profile.id);
        bonus = getDailyBonusAward(profile);
        assertEquals(11, bonus.loginSequence);

        disconnect(profile);

        setLastLoginAs(-1);

        println "-- заходим на следующий день после не забранного бонуса на 2й неделе --"
        // заходим ещё один день, не сбрасываемся на первый день, но начинаем текущую 2ую неделю сначала (WORMIX-4640)
        profile = getProfile(profile.id);
        bonus = testAtWeekAndDay(2, 1)

        disconnect(profile);
    }

    private DailyBonusAward testAtWeekAndDay(int week, int dayInWeek) {
        int day = (week - 1)*dailyBonusAwardSettings.weekLength + dayInWeek

        if (week > 1 && dayInWeek == 1) {
            println "=== НАЧАЛО ${week}-й недели ==="
        }

        def expectedAward = dailyBonusAwardSettings.getAwardByWeekAndDay(week, dayInWeek)
        if(expectedAward.levelMaxAward != null && profile.level == levelCreator.maxLevel) {
            println "  игрок MAX-уровня! Поэтому вместо ${expectedAward} он должен получить ${expectedAward.levelMaxAward}"
            expectedAward = expectedAward.levelMaxAward;
        }

        profile = getProfile(profile.id);
        DailyBonusAward bonus = getDailyBonusAward(profile);
        assertEquals(day, bonus.loginSequence);

        println "  сегодняшний бонус: " + bonus

        List<GenericAwardStructure> returnedAwards = []
        if (StringUtils.isEmpty(expectedAward.seasonWeapons)) {
            // validate bonus
            def expectedAwardSt = toAwardStructure(expectedAward);
            assertEquals(expectedAwardSt.awardKind, bonus.dailyBonusType);
            assertEquals(expectedAwardSt.count, bonus.dailyBonusCount);

            // reset profile
            resetCount(expectedAwardSt, profile)

            // take bonus
            println "  забираем награду ${day}-го дня..."
            assertEquals(SimpleResultEnum.SUCCESS, profileBonusService.pickUpDailyBonus(profile, returnedAwards));
            assertAwarded(expectedAwardSt, profile)

            // check returnedAwards
            assertEquals(bonus.awards, returnedAwards)

            println "  ...успешно получили ожиданный бонус " + expectedAward
        } else {
            // NB: специальный случай выдачи случайных сезонных оружий, проверяем отдельно:

            def curSeasonWeapons = seasonService.currentSeasonWeapons

            // validate bonus
            assertThat(bonus.awards.collect { it.awardKind }, everyItem(is(AwardKindEnum.WEAPON_SHOT)))
            int expectedWeapons = expectedAward.seasonWeapons.split(':')[0] as int
            int expectedShotCount = expectedAward.seasonWeapons.split(':')[1] as int
            assertEquals(expectedWeapons*expectedShotCount, bonus.awards.sum {it.count})
            assertTrue("should give only season weapons, got ${bonus.awards}", bonus.awards.every {curSeasonWeapons.contains(it.itemId)})

            // reset profile
            curSeasonWeapons.each { weaponService.removeWeapon(profile, it) }

            // take bonus
            println "  забираем награду ${day}-го дня"
            assertEquals(SimpleResultEnum.SUCCESS, profileBonusService.pickUpDailyBonus(profile, returnedAwards));
            def gotSeasonWeapons = curSeasonWeapons
                    .findResults { profile.getBackpackItemByWeaponId(it) }
                    .findAll { it.count != 0 }
                    .sort { it.weaponId }
            def totalSeasonWeaponsShots = gotSeasonWeapons.sum { it.count }
            assertEquals(expectedWeapons*expectedShotCount, totalSeasonWeaponsShots)
            assertTrue("should give only season weapons, got ${gotSeasonWeapons}", gotSeasonWeapons.every {curSeasonWeapons.contains(it.weaponId)})

            // check returnedAwards (NB: cannot just assertEquals with bonus.awards: dices could fall differently, so only count of shots is the same)
            assertThat(returnedAwards.collect { it.awardKind }, everyItem(is(AwardKindEnum.WEAPON_SHOT)))
            assertEquals(bonus.awards.sum {it.count}, returnedAwards.sum {it.count})
            def returnedWeapons = returnedAwards
                    .findResults { profile.getBackpackItemByWeaponId(it.itemId) }
                    .sort { it.weaponId }
            assertEquals(gotSeasonWeapons, returnedWeapons)

            println "  ...успешно получили ожиданный бонус: ${expectedWeapons} раз по ${expectedShotCount} выстрелов, а именно: ${returnedAwards}"
        }
        bonus
    }

    private def getDailyBonusAward(UserProfile profile) {
        // эмулирует логин и запрос сегодняшней награды
        def res = profileBonusService.getDailyBonusAward(profile)
        profile.lastLoginTime = new Date()
        res
    }

    private GenericAwardStructure toAwardStructure(GenericAward award) {
        def structures = profileBonusService.genericAwardToListOfGenericAwardStructure(award, profile)
        assertEquals("we assume exactly one daily award for this test", 1, structures.size())
        structures[0]
    }

    private void resetCount(GenericAwardStructure award, UserProfile profile) {
        switch (award.awardKind) {
            case AwardKindEnum.MONEY:
                profile.money = 0
                break
            case AwardKindEnum.REAL_MONEY:
                profile.realMoney = 0
                break
            case AwardKindEnum.BATTLES_COUNT:
                profile.battlesCount.times {
                    battleService.decBattleCount(profile)
                    profile.lastBattleTime = System.currentTimeMillis()
                }
                break
            case AwardKindEnum.REACTION_RATE:
                profile.reactionRate = 0
                break
            case AwardKindEnum.WEAPON_SHOT:
            case AwardKindEnum.WEAPON:
                weaponService.removeWeapon(profile, award.itemId)
                break
            case AwardKindEnum.TEMPORARY_STUFF:
            case AwardKindEnum.STUFF:
                stuffService.removeStuff(profile, (short)award.itemId)
                break
            case AwardKindEnum.REAGENT:
                craftService.setReagentValue(profile, (byte)award.itemId, 0)
                break
            case AwardKindEnum.EXPERIENCE:
                profile.experience = 0
                break
            case AwardKindEnum.WAGER_AWARD_TOKEN:
                dailyRegistry.setWagerWinAwardToken(profile.id, 0)
                break
            case AwardKindEnum.BOSS_AWARD_TOKEN:
                dailyRegistry.setBossWinAwardToken(profile.id, 0)
                break
            default:
                throw new IllegalArgumentException("Should not be used in daily award: " + award.awardKind)
        }
    }
    private void assertAwarded(GenericAwardStructure award, UserProfile profile) {
        switch (award.awardKind) {
            case AwardKindEnum.MONEY:
                assertEquals(award.count, profile.money)
                break
            case AwardKindEnum.REAL_MONEY:
                assertEquals(award.count, profile.realMoney)
                break
            case AwardKindEnum.BATTLES_COUNT:
                assertEquals(award.count, profile.battlesCount)
                break
            case AwardKindEnum.REACTION_RATE:
                assertEquals(award.count, profile.reactionRate)
                break
            case AwardKindEnum.WEAPON_SHOT:
                assertEquals(award.count, profile.getBackpackItemByWeaponId(award.itemId).count)
                break;
            case AwardKindEnum.WEAPON:
                def item = profile.getBackpackItemByWeaponId(award.itemId)
                assertNotNull(item)
                assertEquals(-1, item.count)
                break
            case AwardKindEnum.TEMPORARY_STUFF:
            case AwardKindEnum.STUFF:
                assertTrue(stuffService.isExist(profile, (short)award.itemId))
                break
            case AwardKindEnum.REAGENT:
                assertEquals(award.count, profile.reagents.getReagentValue((byte) award.itemId))
                break
            case AwardKindEnum.EXPERIENCE:
                assertEquals(award.count, profile.experience)
                break
            case AwardKindEnum.WAGER_AWARD_TOKEN:
                assertEquals(award.count, dailyRegistry.getWagerWinAwardToken(profile.id))
                break
            case AwardKindEnum.BOSS_AWARD_TOKEN:
                assertEquals(award.count, dailyRegistry.getBossWinAwardToken(profile.id))
                break
            default:
                throw new IllegalArgumentException("Should not be used in daily award: " + award.awardKind)
        }
    }

    private void setLastLoginAs(int daysDelta) {
        def profile = getProfile(testerProfileId)

        // move back lastLoginTime
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, daysDelta);
        profile.lastLoginTime = cal.time

        // as well as getPickUpDailyBonusDay
        if ((profile.pickUpDailyBonus as int) > 0) {
            cal = Calendar.getInstance()
            cal.set(Calendar.MONTH, profile.pickUpDailyBonusMonth - 1)
            cal.set(Calendar.DAY_OF_MONTH, profile.pickUpDailyBonusDay)
            cal.add(Calendar.DAY_OF_YEAR, daysDelta)
            profile.pickUpDailyBonus = cal.time
        }
        profileService.updateSync(profile)
        softCache.remove(UserProfile.class, profile.id);
    }

    protected void disconnect(UserProfile profile) {
        loginController.onDisconnect(profile);
        softCache.remove(UserProfile.class, profile.getId());
    }

    protected void disconnect() {
        disconnect(softCache.get(UserProfile.class, testerProfileId));
    }

}
