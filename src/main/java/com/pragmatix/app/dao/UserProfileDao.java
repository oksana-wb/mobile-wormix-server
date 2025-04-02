package com.pragmatix.app.dao;

import com.pragmatix.app.domain.UserProfileEntity;
import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.RaceService;
import com.pragmatix.app.services.rating.League;
import com.pragmatix.app.services.rating.OldRatingService;
import com.pragmatix.dao.AbstractDao;
import com.pragmatix.server.Server;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.persistence.Query;
import javax.validation.constraints.Null;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Dao класс для сохронения и загрузки UserProfileEntity
 * User: denis
 * Date: 15.11.2009
 * Time: 3:53:21
 */
@Component
public class UserProfileDao extends AbstractDao<UserProfileEntity> {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public UserProfileDao() {
        super(UserProfileEntity.class);
    }

    public void updateReferrer(Long profileId, String referrer) {
        jdbcTemplate.update("UPDATE wormswar.creation_date SET referrer = ? WHERE id = ?", referrer, profileId);
    }

    /**
     * Обновить время последнего обыска домика игрока
     *
     * @param id             игрока которого обыскиваем
     * @param lastSearchTime время последнего обыска
     * @return true если успешно
     */
    public boolean setLastSearchTime(Long id, Date lastSearchTime) {
        int count = getEm().createNamedQuery("setLastSearchTime").setParameter("id", id).
                setParameter("lastSearchTime", lastSearchTime).executeUpdate();
        //setNeedCommit();
        return count > 0;
    }

    /**
     * Обновить параметр скорости реакции
     *
     * @param id           игрока которого качаем
     * @param reactionRate новое значения параметра
     * @return true если успешно
     */
    public boolean setReactionRate(Long id, int reactionRate) {
        int count = getEm().createNamedQuery("setReactionRate").setParameter("id", id).
                setParameter("reactionRate", reactionRate).executeUpdate();
        return count > 0;
    }

    /**
     * вернет список рейтингоов лучших игроков в лиге
     *
     * @param league         лига
     * @param overheadFactor множитель больше единицы - с каким запасом выбирать игроков. Влияен на наполняемость лиг
     * @param lastLoginDays  учитывать в топе только тех игроков, которые заходили хотя бы lastLoginDays дней назад
     * @return список структур
     */

