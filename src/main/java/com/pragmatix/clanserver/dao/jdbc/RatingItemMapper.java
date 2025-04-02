package com.pragmatix.clanserver.dao.jdbc;

import com.pragmatix.clanserver.domain.RatingItem;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Author: Vladimir
 * Date: 23.04.13 15:09
 */
public class RatingItemMapper implements RowMapper<RatingItem> {
    @Override
    public RatingItem mapRow(ResultSet rs, int i) throws SQLException {
        RatingItem item = new RatingItem();
        item.clanId = rs.getInt(1);
//        item.rating = rs.getInt(2);
        item.joinRating = rs.getInt(3);
        item.seasonRating = rs.getInt(4);
        return item;
    }
}
