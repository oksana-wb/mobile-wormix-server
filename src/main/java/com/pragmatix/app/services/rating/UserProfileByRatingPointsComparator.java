package com.pragmatix.app.services.rating;

import com.pragmatix.app.messages.structures.RatingProfileStructure;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator для упорядочевания
 * профайлов по сезонному рейтингу
 */
public class UserProfileByRatingPointsComparator implements Comparator<RatingProfileStructure>, Serializable {

    @Override
    public int compare(RatingProfileStructure o1, RatingProfileStructure o2) {
        if(o1.equals(o2)) {
            return 0;
        }
        int result = Integer.compare(o1.ratingPoints, o2.ratingPoints) * -1;
        if(result == 0)
            result = Long.compare(o1.id, o2.id);
        return result;
    }

}