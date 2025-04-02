package com.pragmatix.clanserver.dao.jdbc;

import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ReviewState;
import com.pragmatix.clanserver.domain.Season;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
* Author: Vladimir
* Date: 04.04.13 12:06
*/
class SeasonMapper implements RowMapper<Season> {

    @Override
    public Season mapRow(ResultSet rs, int i) throws SQLException {
        return new Season(rs.getInt("id"), rs.getDate("start"), rs.getDate("finish"), rs.getBoolean("closed"));
    }

}