    public List<RatingProfileStructure> getTopPlayers(League league, double overheadFactor, int lastLoginDays) {
        final List<RatingProfileStructure> result = new ArrayList<RatingProfileStructure>();
        String query = "SELECT a.id, armor, attack, level, money, realmoney, reaction_rate, rating, hat, coalesce(race, 0), coalesce(kit, 0), name, vip_expiry_time," +
                " (CASE WHEN wg.profile_id IS NOT NULL THEN 2 ELSE 1 END) + (CASE WHEN wg.team_member_3 IS NOT NULL THEN 1 ELSE 0 END) + (CASE WHEN wg.team_member_4 IS NOT NULL THEN 1 ELSE 0 END) AS group_size  FROM \n" +
                " (SELECT  a.id, armor, attack, level, money, realmoney, reaction_rate, rating,  hat, race, kit, name, vip_expiry_time\n" +
                " FROM wormswar.user_profile a \n" +
                "  WHERE a.id NOT IN (SELECT profile_id \n" +
                "                     FROM wormswar.ban_list \n" +
                "                     WHERE end_date IS NULL OR end_date > now() \n" +
                "     ) \n" +
                " AND rating >= #MIN# AND rating < #MAX# #lastLoginDaysCond#\n" +
                "ORDER BY a.rating DESC \n" +
                "LIMIT #LIMIT# ) AS a\n" +
                "LEFT JOIN wormswar.worm_groups wg ON a.id=wg.profile_id ";

        int maxInTop = OldRatingService.MAX_TOP * league.getDivisionCount();
        int limit = league.getDivisionCount() > 1 ? (int) (maxInTop * overheadFactor) : maxInTop;
        query = query
                .replaceFirst("#MIN#", "" + league.getMin())
                .replaceFirst("#MAX#", "" + league.getMax())
                .replaceFirst("#LIMIT#", "" + limit)
        ;
        if(lastLoginDays > 0) {
            query = query.replaceFirst("#lastLoginDaysCond#", "AND last_login_time > now() - INTERVAL '" + lastLoginDays + " DAYS'");
        } else {
            query = query.replaceFirst("#lastLoginDaysCond#", "");
        }
        Server.sysLog.info(query);
        jdbcTemplate.query(query, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet res) throws SQLException {
                RatingProfileStructure structure = new RatingProfileStructure();
                int i = 1;
                structure.id = res.getLong(i++);
                structure.armor = res.getShort(i++);
                structure.attack = res.getShort(i++);
                structure.level = res.getShort(i++);
                structure.money = res.getInt(i++);
                structure.realMoney = res.getInt(i++);
                structure.reactionRate = res.getInt(i++);
                structure.rating = res.getInt(i++);
                structure.hat = res.getShort(i++);
                short race = res.getShort(i++);
                if(race == 0) {
                    // значит расса закодирована в hat
                    structure.race = RaceService.getRaceId(structure.hat);
                    structure.hat = RaceService.getHatId(structure.hat);
                } else {
                    structure.race = race;
                }
                structure.kit = res.getShort(i++);
                structure.name = res.getString(i++);
                Timestamp vipExpiryTime = res.getTimestamp(i++);
                structure.vipExpiryTime = vipExpiryTime != null ? (int) (vipExpiryTime.getTime() / 1000L) : 0;
                structure.groupCount = res.getInt(i++);
                result.add(structure);
            }
        });
        return result;
    }

    /**
     * Вернет спиок профайлов по их серверному id
     * метод для постраничного вывода.
     *
     * @param ids список id
     * @return список профилей
     */
    public List<UserProfileEntity> getProfilesByIds(Collection ids) {
        Query query = em.createNamedQuery("getProfilesById").
                setParameter("ids", ids);
        return (List<UserProfileEntity>) query.getResultList();
    }

    @Null
    public Tuple2<Integer, Byte> selectRankValues(Long profileId) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM wormswar.ranks WHERE profile_id = ?", (res, i) -> {
                return Tuple.of(res.getInt("rank_points"), res.getByte("best_rank"));
            }, profileId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public <T> Map<Long, Tuple2<Integer, Byte>> selectRankValues(Collection<Long> ids) {
        Map<Long, Tuple2<Integer, Byte>> result = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        params.put("ids", ids);
        namedParameterJdbcTemplate.query("SELECT * FROM wormswar.ranks WHERE profile_id IN (:ids)", params, (res) -> {
            result.put(res.getLong("profile_id"), Tuple.of(res.getInt("rank_points"), res.getByte("best_rank")));
        });
        return result;
    }

    /**
     * обновить профайл в БД
     *
     * @param profile профайл который необходимо обновить
     * @return true если успешно
     */
    public void updateProfile(UserProfile profile) {
        if(profile.isDirty()) {
            getEm().createNamedQuery("updateUserProfile").
                    setParameter("id", profile.getId()).
                    setParameter("name", profile.getName()).
                    setParameter("money", profile.getMoney()).
                    setParameter("realmoney", profile.getRealMoney()).
                    setParameter("rating", profile.getRating()).
                    setParameter("armor", (short) profile.getArmor()).
                    setParameter("attack", (short) profile.getAttack()).
                    setParameter("battlesCount", profile.getBattlesCount()).
                    setParameter("level", (short) profile.getLevel()).
                    setParameter("experience", (short) profile.getExperience()).
                    setParameter("hat", profile.getHat()).
                    setParameter("race", profile.getRace()).
                    setParameter("races", profile.getRaces()).
                    setParameter("selectRaceTime", profile.getSelectRaceTime()).
                    setParameter("kit", profile.getKit()).
                    setParameter("lastBattleTime", new Date(profile.getLastBattleTime())).
                    setParameter("lastLoginTime", profile.getLastLoginTime()).
                    setParameter("stuff", ArrayUtils.isNotEmpty(profile.getStuff()) ? profile.getStuff() : null).
                    setParameter("temporalStuff", ArrayUtils.isNotEmpty(profile.getTemporalStuff()) ? profile.getTemporalStuff() : null).
                    setParameter("lastSearchTime", profile.getLastSearchTime()).
                    setParameter("loginSequence", profile.getLoginSequence()).
                    setParameter("reactionRate", profile.getReactionRate()).
                    setParameter("currentMission", profile.getCurrentMission()).
                    setParameter("currentNewMission", profile.getCurrentNewMission()).
                    setParameter("recipes", ArrayUtils.isNotEmpty(profile.getRecipes()) ? profile.getRecipes() : null).
                    setParameter("comebackedFriends", profile.getComebackedFriends()).
                    setParameter("locale", (short) profile.getLocale().getType()).
                    setParameter("renameAct", profile.getRenameAct()).
                    setParameter("renameVipAct", profile.getRenameVipAct()).
                    setParameter("logoutTime", profile.getLogoutTime() > 0 ? new Date((long) profile.getLogoutTime() * 1000L) : null).
                    setParameter("pickUpDailyBonus", profile.getPickUpDailyBonus()).
                    setParameter("skins", ArrayUtils.isNotEmpty(profile.getSkins()) ? profile.getSkins() : null).
                    setParameter("vipExpiryTime", profile.getVipExpiryTime() > 0 ? new Date((long) profile.getVipExpiryTime() * 1000L) : null).
                    setParameter("vipSubscriptionId", profile.getVipSubscriptionId()).
                    setParameter("countryCode", profile.getCountryCode()).
                    setParameter("currencyCode", profile.getCurrencyCode()).
                    setParameter("levelUpTime", profile.getLevelUpTime()).
                    setParameter("releaseAward", profile.getReleaseAward()).
                    executeUpdate();

            if(profile.getRankPoints() != 0)
                getEm().createNamedQuery("insertOrUpdateRankValues").
                        setParameter("profileId", profile.getId()).
                        setParameter("rankPoints", profile.getRankPoints()).
                        setParameter("bestRank", profile.getBestRank()).
                        executeUpdate();

            //говорим, что все данные игрока уже сохранены
            profile.setDirty(false);
        }
    }

    /**
     * увеличиваем счетчик возвращенных друзей
     *
     * @param profileId игрока
     * @return true если удалить удалось
     */
    public boolean incComebackedFriends(Long profileId) {
        return jdbcTemplate.update("UPDATE wormswar.user_profile SET comebacked_friends = coalesce(comebacked_friends, 0) + 1 WHERE id = ?", profileId) > 0;
    }

    /**
     * выставляем счетчик возвращенных друзей
     *
     * @param profileId игрока
     * @return true если удалить удалось
     */
    public boolean setComebackedFriends(short comebackedFriends, Long profileId) {
        return jdbcTemplate.update("UPDATE wormswar.user_profile SET comebacked_friends = ? WHERE id = ?", comebackedFriends, profileId) > 0;
    }

    /**
     * Устанавливает игроку последнее время возвращения
     * <p>
     * защита от частых возвратов на фейках (http://jira.pragmatix-corp.com/browse/WORMIX-4273)
     *
     * @param profileId               id игрока
     * @param lastBeingComebackedTime время его последнего возвращения
     * @return true если обновить удалось
     */
    public void setLastBeingComebackedTime(Long profileId, Date lastBeingComebackedTime) {
        getEm().createNamedQuery("insertOrUpdateLastBeingComebackedTime").
                setParameter("id", profileId).
                setParameter("lastBeingComebackedTime", lastBeingComebackedTime).
                executeUpdate();
    }

    /**
     * Очищает для игрока доп.информацию, такую как время последнего возращения
     *
     * @param profileId id игрока
     * @return true если удалить удалось
     */
    public void clearMeta(Long profileId) {
        getEm().createNamedQuery("clearMeta").setParameter("profileId", profileId).executeUpdate();
    }

    public void clearRanks(Long profileId) {
        getEm().createNamedQuery("clearRank").setParameter("profileId", profileId).executeUpdate();
    }

    /**
     * увеличить скорость реакции на 1
     *
     * @param friendsIds список друзей которым прокачали реакцию
     * @return true если удалось
     */
    public int increaseReactionRates(Set<Long> friendsIds) {
        if(friendsIds.size() > 0) {
            StringBuilder s = new StringBuilder();
            for(Long friendsId : friendsIds) {
                s.append(',').append(friendsId);
            }
            return jdbcTemplate.update("UPDATE wormswar.user_profile SET reaction_rate = coalesce(reaction_rate, 0) + 1 WHERE id IN (" + s.substring(1) + ")");
        } else {
            return 0;
        }
    }

    /**
     * вернес список id профайлов существующих в базе
     *
     * @param profileIds
     * @return
     */
    public Collection<Long> checkProfiles(Collection<Long> profileIds) {
        if(profileIds.size() > 0) {
            StringBuilder s = new StringBuilder();
            for(Long profileId : profileIds) {
                s.append(',').append(profileId);
            }
            return jdbcTemplate.query("SELECT id FROM wormswar.user_profile WHERE id IN (" + s.substring(1) + ")", new RowMapper<Long>() {
                @Override
                public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getLong("id");
                }
            });
        } else {
            return new HashSet<Long>();
        }
    }
}

