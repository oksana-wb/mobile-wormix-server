package com.pragmatix.pvp;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.12.12 18:13
 */
public enum WagerValue {

    WV_0(0, /*hasRankPoints*/false),
    WV_15(15, /*hasRankPoints*/true),
    WV_20(20, /*hasRankPoints*/false),
    WV_50(50, /*hasRankPoints*/true),
    WV_300(300, /*hasRankPoints*/true),;

    /**
     * значение ставки в фузах
     */
    public final int value;
    public final boolean hasRankPoints;

    WagerValue(int value, boolean hasRankPoints) {
        this.value = value;
        this.hasRankPoints = hasRankPoints;
    }
}
