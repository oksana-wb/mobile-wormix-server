package com.pragmatix.clanserver.dao.jdbc;

import com.pragmatix.clanserver.dao.DAO;
import com.pragmatix.clanserver.dao.Dictionary;
import com.pragmatix.clanserver.domain.*;
import com.pragmatix.clanserver.messages.structures.ClanAuditActionTO;
import com.pragmatix.clanserver.services.RatingServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Author: Vladimir
 * Date: 04.04.13 11:08
 */
@Repository
public class JdbcDAO implements DAO {
    public static final Map<String, Object> NO_PARAMS = new HashMap<>(0);

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    RatingServiceImpl ratingService;

    final ClanMapper clanMapper = new ClanMapper();

    final RatingItemMapper ratingItemMapper = new RatingItemMapper();

    final SeasonMapper seasonMapper = new SeasonMapper();

    final ClanAuditActionMapper clanAuditActionMapper = new ClanAuditActionMapper();

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Clan createClan(Clan clan, ClanMember leader) {
        clan.createDate = new Date();

        String sql = "INSERT INTO clan.clan(name, level, size, rating, season_rating, create_date, emblem, description, review_state, normal_name)" +
                " VALUES(:name, :level, :size, :rating, :seasonRating, :createDate, :emblem, :description, :reviewState, :normalName)";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", clan.name)
                .addValue("level", clan.level)
                .addValue("size", clan.size)
                .addValue("rating", clan.rating)
                .addValue("seasonRating", clan.seasonRating)
                .addValue("createDate", clan.createDate)
                .addValue("emblem", clan.emblem)
                .addValue("description", clan.description)
                .addValue("reviewState", clan.reviewState.code)
                .addValue("normalName", clan.normalName());
        jdbcTemplate.update(sql, params, keyHolder, names("id"));
        clan.id = keyHolder.getKey().intValue();

        leader.clan = clan;
        leader.rank = Rank.LEADER;
        leader.joinDate = new Date();

        if(leader.isNew()) {
            createClanMember(leader);
        } else {
            updateClanMember(leader);
        }

        return clan;
    }

    @Override
    @Transactional
    public Clan getClan(Integer id) {
        String sql = "SELECT * FROM clan.clan WHERE id=" + id;

        final Clan clan = find(sql, NO_PARAMS, clanMapper);

        if(clan != null) {
            completeClan(clan);
        }

        return clan;
    }

    @Override
    @Transactional
    public List<Clan> getClans(int[] clansId) {
        StringBuilder sql = new StringBuilder("SELECT * FROM clan.clan WHERE id in (");

        for(int i = 0; i < clansId.length; i++) {
            if(i > 0) {
                sql.append(",");
            }
            sql.append(clansId[i]);
        }

        sql.append(")");

        List<Clan> res = jdbcTemplate.query(sql.toString(), NO_PARAMS, clanMapper);

        completeClans(res);

        return res;
    }

    @Override
    @Transactional
    public List<Clan> getClans(short socialId, int[] profilesId) {
        StringBuilder sql = new StringBuilder("SELECT * FROM clan.clan WHERE id in (" +
                "\nSELECT DISTINCT clan_id FROM clan.clan_member WHERE social_id=" + socialId + " AND profile_id in (");

        for(int i = 0; i < profilesId.length; i++) {
            if(i > 0) {
                sql.append(",");
            }
            sql.append(profilesId[i]);
        }

        sql.append("))");

        List<Clan> res = jdbcTemplate.query(sql.toString(), NO_PARAMS, clanMapper);

        completeClans(res);

        return res;
    }

