package com.pragmatix.clanserver.dao.jdbc;

import com.pragmatix.clanserver.messages.structures.ClanAuditActionTO;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

class ClanAuditActionMapper implements RowMapper<ClanAuditActionTO> {

    @Override
    public ClanAuditActionTO mapRow(ResultSet rs, int i) throws SQLException {
        ClanAuditActionTO result = new ClanAuditActionTO();
        result.date = (int) (rs.getTimestamp("date").getTime() / 1000L);
        result.action = rs.getShort("action");
        result.memberId = rs.getInt("member_id");
        result.publisherId = rs.getInt("publisher_id");
        result.param = rs.getInt("param");
        return result;
    }

}
