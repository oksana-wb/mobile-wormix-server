package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.RatingItem;

/**
 * Author: Vladimir
 * Date: 23.04.13 15:29
 */
public interface RatingService {
    RatingItem[] topN(int count, boolean season);

    void updateRatings(Integer clanId, int rating, int seasonRating, int joinRating);

    void updateRatings(Clan clan);

    int position(Integer clanId, boolean season);

    int seasonPosition(Integer clanId);

    RatingItem removeRating(Integer clanId);

    void reloadTop();

    RatingItem[] joinsN(int rating, int count);
}