    @Override
    @Transactional
    public boolean clanExists(String name, int clanId) {
        String sql = "SELECT id FROM clan.clan WHERE normal_name=:normalName";
        try {
            return clanId != jdbcTemplate.queryForObject(sql, params(new String[]{"normalName"}, new String[]{Clan.normalName(name)}), Integer.class);
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public Clan getClanByMember(short socialId, int profileId) {
        String sql = "SELECT * FROM clan.clan WHERE id=(SELECT clan_id FROM clan.clan_member WHERE social_id=:socialId AND profile_id=:profileId)";

        final Clan clan = find(sql, params(names("socialId", "profileId"), values(socialId, profileId)), clanMapper);

        if(clan != null) {
            completeClan(clan);
        }

        return clan;

    }

    @Override
    public List<Clan> listClansByName(String searchPhrase, int limit) {
        String sql = "SELECT * FROM clan.clan WHERE normal_name LIKE :searchPhrase AND review_state != :lockState ORDER BY rating DESC LIMIT " + limit;

        return jdbcTemplate.query(sql, params(names("searchPhrase", "lockState"),
                values("%" + Clan.normalName(searchPhrase) + "%", ReviewState.LOCKED.code)), clanMapper);
    }

    @Override
    public List<Clan> listClansOrderByName(String searchPhrase, ReviewState[] reviewStates, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM clan.clan");

        StringBuilder where = new StringBuilder();
        HashMap<String, Object> params = new HashMap<>();

        if(searchPhrase != null && (searchPhrase = searchPhrase.trim()).length() > 0) {
            if(where.length() > 0) {
                where.append(" AND ");
            }
            where.append("normal_name LIKE :searchPhrase");
            params.put("searchPhrase", "%" + Clan.normalName(searchPhrase) + "%");
        }

        if(reviewStates != null && reviewStates.length > 0) {
            if(where.length() > 0) {
                where.append(" AND ");
            }
            for(int i = 0; i < reviewStates.length; i++) {
                where.append(i == 0 ? "(" : " OR ");
                where.append("review_state=").append(reviewStates[i].code);
            }
            where.append(")");
        }

        if(where.length() > 0) {
            sql.append("\nWHERE ").append(where);
        }
        sql.append("\nORDER BY name, id OFFSET ").append(offset).append(" LIMIT ").append(limit);

        return jdbcTemplate.query(sql.toString(), params, clanMapper);
    }

    @Override
    @Transactional
    public void updateClan(Clan clan) {
        String sql = "UPDATE clan.clan SET name=:name, level=:level, size=:size, rating=:rating, season_rating=:seasonRating, join_rating=:joinRating" +
                ", description=:description, review_state=:reviewState, normal_name=:normalName, closed=:closed, treas=:treas, medal_price=:medalPrice" +
                ", cashed_medals=:cashedMedals  WHERE id=:id";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", clan.id)
                .addValue("name", clan.name)
                .addValue("level", clan.level)
                .addValue("size", clan.size)
                .addValue("rating", clan.rating)
                .addValue("seasonRating", clan.seasonRating)
                .addValue("joinRating", clan.joinRating)
                .addValue("description", clan.description)
                .addValue("reviewState", clan.reviewState.code)
                .addValue("normalName", clan.normalName())
                .addValue("closed", clan.closed)
                .addValue("treas", clan.treas)
                .addValue("medalPrice", clan.medalPrice)
                .addValue("cashedMedals", clan.cashedMedals);

        jdbcTemplate.update(sql, params);

        clan.setDirty(false);
    }

    @Override
    @Transactional
    public void updateClanAggregates(Clan clan) {
        String sql = "UPDATE clan.clan SET size=:size, rating=:rating, season_rating=:seasonRating, treas=:treas, cashed_medals=:cashedMedals WHERE id=:id";

        jdbcTemplate.update(sql, params(
                names("id", "size", "rating", "seasonRating", "treas", "cashedMedals"),
                values(clan.id, clan.size, clan.rating, clan.seasonRating, clan.treas, clan.cashedMedals)));

        clan.setDirty(false);
    }

    @Override
    @Transactional
    public void updateClanName(Integer id, String name, ReviewState reviewState) {
        String sql = "UPDATE clan.clan SET name=:name, normal_name=:normalName, review_state=:reviewState WHERE id=:id";

        jdbcTemplate.update(sql, params(
                names("id", "name", "normalName", "reviewState"),
                values(id, name, Clan.normalName(name), reviewState.code)));
    }

    @Override
    @Transactional
    public void updateClanReviewState(Integer id, ReviewState reviewState) {
        String sql = "UPDATE clan.clan SET review_state=:reviewState WHERE id=:id";

        jdbcTemplate.update(sql, params(
                names("id", "reviewState"),
                values(id, reviewState.code)));
    }

    @Override
    @Transactional
    public void updateClanClosedState(Integer id, boolean closed) {
        String sql = "UPDATE clan.clan SET closed=:closed WHERE id=:id";

        jdbcTemplate.update(sql, params(
                names("id", "closed"),
                values(id, closed)));
    }

    @Override
    @Transactional
    public void updateClanMedalPrice(Integer id, byte medalPrice) {
        String sql = "UPDATE clan.clan SET medal_price=:medalPrice WHERE id=:id";

        jdbcTemplate.update(sql, params(
                names("id", "medalPrice"),
                values(id, medalPrice)));
    }

    @Override
    @Transactional
    public void updateClanEmblem(Integer id, byte[] emblem) {
        String sql = "UPDATE clan.clan SET emblem=:emblem WHERE id=:id";

        jdbcTemplate.update(sql, params(
                names("id", "emblem"),
                values(id, emblem)));
    }

    @Override
    @Transactional
    public void updateClanDescription(Integer id, String description, ReviewState reviewState) {
        String sql = "UPDATE clan.clan SET description=:description, review_state=:reviewState WHERE id=:id";

        jdbcTemplate.update(sql, params(
                names("id", "description", "reviewState"),
                values(id, description, reviewState.code)));
    }

    @Override
    @Transactional
    public void updateClanJoinRating(Integer id, int joinRating) {
        String sql = "UPDATE clan.clan SET join_rating=:joinRating WHERE id=:id";

        jdbcTemplate.update(sql, params(
                names("id", "joinRating"),
                values(id, joinRating)));
    }

    private void completeClan(Clan clan) {
        String sql = "SELECT * FROM clan.clan_member WHERE clan_id=:clanId";

        List<ClanMember> members = jdbcTemplate.query(sql, params(names("clanId"), values(clan.id)),
                new ClanMemberMapper(dictionary(clan), ratingService));

        for(ClanMember member : members) {
            member.oldPlace = ratingService.getMemberOldPlace(member);
            clan.memberDict.put(member.getId(), member);
        }
    }

    private void completeClans(List<Clan> clans) {
        if(clans.size() == 0) {
            return;
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM clan.clan_member WHERE clan_id in (");

        int i = 0;
        for(Clan clan : clans) {
            if(i++ > 0) {
                sql.append(",");
            }
            sql.append(clan.id);
        }

        sql.append(")");

        List<ClanMember> members = jdbcTemplate.query(sql.toString(), NO_PARAMS, new ClanMemberMapper(clanDictionary(clans), ratingService));

        for(ClanMember member : members) {
            member.clan.memberDict.put(member.getId(), member);
        }
    }

    @Override
    @Transactional
    public void deleteClan(Integer clanId) {
        String sql = "DELETE FROM clan.clan WHERE id=" + clanId;
        jdbcTemplate.update(sql, NO_PARAMS);
    }

    @Override
    @Transactional
    public void expandClan(Integer clanId, int level) {
        String sql = "UPDATE clan.clan SET level=:level WHERE id=:id";

        jdbcTemplate.update(sql, params(names("id", "level"), values(clanId, level)));
    }

    @Override
    @Transactional
    public ClanMember createClanMember(ClanMember clanMember) {
        String sql = "INSERT INTO clan.clan_member(clan_id, rank, name, social_id, profile_id, social_profile_id, join_date, login_date, rating, last_login_time)\n" +
                "VALUES(:clanId, :rank, :name, :socialId, :profileId, :socialProfileId, :joinDate, :loginDate, :rating, :lastLoginTime)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("clanId", clanMember.getClanId())
                .addValue("rank", clanMember.rank.code)
                .addValue("name", clanMember.name)
                .addValue("socialId", clanMember.socialId)
                .addValue("profileId", clanMember.profileId)
                .addValue("socialProfileId", clanMember.socialProfileId)
                .addValue("joinDate", clanMember.joinDate != null ? clanMember.joinDate : new Date())
                .addValue("loginDate", clanMember.loginDate)
                .addValue("rating", clanMember.rating)
                .addValue("lastLoginTime", new Date(clanMember.lastLoginTime * 1000L));

        jdbcTemplate.update(sql, params);

        // восстанавливаем игрока в этом клане (заполняется тригерром) если игрок входит в тот же клан из которого до этого вышел
        params = new MapSqlParameterSource().addValue("socialId", clanMember.socialId).addValue("profileId", clanMember.profileId);
        Map<String, Object> objectMap = jdbcTemplate.queryForMap(
                "SELECT season_rating, donation, donation_curr_season, donation_prev_season, donation_curr_season_comeback, donation_prev_season_comeback, cashed_medals" +
                        " FROM clan.clan_member WHERE social_id = :socialId AND profile_id = :profileId", params);
        clanMember.seasonRating = (Integer) objectMap.get("season_rating");
        clanMember.donation = (Integer) objectMap.get("donation");
        clanMember.donationCurrSeason = (Integer) objectMap.get("donation_curr_season");
        clanMember.donationPrevSeason = (Integer) objectMap.get("donation_prev_season");
        clanMember.donationCurrSeasonComeback = (Integer) objectMap.get("donation_curr_season_comeback");
        clanMember.donationPrevSeasonComeback = (Integer) objectMap.get("donation_prev_season_comeback");
        clanMember.cashedMedals = (Integer) objectMap.get("cashed_medals");
        clanMember.setNew(false);
        clanMember.setDirty(false);

        return clanMember;
    }

    @Override
    @Transactional
    public void deleteClanMember(short socialId, int profileId) {
        String sql = "DELETE FROM clan.clan_member WHERE social_id=" + socialId + " AND profile_id=" + profileId;

        jdbcTemplate.update(sql, NO_PARAMS);
    }

    @Override
    @Transactional
    public ClanMember updateClanMember(ClanMember clanMember) {
        String sql = "UPDATE clan.clan_member " +
                "SET clan_id=:clanId, name=:name, rank=:rank, join_date=:joinDate, login_date=:loginDate, logout_date=:logoutDate, rating=:rating, season_rating=:seasonRating " +
                ", last_login_time=:lastLoginTime,  donation=:donation, donation_curr_season=:donationCurrSeason, donation_prev_season=:donationPrevSeason" +
                ", donation_curr_season_comeback=:donationCurrSeasonComeback, donation_prev_season_comeback=:donationPrevSeasonComeback, cashed_medals=:cashedMedals" +
                ", expel_permit=:expelPermit, mute_mode=:muteMode, host_profile_id=:hostProfileId, daily_rating=:dailyRating " +
                " WHERE social_id=:socialId AND profile_id=:profileId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("socialId", clanMember.socialId)
                .addValue("profileId", clanMember.profileId)
                .addValue("clanId", clanMember.getClanId())
                .addValue("name", clanMember.name)
                .addValue("rank", clanMember.rank.code)
                .addValue("joinDate", clanMember.joinDate)
                .addValue("loginDate", clanMember.loginDate)
                .addValue("logoutDate", clanMember.logoutDate)
                .addValue("rating", clanMember.rating)
                .addValue("seasonRating", clanMember.seasonRating)
                .addValue("lastLoginTime", new Date(clanMember.lastLoginTime * 1000L))
                .addValue("donation", clanMember.donation)
                .addValue("donationCurrSeason", clanMember.donationCurrSeason)
                .addValue("donationPrevSeason", clanMember.donationPrevSeason)
                .addValue("donationCurrSeasonComeback", clanMember.donationCurrSeasonComeback)
                .addValue("donationPrevSeasonComeback", clanMember.donationPrevSeasonComeback)
                .addValue("cashedMedals", clanMember.cashedMedals)
                .addValue("expelPermit", clanMember.expelPermit)
                .addValue("muteMode", clanMember.muteMode)
                .addValue("hostProfileId", clanMember.hostProfileId)
                .addValue("dailyRating", RatingServiceImpl.dailyRatingToGson(clanMember.dailyRating))
                ;

        jdbcTemplate.update(sql, params);

        clanMember.setDirty(false);

        return clanMember;
    }

    @Override
    @Transactional
    public void runInTransaction(Runnable task) {
        task.run();
    }

    @Override
    public List<RatingItem> getTopRatings(int limit) {
        String sql = "SELECT id, rating, join_rating, season_rating FROM clan.clan" +
                "\nWHERE review_state != " + ReviewState.LOCKED.code +
                "\nORDER BY rating DESC, id DESC";

        if(limit > 0) {
            sql += " LIMIT " + limit;
        }

        return jdbcTemplate.query(sql, NO_PARAMS, ratingItemMapper);
    }

    @Override
    public Season selectCurrentOpenSeason() {
        String sql = "SELECT * FROM clan.season WHERE (now() > start OR id = 1) AND NOT closed ORDER BY id DESC LIMIT 1";

        return jdbcTemplate.queryForObject(sql, NO_PARAMS, seasonMapper);
    }

    @Override
    @Transactional
    public int insertNewSeason(Season season) {
        String sql = "INSERT INTO clan.season (id, start, finish) VALUES (:id, :start, :finish)";
        return jdbcTemplate.update(sql, params(new String[]{"id", "start", "finish"}, new Object[]{season.id, season.start, season.finish}));
    }

    @Override
    @Transactional
    public int backupMember(int clanId, ClanMember member) {
        if(member.seasonRating > 0 || member.donation > 0 || member.donationCurrSeason > 0 || member.donationPrevSeason > 0 || member.cashedMedals > 0) {
            String sql = "INSERT INTO clan.clan_member_backup_rating " +
                    " (social_id, profile_id, clan_id, season_rating, backup_date, donation, donation_curr_season, donation_prev_season, donation_curr_season_comeback, donation_prev_season_comeback, cashed_medals) " +
                    " VALUES " +
                    " (:social_id, :profile_id, :clan_id, :season_rating, now(), :donation, :donation_curr_season, :donation_prev_season, :donation_curr_season_comeback, :donation_prev_season_comeback, :cashed_medals)";
            return jdbcTemplate.update(sql, params(new String[]{
                            "social_id", "profile_id", "clan_id", "season_rating", "donation", "donation_curr_season", "donation_prev_season"
                            , "donation_curr_season_comeback", "donation_prev_season_comeback", "cashed_medals"},
                    new Object[]{member.socialId, member.profileId, clanId, member.seasonRating, member.donation, member.donationCurrSeason, member.donationPrevSeason
                            , member.donationCurrSeasonComeback, member.donationPrevSeasonComeback, member.cashedMedals}));
        }
        return 0;
    }

    @Override
    @Transactional
    public void logClanAction(int clanId, int action, int publisherId, int memberId, int param, int treas) {
        String sql = "INSERT INTO clan.audit (clan_id, action, publisher_id, member_id, param, treas) VALUES " +
                " (:clan_id, :action, :publisher_id, :member_id, :param, :treas)";
        jdbcTemplate.update(sql, params(new String[]{"clan_id", "action", "clan_id", "publisher_id", "member_id", "param", "treas"},
                new Object[]{clanId, action, clanId, publisherId, memberId, param, treas}));
    }

    public List<ClanAuditActionTO> selectClanActions(int clanId) {
        try {
            String sql = "SELECT date, action, publisher_id, member_id, param FROM clan.audit WHERE clan_id = :clanId and date > now() - interval '1 MONTH'";
            return jdbcTemplate.query(sql, params(names("clanId"), values(clanId)), clanAuditActionMapper);
        } catch (DataAccessException e) {
            logger.error(e.toString());
            return Collections.emptyList();
        }
    }

    /**
     * Утилитные методы **
     */

    private <ENTITY> com.pragmatix.clanserver.dao.Dictionary<ENTITY> dictionary(final ENTITY entity) {
        return new com.pragmatix.clanserver.dao.Dictionary<ENTITY>() {
            @Override
            public ENTITY get(Integer id) {
                return entity;
            }
        };
    }

    private Dictionary<Clan> clanDictionary(final List<Clan> clans) {
        return new Dictionary<Clan>() {
            Map<Integer, Clan> dict = new HashMap<>();

            {
                for(Clan clan : clans) {
                    dict.put(clan.id, clan);
                }
            }

            @Override
            public Clan get(Integer id) {
                return dict.get(id);
            }
        };
    }

    private <ENTITY> ENTITY find(String sql, Map<String, Object> params, RowMapper<ENTITY> rowMapper) {
        List<ENTITY> entities = jdbcTemplate.query(sql, params, rowMapper);

        return entities.size() > 0 ? entities.get(0) : null;
    }

    private Map<String, Object> params(String[] names, Object[] values) {
        HashMap<String, Object> paramsMap = new HashMap<String, Object>();
        int i = 0;
        for(String name : names) {
            paramsMap.put(name, values[i++]);
        }
        return paramsMap;
    }

    private String[] names(String... names) {
        return names;
    }

    private Object[] values(Object... values) {
        return values;
    }

    static Integer getInt(ResultSet rs, String col) throws SQLException {
        Integer value = rs.getInt(col);

        if(rs.wasNull()) {
            value = null;
        }

        return value;
    }

    static String getStringNotNull(ResultSet rs, String col) throws SQLException {
        String value = rs.getString(col);

        if(value == null) {
            value = "";
        }

        return value;
    }

    static <ENUM extends Enum> ENUM getEnum(ResultSet rs, String col, Class<ENUM> enumClass) throws SQLException {
        String name = rs.getString(col);

        if(name != null) {
            return (ENUM) Enum.valueOf(enumClass, name);
        } else {
            return null;
        }
    }

}
