package com.pragmatix.pvp.messages.battle.server;

import com.pragmatix.app.common.PvpBattleResult;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.05.2016 11:03
 */
public class EndPvpBattleResultStructure {

    public final PvpBattleResult battleResult;
    /**
     * начисленный рейтинг по итогам боя
     */
    public final int ratingPoints;
    /**
     * начисленные rankPoints по итогам боя
     */
    public final int rankPoints;

    public EndPvpBattleResultStructure(PvpBattleResult battleResult, int ratingPoints, int rankPoints) {
        this.battleResult = battleResult;
        this.ratingPoints = ratingPoints;
        this.rankPoints = rankPoints;
    }

    @Override
    public String toString() {
        return "{" +
                ", result=" + battleResult +
                ", ratingPoints=" + ratingPoints +
                ", rankPoints=" + rankPoints +
                '}';
    }

}
