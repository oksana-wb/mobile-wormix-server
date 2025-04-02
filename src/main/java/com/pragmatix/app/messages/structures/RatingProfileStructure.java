package com.pragmatix.app.messages.structures;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clan.structures.ClanMemberStructure;
import com.pragmatix.serialization.annotations.Structure;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Профайл игрока из рейтинга
 * User: denis
 * Date: 23.06.2010
 * Time: 0:06:22
 */
@Structure
public class RatingProfileStructure extends SimpleProfileStructure implements Serializable {

    private static final long serialVersionUID = -3623398482624254947L;
    /**
     * Рейтинг игрока за день (или rankPoints за зезон)
     */
    public int ratingPoints;

    /**
     * позиция в ТОП 1000 30 мин. назад
     * равна 0 если не попал в ТОП 1000
     */
    transient public volatile int oldPlace;

    public RatingProfileStructure() {
    }

    public RatingProfileStructure(UserProfile profile, Callable<Tuple3<ClanMemberStructure, Byte, Byte>> clanMember_rank_skin) {
        super(profile, clanMember_rank_skin);
        this.ratingPoints = profile.getRankPoints();
    }

    public RatingProfileStructure(UserProfile profile, Callable<Tuple3<ClanMemberStructure, Byte, Byte>> clanMember_rank_skin, int rating, int oldPlace) {
        super(profile, clanMember_rank_skin);
        this.ratingPoints = rating;
        this.oldPlace = oldPlace;
    }

    public void init(UserProfile profile, Callable<Tuple3<ClanMemberStructure, Byte, Byte>> clanMember_rank_skin) {
        super.init(profile, clanMember_rank_skin);
        this.ratingPoints = profile.getRankPoints();
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        RatingProfileStructure that = (RatingProfileStructure) o;

        if(id != that.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + (StringUtils.isEmpty(profileStringId) ? "" : profileStringId + ":") + id +
                (StringUtils.isNoneEmpty(name) ?  ", name=" + name : "") +
                (rating > 0 ? ", rating=" + rating : "") +
                (ratingPoints > 0 ? ", ratingPoints=" + ratingPoints + ", rank=" + rank: "") +
                (oldPlace > 0 ? ", oldPlace=" + oldPlace : "") +
//                ", groupCount=" + groupCount +
                '}';
    }
}
