package com.pragmatix.clanserver.domain;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 10.10.13 16:29
 */
public class Season implements Cloneable {

    public int id;

    public Date start;

    public Date finish;

    public boolean closed;

    public Season(int id, Date start, Date finish, boolean closed) {
        this.id = id;
        this.start = start;
        this.finish = finish;
        this.closed = closed;
    }

    @Override
    public String toString() {
        return "Season{" +
                "id=" + id +
                ", start=" + start +
                ", finish=" + finish +
                ", closed=" + closed +
                '}';
    }

}
