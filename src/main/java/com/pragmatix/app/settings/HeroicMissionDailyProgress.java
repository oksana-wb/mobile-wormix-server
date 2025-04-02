package com.pragmatix.app.settings;

import com.pragmatix.serialization.annotations.Serialize;
import com.pragmatix.serialization.annotations.Structure;
import org.apache.mina.util.ConcurrentHashSet;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 13.10.2016 13:49
 */
@Structure(annotatedOnly = true)
public class HeroicMissionDailyProgress implements Serializable {

    private static final long serialVersionUID = 6594377744410474054L;

    public AtomicInteger defeatCount = new AtomicInteger();

    public Set<Long> winners = new ConcurrentHashSet<>();

    @Serialize
    public int getDefeatCount() {
        return defeatCount.get();
    }

    @Serialize
    public int getWinCount() {
        return winners.size();
    }

    public void setDefeatCount(int defeatCount) {
        this.defeatCount = new AtomicInteger(defeatCount);
    }

    public void setWinCount(int winCount) {
    }

    @Override
    public String toString() {
        return "{" +
                "defeatCount=" + getDefeatCount() +
                ", winCount=" + getWinCount() +
                '}';
    }

}
