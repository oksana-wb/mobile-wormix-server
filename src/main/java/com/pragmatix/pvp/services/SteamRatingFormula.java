package com.pragmatix.pvp.services;

import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import io.vavr.Function2;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.function.Function;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 24.05.2016 13:50
 */
public class SteamRatingFormula extends RankBasedRatingFormula {

    private int fixedRank = 10;

    private int fixedLevel = 30;

    @Override
    protected Function<BattleParticipant, Tuple2<Integer, Integer>> getMyRankAndLevel() {
        return (BattleParticipant battleParticipant) -> Tuple.of(fixedRank, fixedLevel);
    }

    @Override
    protected Function2<BattleBuffer, BattleParticipant, Tuple2<Integer, Integer>> getEnemyRankAndLevel() {
        return (BattleBuffer battleBuffer, BattleParticipant battleParticipant) -> Tuple.of(fixedRank, fixedLevel);
    }

    public int getFixedRank() {
        return fixedRank;
    }

    public void setFixedRank(int fixedRank) {
        this.fixedRank = fixedRank;
    }

    public int getFixedLevel() {
        return fixedLevel;
    }

    public void setFixedLevel(int fixedLevel) {
        this.fixedLevel = fixedLevel;
    }
}
