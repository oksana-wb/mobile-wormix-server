package com.pragmatix.clanserver.dao.jdbc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pragmatix.clanserver.dao.Dictionary;
import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.clanserver.services.RatingServiceImpl;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
* Author: Vladimir
* Date: 04.04.13 12:06
*/
class ClanMemberMapper implements RowMapper<ClanMember> {

    Dictionary<Clan> clanDict;

    RatingServiceImpl ratingService;

    ClanMemberMapper(Dictionary<Clan> clanDict, RatingServiceImpl ratingService) {
        this.clanDict = clanDict;
        this.ratingService = ratingService;
    }

    @Override
    public ClanMember mapRow(ResultSet rs, int index) throws SQLException {
        ClanMember clanMember = new ClanMember();
        clanMember.profileId = rs.getInt("profile_id");
        clanMember.socialProfileId = rs.getString("social_profile_id");
        clanMember.clan = clanDict.get(JdbcDAO.getInt(rs, "clan_id"));
        clanMember.rank = Rank.valueOf(rs.getInt("rank"));
        clanMember.name = rs.getString("name");
        clanMember.joinDate = rs.getTimestamp("join_date");
        clanMember.loginDate = rs.getTimestamp("login_date");
        clanMember.logoutDate = rs.getTimestamp("logout_date");
        clanMember.rating = rs.getInt("rating");
        clanMember.seasonRating = rs.getInt("season_rating");

        Date lastLoginTime = rs.getDate("last_login_time");
        clanMember.lastLoginTime = lastLoginTime !=null ?  (int) (lastLoginTime.getTime() / 1000L) : 0;

        clanMember.donation = rs.getInt("donation");
        clanMember.donationCurrSeason = rs.getInt("donation_curr_season");
        clanMember.donationPrevSeason = rs.getInt("donation_prev_season");
        clanMember.donationCurrSeasonComeback = rs.getInt("donation_curr_season_comeback");
        clanMember.donationPrevSeasonComeback = rs.getInt("donation_prev_season_comeback");
        clanMember.cashedMedals = rs.getInt("cashed_medals");
        clanMember.expelPermit = rs.getBoolean("expel_permit");
        clanMember.muteMode = rs.getBoolean("mute_mode");
        clanMember.hostProfileId = rs.getInt("host_profile_id");
        clanMember.dailyRating = ratingService.dailyRatingFromGson(rs.getString("daily_rating"));

        return clanMember;
    }
}
