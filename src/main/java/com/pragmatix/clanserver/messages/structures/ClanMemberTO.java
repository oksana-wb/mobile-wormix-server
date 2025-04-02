package com.pragmatix.clanserver.messages.structures;

import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.serialization.DateTimeType;
import com.pragmatix.serialization.annotations.DateTime;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Date;

/**
 * Author: Vladimir
 * Date: 05.04.13 9:29
 */
@Structure
public class ClanMemberTO {
    public static final ClanMemberTO[] EMPTY_ARRAY = new ClanMemberTO[0];

    @Ignore
    public short socialId;

    public int profileId;

    public String socialProfileId = "";

    public int clanId;

    public Rank rank;

    public String name;

    public boolean online;

    public int rating;

    public int seasonRating;

    /**
     * позиция в ТОП-е клана сутки назад
     */
    public int oldPlace;

    // в секундах
    public int lastLoginTime;

    public int donation;

    public int donationCurrSeason;

    public int donationPrevSeason;

    public int cashedMedals;

    // возможность включить/отключит офицерам выгонять участников
    public boolean expelPermit = true;
    // возможность включить/отключить молчанку для игрока
    public boolean muteMode = false;

    @DateTime(DateTimeType.SECONDS)
    public Date joinDate;

    public int hostProfileId;

    //набитый рейтнг за последние 7 дней
    public int weeklyRating;

    //набитый рейтнг за вчера
    public int yesterdayRating;

    public ClanMemberTO() {
    }

    public ClanMemberTO(ClanMember clanMember, int scope, int weeklyRating, int yesterdayRating) {
        this.clanId = clanMember.getClanId();
        this.rank = clanMember.rank;
        this.name = clanMember.name;
        if((scope & ClanTO.SCOPE_MEMBER_DETAILS) == ClanTO.SCOPE_MEMBER_DETAILS) {
            this.socialId = clanMember.socialId;
            this.profileId = clanMember.profileId;
            this.socialProfileId = clanMember.socialProfileId;
            this.online = clanMember.isOnline();
        }
        this.rating = clanMember.rating;
        this.seasonRating = clanMember.seasonRating;
        this.oldPlace = clanMember.oldPlace;

        this.lastLoginTime = clanMember.lastLoginTime;

        this.donation = clanMember.donation;
        this.donationCurrSeason = clanMember.donationCurrSeason;
        this.donationPrevSeason = clanMember.donationPrevSeason;
        this.cashedMedals = clanMember.cashedMedals;
        this.expelPermit = clanMember.expelPermit;
        this.muteMode = clanMember.muteMode;
        this.joinDate = clanMember.joinDate;
        this.hostProfileId = clanMember.hostProfileId;
        this.weeklyRating = weeklyRating;
        this.yesterdayRating = yesterdayRating;
    }

    @Override
    public String toString() {
        return "{" +
                "profileId=" + profileId +
                ", clanId=" + clanId +
                ", rank=" + rank +
                ", name='" + name + '\'' +
                ", online=" + online +
                ", seasonRating=" + seasonRating +
                ", oldPlace=" + oldPlace +
                ", donation=" + donation +
                ", donationCurrSeason=" + donationCurrSeason +
                ", donationPrevSeason=" + donationPrevSeason +
                ", cashedMedals=" + cashedMedals +
                ", expelPermit=" + expelPermit +
                ", muteMode=" + muteMode +
                ", joinDate=" + joinDate +
                ", weeklyRating=" + weeklyRating +
                ", yesterdayRating=" + yesterdayRating +
                ", hostProfileId=" + hostProfileId +
                '}';
    }
}
