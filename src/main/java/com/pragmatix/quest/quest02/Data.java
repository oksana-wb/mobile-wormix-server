package com.pragmatix.quest.quest02;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.06.2016 12:31
 */
public class Data {

    public Date startDate;
    public short win;
    public volatile Date finishedDate;
    public boolean rewarded;

    public boolean isEmpty() {
        return startDate == null && win == 0 && finishedDate == null;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        Data data = (Data) o;

        if(win != data.win) return false;
        if(rewarded != data.rewarded) return false;
        if(startDate != null ? !startDate.equals(data.startDate) : data.startDate != null) return false;
        return finishedDate != null ? finishedDate.equals(data.finishedDate) : data.finishedDate == null;

    }

    @Override
    public int hashCode() {
        int result = startDate != null ? startDate.hashCode() : 0;
        result = 31 * result + (int) win;
        result = 31 * result + (finishedDate != null ? finishedDate.hashCode() : 0);
        result = 31 * result + (rewarded ? 1 : 0);
        return result;
    }
}
