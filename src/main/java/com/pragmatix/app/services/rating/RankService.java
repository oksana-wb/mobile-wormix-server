package com.pragmatix.app.services.rating;

import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.init.LevelCreator;
import com.pragmatix.app.model.Level;
import com.pragmatix.app.model.RankItem;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleKey;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import io.vavr.Function2;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.pragmatix.app.common.PvpBattleResult.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 19.05.2016 12:31
 */
public class RankService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public final static byte INIT_RANK_VALUE = 20;

    @Resource
    private LevelCreator levelCreator;

    //base amount of points used in all calculations
    //represents points gain for a full team on level 30
    private int maxRatingPoints = 70;

    //special parameter, so we could regulate points lost to defeat for each social network
    //we can also change it during season if we find players' progress towards rank0 too slow or too fast
    //private double socialNetworkDefeatModifier = 1.0;

    private Map<SocialServiceEnum, Double> networkDefeatModifiers = new EnumMap<>(SocialServiceEnum.class);

    public List<RankItem> ranks;

    public int[] hpByLevel;

    public double[] teamSizeModifiers = new double[]{1.0, 1.0, 1.0, 1.0};

    public Map<PvpBattleKey, Double> battleModeBonus = Collections.emptyMap();

    public Map<PvpBattleKey, Integer> overrideTeamSize = Collections.emptyMap();

    private boolean enabled;

    @PostConstruct
    public void init() {
        hpByLevel = levelCreator.getLevels().values().stream().mapToInt(Level::getLevelHp).toArray();
        int acc = 0;
        for(int i = ranks.size() - 1; i >= 0; i--) {
            RankItem rank = ranks.get(i);
            rank.needPoints = acc;
            acc += rank.pointsToNext;
        }
    }

    public byte getPlayerRealRank(UserProfile profile) {
        return getPlayerRealRank(profile.getBestRank(), profile.getRankPoints());
    }

    public byte getPlayerRealRank(BattleParticipant profile) {
        return getPlayerRealRank(profile.bestRank, profile.profileRankPoints);
    }

    public byte getPlayerRealRank(int bestRank, int rankPoints) {
        return (byte) (bestRank == 0 ? 0 : getPlayerRankValue(rankPoints));
    }

    public void onSetRankPoints(UserProfile profile) {
        profile.setBestRank(Math.min(profile.getBestRank(), getPlayerRankValue(profile.getRankPoints())));
    }

    public int getVictoryRatingPoints(BattleBuffer battleBuffer, BattleParticipant battleParticipant,
                                      Function2<BattleBuffer, BattleParticipant, Tuple2<Integer, Integer>> enemyRankAndLevelFun,
                                      Function<BattleParticipant, Tuple2<Integer, Integer>> myRankAndLevelFun
    ) {
        // если сезонный рейтинг ушел в минус, рейтинг не растет
        if (battleParticipant.profileRankPoints < 0)
            return 0;
        Tuple2<Integer, Integer> enemyRankAndLevel = enemyRankAndLevelFun.apply(battleBuffer, battleParticipant);
        int enemyRank = enemyRankAndLevel._1;
        int enemyLevel = enemyRankAndLevel._2;
        Tuple2<Integer, Integer> myRankAndLevel = myRankAndLevelFun.apply(battleParticipant);
        int myRank = myRankAndLevel._1;
        int myLevel = myRankAndLevel._2;
        PvpBattleKey pvpBattleKey = PvpBattleKey.valueOf(battleBuffer.getBattleType(), battleBuffer.getWager());
        return getVictoryRatingPoints(battleParticipant.getTeamSize(), enemyRank, enemyLevel, myRank, myLevel, pvpBattleKey);
    }

    //calculation of RATING points gained for victory
    public int getVictoryRatingPoints(int teamSize, int enemyRank, int enemyLevel, int myRank, int myLevel, PvpBattleKey pvpBattleKey) {
        RankItem rankItem = ranksGet(myRank);
        double enemyRankModifier = getEnemyRankModifier(myRank, enemyRank, true);
        double enemyLevelModifier = getEnemyLevelModifier(myLevel, enemyLevel, true);
        int basePoints = getBasePoints(myLevel, teamSize, pvpBattleKey);
        double rating = enemyRankModifier * enemyLevelModifier * rankItem.victoryBonus * basePoints;
        return (int) Math.round(rating);
    }

    private boolean isNeedCountRating(PvpBattleResult result, BattleWager wager) {
        return (result == NOT_WINNER || result == WINNER || result == DRAW_GAME) && wager.getWagerValue().hasRankPoints;
    }

    public int getRankPoints(BattleBuffer battleBuffer, BattleParticipant battleParticipant, PvpBattleResult result) {
        BattleWager wager = battleBuffer.getWager();
        if (!isNeedCountRating(result, wager)) {
            return 0;
        } else {
            Tuple2<Integer, Integer> enemyRankAndLevel = enemyRankAndLevel(battleBuffer, battleParticipant);
            int enemyRank = enemyRankAndLevel._1;
            int enemyLevel = enemyRankAndLevel._2;
            PvpBattleKey pvpBattleKey = PvpBattleKey.valueOf(battleBuffer.getBattleType(), battleBuffer.getWager());
            int rankPoints = getRankPoints(battleParticipant, enemyRank, enemyLevel, pvpBattleKey, result == PvpBattleResult.WINNER);
            if (result == PvpBattleResult.DRAW_GAME) {
                //За ничью надо списывать 25% от очков за поражение, только для игроков из рубиновой лиги
                rankPoints = battleParticipant.bestRank == 0 ? rankPoints / 4 : 0;
            }
            return rankPoints;
        }
    }

    public Tuple2<Integer, Integer> myRankAndLevel(BattleParticipant profile) {
        int myRank = getPlayerRankValue(profile.profileRankPoints);
        int myLevel = profile.getLevel();
        return Tuple.of(myRank, myLevel);
    }

    public Tuple2<Integer, Integer> enemyRankAndLevel(BattleBuffer battleBuffer, BattleParticipant battleParticipant) {
        double enemyRankPointsSum = 0;
        double enemyLevelSum = 0;
        double opponents = 0;
        int enemyBestRank = INIT_RANK_VALUE;
        for(BattleParticipant participant : battleBuffer.getParticipants()) {
            if (participant.getPlayerTeam() != battleParticipant.getPlayerTeam()) {
                enemyRankPointsSum += participant.getProfileRankPoints();
                enemyLevelSum += participant.getLevel();
                enemyBestRank = Math.min(enemyBestRank, participant.bestRank);
                opponents++;
            }
        }
        int enemyRankPoints = (int) Math.round(enemyRankPointsSum / opponents);
        int enemyRank = getPlayerRealRank(enemyBestRank, enemyRankPoints);

        int enemyLevel = (int) Math.round(enemyLevelSum / opponents);

        return Tuple.of(enemyRank, enemyLevel);
    }

    private int getRankPoints(BattleParticipant profile, int enemyRank, int enemyLevel, PvpBattleKey pvpBattleKey, boolean isVictory) {
        int rankPoints = profile.getProfileRankPoints();
        int myRank = getPlayerRealRank(profile);
        RankItem rankItem = ranksGet(myRank);
        double enemyRankModifier = getEnemyRankModifier(myRank, enemyRank, isVictory);
        double enemyLevelModifier = getEnemyLevelModifier(profile.getLevel(), enemyLevel, isVictory);
        int basePoints = getBasePoints(profile.getLevel(), profile.getTeamSize(), pvpBattleKey);

        double resultPoints;
        if (isVictory) {
            //victory points are limited by current rank
            resultPoints = (int) Math.round(enemyRankModifier * enemyLevelModifier * rankItem.getVictoryPoints(basePoints));
        } else {
            //defeat points are not limited but can be modified by socialNetworkDefeatModifier
            int baseDefeatPoints = (myRank == 0 ? -maxRatingPoints : rankItem.getDefeatPoints());
            resultPoints = (int) Math.round(getSocialNetworkDefeatModifier(profile.getSocialNetId()) * enemyRankModifier * enemyLevelModifier * baseDefeatPoints);

            int pointsOnCurrentRank = rankPoints - rankItem.needPoints;
            if (myRank > 0 && Math.abs(resultPoints) > pointsOnCurrentRank) {
                //special case when player looses the rank after defeat
                //we limit the total loss, leaving player in a position where they can restore rank,
                //if they manage to win the next battle
                double prevRankTotalLoss = Math.abs(resultPoints) - pointsOnCurrentRank;
                int prevRankVictoryPoints = ranksGet(myRank + 1).getVictoryPoints(basePoints);
                resultPoints = -1 * (pointsOnCurrentRank + Math.min(prevRankTotalLoss, prevRankVictoryPoints / 2));
            }
        }

        //special routine for rank0
        if (myRank == 0) {
            if (isVictory) {
                resultPoints *= sigma(rankPoints);
            } else {
                resultPoints -= looseCut(rankPoints);
            }
        }
        return (int) Math.round(resultPoints);
    }

    private RankItem ranksGet(int myRank) {
        try {
            return ranks.get(myRank);
        } catch (Exception e) {
            log.error(e.toString(), e);
            return ranks.get(ranks.size() - 1);
        }
    }

    private double getSocialNetworkDefeatModifier(byte socialNetId) {
        return networkDefeatModifiers.getOrDefault(SocialServiceEnum.valueOf(socialNetId), 1.0);
    }

    public int getPlayerRankValue(int rankPoints) {
        for(RankItem rank : ranks) {
            if (rankPoints > rank.needPoints)
                return rank.rank;
        }
        return ranks.get(ranks.size() - 1).rank;
    }

    private double getEnemyRankModifier(int myRank, int enemyRank, boolean isVictory) {
        double baseModifier = (myRank - enemyRank) * 0.01;
        return (isVictory ? 1.0 + baseModifier : 1.0 - baseModifier);
    }

    private double getEnemyLevelModifier(int myLevel, int enemyLevel, boolean isVictory) {
        double myHp = hpByLevel[myLevel];
        double enemyHp = hpByLevel[enemyLevel];
        return (isVictory ? enemyHp / myHp : myHp / enemyHp);
    }

    //calculates, how much the battle "worths" for a player, based on his level, team size and battle mode
    private int getBasePoints(int level, int teamSize, PvpBattleKey battleMode) {
        double levelModifier = Math.pow(0.03 * level + 0.1, 1.44);
        double battleModeBonus = this.battleModeBonus.getOrDefault(battleMode, 1.0);
        return (int) Math.round(maxRatingPoints * teamSizeModifiers[overrideTeamSize.getOrDefault(battleMode, teamSize) - 1] * levelModifier * battleModeBonus);
    }

    private double sigma(int x) {
        return Math.max(0.1, 1 / (1 + Math.exp((x) / 1000 - 7)));
    }

    private double looseCut(int x) {
        int expLooseThreshold = 3000;
        return (x > expLooseThreshold ? Math.round(0.02 * (x - expLooseThreshold)) : 0);
    }

    //====================== Getters and Setters =================================================================================================================================================

    public void setRanks(List<RankItem> ranks) {
        this.ranks = ranks;
    }

    public void setBattleModeBonus(Map<PvpBattleKey, Double> battleModeBonus) {
        this.battleModeBonus = battleModeBonus;
    }

    public void setOverrideTeamSize(Map<PvpBattleKey, Integer> overrideTeamSize) {
        this.overrideTeamSize = overrideTeamSize;
    }

    public void setMaxRatingPoints(int maxRatingPoints) {
        this.maxRatingPoints = maxRatingPoints;
    }

    public void setTeamSizeModifiers(double[] teamSizeModifiers) {
        this.teamSizeModifiers = teamSizeModifiers;
    }

    public void setNetworkDefeatModifiers(Map<SocialServiceEnum, Double> networkDefeatModifiers) {
        this.networkDefeatModifiers = new EnumMap<>(networkDefeatModifiers);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
