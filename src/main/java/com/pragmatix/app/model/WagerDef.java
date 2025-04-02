package com.pragmatix.app.model;

import com.pragmatix.pvp.BattleWager;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.02.2016 15:27
 */
public class WagerDef {

    public final BattleWager wager;
    public final int team;
    public final int value;
    public final int award;

    public WagerDef(BattleWager wager, int team, int value, int award) {
        this.wager = wager;
        this.team = team;
        this.value = value;
        this.award = award;
    }

    public WagerDef setTeam(int team) {
        return new WagerDef(this.wager, team, this.value, this.award);
    }

    @Override
    public String toString() {
        return "WagerDef{" +
                "wager=" + wager +
                ", team=" + team +
                ", value=" + value +
                ", award=" + award +
                '}';
    }
}
