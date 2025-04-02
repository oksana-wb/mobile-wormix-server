package com.pragmatix.app.dao;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static net.sf.ezmorph.test.ArrayAssertions.assertEquals;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.01.13 16:44
 */
public class UserProfileDaoTest extends AbstractSpringTest {

    @Resource
    private UserProfileDao userProfileDao;

    @Resource
    protected SoftCache softCache;

    @Test
    public void daoTest() throws Exception {
        LocalDateTime  currentSeasonStartDateTime = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        daoService.doInTransactionWithoutResult(() ->
                jdbcTemplate.update("INSERT INTO wormswar.season_total (season, profile_id, rank, rank_points) SELECT ?, profile_id, best_rank, rank_points FROM wormswar.ranks WHERE best_rank <= ?",
                        currentSeasonStartDateTime, 15)
        );
    }

    @Test
    public void testUpdateProfile() throws Exception {
        final UserProfile profile = getProfile(testerProfileId);
        short kitId = (short) 2000;
        profile.setKit(kitId);
        profile.setDirty(true);

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                userProfileDao.updateProfile(profile);
            }
        });

        softCache.remove(UserProfile.class, testerProfileId);
        UserProfile profileReloaded = getProfile(testerProfileId);

        assertEquals(kitId, profileReloaded.getKit());
    }
}
