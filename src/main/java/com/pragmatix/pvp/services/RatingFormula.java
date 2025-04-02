package com.pragmatix.pvp.services;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;

/**
 * Расчет набранных.потерянных очков рейтинга по итогам PVP боя
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 29.07.13 15:49
 */

public interface RatingFormula {
    /**
     * расчитать количество полученного (потерянного) рейтинга
     */
    int getRatingPoints(BattleBuffer battleBuffer, BattleParticipant battleParticipant, PvpBattleResult battleResult);

}
