package com.pragmatix.clan.structures;

import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.clanserver.domain.ReviewState;
import com.pragmatix.serialization.annotations.Mark;
import com.pragmatix.serialization.annotations.Serialize;
import com.pragmatix.serialization.annotations.Structure;

import javax.validation.constraints.Null;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.04.13 18:17
 */
@Structure(annotatedOnly = true, nullable = true)
public class ClanMemberStructure {

    private ClanMember clanMember;
    private Clan clan;

    public ClanMemberStructure(@Null ClanMember clanMember) {
        init(clanMember);
    }

    public void init(@Null ClanMember clanMember) {
        this.clanMember = clanMember;
        if(clanMember != null) {
            clan = clanMember.clan;
        }
    }

    @Serialize
    public Rank getRank() {
        return clanMember != null && clanMember.rank != null ? clanMember.rank : Rank.SOLDIER;
    }

    @Serialize
    @Mark
    public int getClanId() {
        return clan != null ? clan.id : 0;
    }

    public int getClanLevel() {
        return clan != null ? clan.level : 0;
    }

    @Serialize
    public String getClanName() {
        return clan != null ? clan.name : "";
    }

    @Serialize
    public byte[] getClanEmblem() {
        return clan != null ? clan.emblem : new byte[0];
    }

    @Serialize
    public int getClanRating() {
        return clan != null ? clan.rating : 0;
    }

    @Serialize
    public int getSeasonClanRating() {
        return clan != null ? clan.seasonRating : 0;
    }

    @Serialize
    public ReviewState getReviewState() {
        return clan != null ? clan.reviewState : ReviewState.NONE;
    }

    @Serialize
    public int getPrevSeasonTopPlace() {
        return clan != null ? clan.prevSeasonTopPlace : 0;
    }

    public void setRank(Rank rank) {
        if(clanMember != null)
            clanMember.rank = rank;
    }

    public void setClanId(int clanId) {
        if(clan != null)
            clan.id = clanId;
    }

    public void setClanName(String clanName) {
        if(clan != null)
            clan.name = clanName;
    }

    public void setClanEmblem(byte[] clanEmblem) {
        if(clan != null)
            clan.emblem = clanEmblem;
    }

    public void setClanRating(int clanRating) {
        if(clan != null)
            clan.rating = clanRating;
    }

    public void setSeasonClanRating(int seasonClanRating) {
        if(clan != null)
            clan.seasonRating = seasonClanRating;
    }

    public void setReviewState(ReviewState reviewState) {
        if(clan != null)
            clan.reviewState = reviewState;
    }

    public void setPrevSeasonTopPlace(int prevSeasonTopPlace) {
        if(clan != null)
            clan.prevSeasonTopPlace = prevSeasonTopPlace;
    }

    @Override
    public String toString() {
        return "{" +
                getRank() +
                "/" + getClanId() +
                "/'" + getClanName() + '\'' +
//                ", clanRating=" + getClanRating() +
//                ", seasonClanRating=" + getSeasonClanRating() +
//                ", reviewState=" + getReviewState() +
//                ", prevSeasonTopPlace=" + getPrevSeasonTopPlace() +
                '}';
    }


    public ClanMember getClanMember() {
        return clanMember;
    }

    public Clan getClan() {
        return clan;
    }

}
