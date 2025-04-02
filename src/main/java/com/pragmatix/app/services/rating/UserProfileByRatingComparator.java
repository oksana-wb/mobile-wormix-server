package com.pragmatix.app.services.rating;

import com.pragmatix.app.messages.structures.RatingProfileStructure;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator для упорядочевания
 * профайлов по рейтингу
 * <p/>
 * User: denis
 * Date: 23.04.2010
 * Time: 1:31:15
 */
public class UserProfileByRatingComparator implements Comparator<RatingProfileStructure>, Serializable {
    @Override
    public int compare(RatingProfileStructure o1, RatingProfileStructure o2) {
        if(o1.equals(o2)) {
            return 0;
        }
        if(o1.rating > o2.rating) {
            return -1;
        } else if(o1.rating < o2.rating) {
            return 1;
        } else if(o1.id > o2.id) {
            return -1;
        } else if(o1.id < o2.id) {
            return 1;
        } else {
            return 0;
        }
    }
}