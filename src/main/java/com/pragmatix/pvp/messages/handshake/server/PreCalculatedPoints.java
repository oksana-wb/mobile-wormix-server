package com.pragmatix.pvp.messages.handshake.server;

import com.pragmatix.pvp.messages.battle.server.EndPvpBattleResultStructure;
import com.pragmatix.serialization.annotations.Structure;

/**
 * Рейтинги просчитанные на случай победы или поражения игрока
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.05.2016 16:30
 */
@Structure
public class PreCalculatedPoints {
    /**
     * рейтинг по итогам боя
     */
    public int ratingPoints;
    /**
     * rankPoints по итогам боя
     */
    public int rankPoints;

    public PreCalculatedPoints() {
    }

    public PreCalculatedPoints(EndPvpBattleResultStructure endPvpBattleResultStructure) {
        this.ratingPoints = endPvpBattleResultStructure.ratingPoints;
        this.rankPoints = endPvpBattleResultStructure.rankPoints;
    }

    @Override
    public String toString() {
        return "{" +
                "ratingPoints=" + ratingPoints +
                ", rankPoints=" + rankPoints +
                '}';
    }

}
