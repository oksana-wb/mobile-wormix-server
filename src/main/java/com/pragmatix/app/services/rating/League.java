package com.pragmatix.app.services.rating;

import java.io.Serializable;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 16.05.11 17:32
 */
public class League  implements Serializable{

    private static final long serialVersionUID = -787222555936904630L;

    private int index;
    private int min;
    private int max;
    private int divisionCount;

    public League() {
    }

    public League(int min, int max, int divisionCount) {
        this.min = min;
        this.max = max;
        this.divisionCount = divisionCount;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getDivisionCount() {
        return divisionCount;
    }

    public void setDivisionCount(int divisionCount) {
        this.divisionCount = divisionCount;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "League{" +
                "index=" + index +
                ", min=" + min +
                ", max=" + max +
                ", divisionCount=" + divisionCount +
                '}';
    }
}
