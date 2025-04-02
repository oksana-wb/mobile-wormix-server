package com.pragmatix.achieve.common;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.09.2015 10:53
 */
public class BonusItem {

    public final int itemId;

    public final int level;

    public final int requires;

    public final int replaces ;

    public BonusItem(int itemId, int level, int requires, int replaces ) {
        this.itemId = itemId;
        this.level = level;
        this.requires = requires;
        this.replaces  = replaces ;
    }

    @Override
    public String toString() {
        return "BonusItem{" +
                "itemId=" + itemId +
                ", level=" + level +
                ", requires=" + requires +
                ", replaces=" + replaces +
                '}';
    }

}
