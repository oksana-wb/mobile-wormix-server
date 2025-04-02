package com.pragmatix.app.services;

import com.pragmatix.app.common.CheatTypeEnum;
import com.pragmatix.app.filters.AuthFilter;
import com.pragmatix.app.messages.server.SearchTheHouseResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 13.07.11 12:24
 */
public class CheatersCheckerServiceTest extends AbstractSpringTest {

    @Resource
    private SearchTheHouseService searchTheHouseService;

    @Resource
    private CheatersCheckerService cheatersCheckerService;

    @Resource
    private AuthFilter authFilter;

    @Test
    public void testSearchTheHouse() {
        UserProfile friendProfile = getProfile(testerProfileId);
        friendProfile.setLevel(5);
        friendProfile.setLastSearchTime(null);
        friendProfile.setLastLoginTime(null);

        long cheaterId = testerProfileId + 1;
        cheatersCheckerService.getExcludedFromRubyAward().remove(cheaterId);
        assertEquals(SearchTheHouseResult.ResultEnum.REAL_MONEY, searchTheHouseService.getSearchResult(friendProfile, cheaterId));

        cheatersCheckerService.getExcludedFromRubyAward().add(cheaterId);
        assertEquals(SearchTheHouseResult.ResultEnum.RUBY_LIMIT_EXEED, searchTheHouseService.getSearchResult(friendProfile, cheaterId));
    }

    @Test
    public void testCheatTypeCompared() {
        assertTrue(CheatTypeEnum.OK.isMoreSevereThan(CheatTypeEnum.UNCHECKED));                 // ZERO > UNDEFINED
        assertTrue(CheatTypeEnum.WEAPON_USAGE_HIGH.isMoreSevereThan(CheatTypeEnum.OK));         // UNDEFINED > REPORT
        assertTrue(CheatTypeEnum.BOSS_HP_LOW.isMoreSevereThan(CheatTypeEnum.WEAPON_USAGE_HIGH));// DISCARD > REPORT
        assertTrue(CheatTypeEnum.BOSS_TURN_NEVER.isMoreSevereThan(CheatTypeEnum.BOSS_HP_LOW));  // IMMEDIATE_BAN > DISCARD
    }

    @Test
    public void testMissionLogValidation() {
        // TODO: write me
    }
}
