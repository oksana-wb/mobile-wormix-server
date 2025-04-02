package com.pragmatix.clanserver.dao.jdbc;

import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ReviewState;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
* Author: Vladimir
* Date: 04.04.13 12:06
*/
class ClanMapper implements RowMapper<Clan> {
    @Override
    public Clan mapRow(ResultSet rs, int i) throws SQLException {
        Clan clan = new Clan();
        clan.id = rs.getInt("id");
        clan.name = rs.getString("name");
        clan.createDate = rs.getTimestamp("create_date");
        clan.level = rs.getInt("level");
        clan.size = rs.getInt("size");
        clan.rating = rs.getInt("rating");
        clan.seasonRating = rs.getInt("season_rating");
        clan.joinRating = rs.getInt("join_rating");
        clan.emblem = rs.getBytes("emblem");
        clan.description = JdbcDAO.getStringNotNull(rs, "description");
        clan.reviewState = ReviewState.getByCode(rs.getInt("review_state"));
        clan.prevSeasonTopPlace = rs.getInt("prev_top_place");

        clan.closed = rs.getBoolean("closed");
        clan.treas = rs.getInt("treas");
        clan.medalPrice = rs.getByte("medal_price");
        clan.cashedMedals = rs.getInt("cashed_medals");
        return clan;
    }
}
