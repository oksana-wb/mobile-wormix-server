package com.pragmatix.app.release;

import com.pragmatix.app.domain.BackpackItemEntity;
import com.pragmatix.app.domain.UserProfileEntity;
import com.pragmatix.app.domain.WormGroupsEntity;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.DaoService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.StuffService;
import com.pragmatix.server.Server;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.10.2015 16:04
 */
public class Release_1_28 {

    private static final Logger log = Server.sysLog;

    private static ProfileService profileService;
    private static DaoService daoService;
    private static StuffService stuffService;
    private static TransactionTemplate transactionTemplate;
    private static JdbcTemplate jdbcTemplate;

    public static void main(String[] args) throws IOException {
        AbstractApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");

        profileService = context.getBean(ProfileService.class);
        stuffService = context.getBean(StuffService.class);
        transactionTemplate = context.getBean(TransactionTemplate.class);
        daoService = context.getBean(DaoService.class);
        jdbcTemplate = context.getBean(JdbcTemplate.class);

        String sql = "SELECT DISTINCT profile_id FROM award_statistic_parent WHERE item_id IN (1027, 1034, 2022, 2037, 1144)";
        log.info(sql + " ...");
        List<Long> uids = jdbcTemplate.query(sql, new RowMapper<Long>() {
            @Override
            public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getLong("profile_id");
            }
        });
        int total = uids.size();
        log.info("result: {} rows", total);
        final List<UserProfile> profilesBundle = new ArrayList<>();
        int i = 0;
        for(Long profileId : uids) {
            i++;
            if(i % 1000 == 0)
                log.info("progress: {}/{}", i, total);

            UserProfile userProfile = getUserProfile(profileId);
            int removedItemsCount = 0;
            if(userProfile != null) {
                for(Integer achieveBonusItemId : Arrays.asList(1027, 1034, 2022, 2037, 1144)) {
                    if(stuffService.removeStuff(userProfile, achieveBonusItemId.shortValue())) {
                        removedItemsCount++;
                    }
                }
            }
            if(removedItemsCount > 0) {
                profilesBundle.add(userProfile);

                if(profilesBundle.size() == 100) {
                    batchUpdate(profilesBundle);
                    profilesBundle.clear();
                }
            }
        }
        batchUpdate(profilesBundle);
        log.info("done: {}/{}", total, total);

        System.exit(0);
    }

    protected static UserProfile getUserProfile(Long profileId) {
        UserProfile result = null;
        if(profileId != null){
            UserProfileEntity userProfileEntity = daoService.getUserProfileDao().get(profileId);
            if (userProfileEntity != null) {
                List<BackpackItemEntity> backpack = Collections.emptyList();
                WormGroupsEntity group = daoService.getWormGroupDao().getWormGroupsByProfileId(profileId);
                Tuple2<Integer, Byte> rankValues = daoService.getUserProfileDao().selectRankValues(profileId);
                result = profileService.newUserProfile(userProfileEntity, backpack, group, rankValues);
            }
        }
        return result;
    }

    protected static void batchUpdate(final List<UserProfile> profilesBundle) {
        log.info("update {} profiles ....", profilesBundle.size());
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                for(UserProfile profile_ : profilesBundle) {
                    daoService.getUserProfileDao().updateProfile(profile_);
                    if(profile_.isTeamMembersDirty()) {
                        daoService.getWormGroupDao().updateWormGroups(profile_);
                    }
                }
            }
        });
    }

}
