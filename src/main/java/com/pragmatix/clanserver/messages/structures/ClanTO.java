package com.pragmatix.clanserver.messages.structures;

import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.domain.ReviewState;
import com.pragmatix.clanserver.services.RatingService;
import com.pragmatix.clanserver.services.RatingServiceImpl;
import com.pragmatix.clanserver.utils.Utils;
import com.pragmatix.common.utils.ArrayUtils;
import com.pragmatix.serialization.annotations.Structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Author: Vladimir
 * Date: 05.04.13 20:46
 */
@Structure
public class ClanTO {
    public static final ClanTO[] EMPTY_ARRAY = new ClanTO[0];
    public static final ClanTO NULL_CLAN;

    public static final int SCOPE_HEADER = 0;
    public static final int SCOPE_MEMBER_HEADER = 1;
    public static final int SCOPE_MEMBER_DETAILS = SCOPE_MEMBER_HEADER | 2;
    public static final int SCOPE_MEMBER_ALL = SCOPE_MEMBER_DETAILS;
    public static final int SCOPE_ALL = 0xFFFF;

    static {
        NULL_CLAN = new ClanTO();
        NULL_CLAN.emblem = ArrayUtils.NULL_BYTES;
        NULL_CLAN.reviewState = ReviewState.NONE;
        NULL_CLAN.members = ClanMemberTO.EMPTY_ARRAY;
    }

    public int id;

    public String name;

    public int level;

    public int size;

    public int rating;

    public int seasonRating;

    public int joinRating = -1;

    public int createDate;

    public byte[] emblem;

    public String description;

    public ReviewState reviewState;

    public int topPlace;

    /**
     * место занимаемое кланов в предыдущем (закрытом) сезоне
     */
    public int prevSeasonTopPlace;

    public ClanMemberTO[] members = ClanMemberTO.EMPTY_ARRAY;

    public boolean closed = false;

    public int treas;

    public byte medalPrice;

    public int cashedMedals;

    public ClanTO() {
    }

    public ClanTO(Clan clan, RatingServiceImpl ratingService) {
        this(clan, SCOPE_ALL, ratingService);
    }

    public ClanTO(Clan clan, int scope, RatingServiceImpl ratingService) {
        id = clan.id;
        name = clan.name;
        level = clan.level;
        size = clan.size;
        rating = clan.rating;
        seasonRating = clan.seasonRating;
        joinRating = clan.joinRating;
        createDate = Utils.toSeconds(clan.createDate);
        emblem = clan.emblem;
        description = clan.description;
        reviewState = clan.reviewState;
        topPlace = ratingService.seasonPosition(id);
        prevSeasonTopPlace = clan.prevSeasonTopPlace;

        if ((scope & SCOPE_MEMBER_HEADER) == SCOPE_MEMBER_HEADER) {
            List<ClanMemberTO> membersList = new ArrayList<>(clan.members().size());
            for (ClanMember clanMember : clan.members()) {
                membersList.add(new ClanMemberTO(clanMember, scope, ratingService.weeklyRating(clanMember), ratingService.yesterdayRating(clanMember)));
            }
            members = membersList.toArray(new ClanMemberTO[membersList.size()]);
        } else {
            members = ClanMemberTO.EMPTY_ARRAY;
        }
        closed = clan.closed;
        treas = clan.treas;
        medalPrice = clan.medalPrice;
        cashedMedals = clan.cashedMedals;
    }

    public static ClanTO[] convert(Clan[] clans, int scope, RatingServiceImpl ratingService) {
        ClanTO[] res = new ClanTO[clans.length];
        int i = 0;
        for (Clan clan: clans) {
            res[i++] = new ClanTO(clan, scope, ratingService);
        }
        return res;
    }

    public Clan update(Clan target) {
        target.name = name;
        target.level = level;
        target.joinRating = joinRating;
        target.description = description;
        target.reviewState = reviewState;

        target.closed = closed;
        target.treas = treas;
        target.medalPrice = medalPrice;
        target.cashedMedals = cashedMedals;

        return target;
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", level=" + level +
                ", size=" + size +
//                ", rating=" + rating +
                ", seasonRating=" + seasonRating +
                ", joinRating=" + joinRating +
                ", createDate=" + createDate +
//                ", emblem=" + Arrays.toString(emblem) +
                ", description='" + description + '\'' +
                ", reviewState=" + reviewState +
                ", prevSeasonTopPlace=" + prevSeasonTopPlace +
                ", closed=" + closed +
                ", treas=" + treas +
                ", members=" + Arrays.toString(members) +
                '}';
    }
}
