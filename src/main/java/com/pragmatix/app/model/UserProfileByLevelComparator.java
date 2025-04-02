package com.pragmatix.app.model;

import java.util.Comparator;

/**
 * Author: Oksana Shevchenko
 * Date: 03.11.2010
 * Time: 1:02:53
 */
public class UserProfileByLevelComparator implements Comparator<UserProfile> {

    public int compare(UserProfile o1, UserProfile o2) {
        if (o1.getLevel() > o2.getLevel()) {
            return -1;
        } else if (o1.getLevel() < o2.getLevel()) {
            return 1;
        } else if (o1.getId() > o2.getId()) {
            return -1;
        } else if (o1.getId() < o2.getId()) {
            return 1;
        } else {
            return 0;
        }
    }
}