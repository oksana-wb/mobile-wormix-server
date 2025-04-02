package com.pragmatix.clanserver.domain;

/**
* Author: Vladimir
* Date: 23.04.13 13:36
*/
public class RatingItem {

    public Integer clanId;

//    public int rating;

    public int joinRating;

    /**
     * сезонный рейтинг
     */
    public int seasonRating;

    /**
     * позиция в ТОП всех кланов 30 мин. назад
     * равна 0 если не попал в ТОП кланов
     */
    transient public int oldPlace;

    public RatingItem() {
    }

    public RatingItem(Integer clanId, int joinRating, int seasonRating, int oldPlace) {
        this.clanId = clanId;
//        this.rating = rating;
        this.joinRating = joinRating;
        this.seasonRating = seasonRating;
        this.oldPlace = oldPlace;
    }

    public static int[] toClansId(RatingItem[] items) {
        int[] clansId = new int[items.length];
        int i = 0;
        for (RatingItem item: items) {
            clansId[i++] = item.clanId;
        }
        return clansId;
    }

    @Override
    public String toString() {
        return "RatingItem{" +
                "clanId=" + clanId +
//                ", rating=" + rating +
                ", joinRating=" + joinRating +
                ", seasonRating=" + seasonRating +
                ", oldPlace=" + oldPlace +
                '}';
    }
}
