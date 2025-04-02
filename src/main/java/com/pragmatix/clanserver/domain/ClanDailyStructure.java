package com.pragmatix.clanserver.domain;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 24.07.13 17:56
 */
public class ClanDailyStructure {

    private int expelCount = 0;

    private boolean donate = false;

    public int getExpelCount() {
        return expelCount;
    }

    public int incExpelCount() {
        return expelCount++;
    }

    public void setExpelCount(int expelCount) {
        this.expelCount = expelCount;
    }

    public boolean isDonate() {
        return donate;
    }

    public void donate() {
        this.donate = true;
    }
}
