package com.pragmatix.achieve.mappers;

import com.pragmatix.achieve.domain.IAchievementName;
import com.pragmatix.achieve.domain.ProfileAchievements;
import com.pragmatix.achieve.domain.WormixAchievements;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static com.pragmatix.achieve.domain.ProfileAchievements.STAT_FIRST_INDEX;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.08.2016 14:48
 */
public class WormixAchievementsMapperImpl implements AchievementsMapper<WormixAchievements> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    private String updateQuery;

    private String insertQuery;

    @PostConstruct
    public void init() {
        updateQuery = "UPDATE achieve.worms_achievements SET " +
                " user_profile_id=?, " +
                " time_sequence=?, " +
                " achieve_points=?, "+
                " invested_award_points=?, ";
        for(WormixAchievements.AchievementName achievementName : WormixAchievements.AchievementName.values()) {
            updateQuery += achievementName.name() + "=?, ";
        }
        updateQuery += " bool_achievements=?, update_date = now() " +
                " where profile_id = ?";

        insertQuery = "INSERT INTO achieve.worms_achievements (" +
                " profile_id, " +
                " user_profile_id, " +
                " time_sequence, " +
                " achieve_points, "+
                " invested_award_points, ";
        for(WormixAchievements.AchievementName achievementName : WormixAchievements.AchievementName.values()) {
            insertQuery += achievementName.name() + ", ";
        }
        insertQuery += " bool_achievements) values (" + StringUtils.repeat("?, ", WormixAchievements.AchievementName.values().length + 5) + "?)";
    }

    @Override
    public WormixAchievements selectAchievements(String profileId) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM achieve.worms_achievements WHERE profile_id = ?", (res, i) -> {
                WormixAchievements result = new WormixAchievements(res.getString("profile_id"));
                result.setUserProfileId(res.getInt("user_profile_id"));
                result.setTimeSequence(res.getInt("time_sequence"));
                result.setInvestedAwardPoints(res.getByte("invested_award_points"));
                result.setBoolAchievements(res.getBytes("bool_achievements"));

                for(WormixAchievements.AchievementName achievementName : WormixAchievements.AchievementName.values()) {
                    if(achievementName.isStat()) {
                        int statValue = res.getInt(achievementName.name());
                        if(statValue < 0)
                            statValue = 0xFFFF & statValue;
                        setStatValue(result, achievementName, statValue);
                    } else {
                        setAchievementValue(result, achievementName, res.getShort(achievementName.name()));
                    }
                }
                return result;
            }, profileId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (DataAccessException e) {
            log.error(e.toString(), e);
            return null;
        }
    }

    @Override
    public int updateAchievements(WormixAchievements entity, int achievePoints) {
        try {
            List<Object> params = new ArrayList<>();
            params.add(entity.userProfileId);
            params.add(entity.getTimeSequence());
            params.add(achievePoints);
            params.add(entity.getInvestedAwardPoints());
            for(WormixAchievements.AchievementName achievementName : WormixAchievements.AchievementName.values()) {
                params.add(getAchievementRaw(entity, achievementName));
            }
            params.add(entity.getBoolAchievements());
            params.add(entity.getProfileId());

            return transactionTemplate.execute(transactionStatus -> jdbcTemplate.update(updateQuery, params.toArray()));
        } catch (TransactionException e) {
            log.error(e.toString());
            return 0;
        }
    }

    @Override
    public int insertAchievements(WormixAchievements entity, int achievePoints) {
        try {
            List<Object> params = new ArrayList<>();
            params.add(entity.getProfileId());
            params.add(entity.userProfileId);
            params.add(entity.getTimeSequence());
            params.add(achievePoints);
            params.add(entity.getInvestedAwardPoints());
            for(WormixAchievements.AchievementName achievementName : WormixAchievements.AchievementName.values()) {
                params.add(getAchievementRaw(entity, achievementName));
            }
            params.add(entity.getBoolAchievements());

            return transactionTemplate.execute(transactionStatus -> jdbcTemplate.update(insertQuery, params.toArray()));
        } catch (TransactionException e) {
            log.error(e.toString());
            return 0;
        }
    }

    @Override
    public int wipeAchievements(String profileId, String wipedId) {
        try {
            return transactionTemplate.execute(transactionStatus ->
                    jdbcTemplate.update("UPDATE achieve.worms_achievements SET profile_id=? WHERE profile_id=?", wipedId, profileId));
        } catch (TransactionException e) {
            log.error(e.toString());
            return 0;
        }
    }

    private Object getAchievementRaw(ProfileAchievements entity, IAchievementName achievementName) {
        return achievementName.isStat() ? entity.getStatistics()[achievementName.getIndex() - STAT_FIRST_INDEX] : entity.getAchievements()[achievementName.getIndex()];
    }

    private void setAchievementValue(ProfileAchievements entity, IAchievementName achievementName, short achievementValue) {
        entity.getAchievements()[achievementName.getIndex()] = achievementValue;
    }

    public void setStatValue(ProfileAchievements entity, IAchievementName achievementName, int achievementValue) {
        entity.getStatistics()[achievementName.getIndex() - STAT_FIRST_INDEX] = achievementValue;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

}
