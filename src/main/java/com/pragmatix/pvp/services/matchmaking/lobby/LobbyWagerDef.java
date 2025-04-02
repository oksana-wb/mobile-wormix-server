package com.pragmatix.pvp.services.matchmaking.lobby;

import com.pragmatix.pvp.BattleWager;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 26.01.2018 15:24
 */
public class LobbyWagerDef {

    public BattleWager wager;

    public boolean matchBySameIp = true;

    public LobbyWagerDef(BattleWager wager) {
        this.wager = wager;
    }

    public LobbyWagerDef(BattleWager wager, boolean matchBySameIp) {
        this.wager = wager;
        this.matchBySameIp = matchBySameIp;
    }

    public BattleWager getWager() {
        return wager;
    }
}
