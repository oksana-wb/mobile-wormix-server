package com.pragmatix.pvp.services.matchmaking.lobby;

import com.pragmatix.performance.statictics.StatCollector;
import com.pragmatix.performance.statictics.ValueHolder;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.services.matchmaking.lobby.LobbyWagerDef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Класс хранит настройки влияющие на подбор соперников в PVP
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 25.04.13 16:16
 */
@Service
public class LobbyConf {

    // сколько нужно подобрать соперников, максимальное значение (3 для 2х2)
    public static final int MAX_NEEDED_PARTICIPANTS = 3;
    /**
     * делать проверку что игроков нельзя соединять друг с другом в течении дня более чем maxBattlesWithSameUsers
     */
    @Value("${pvp.checkOpponents:true}")
    private boolean checkOpponents = true;
    /**
     * делать проверку что 2х игроков нельзя соединять 2 раза подряд
     */
    @Value("${pvp.checkLastOpponent:true}")
    private boolean checkLastOpponent = true;

    @Value("${pvp.maxBattlesWithSameUser:3}")
    private int maxBattlesWithSameUser = 3;

    @Value("${pvp.maxSurrenderedBattlesToSameUser:3}")
    private int maxSurrenderedBattlesToSameUser = 3;
    /**
     * используются ли черные списки
     */
    @Value("${pvp.checkFakes:true}")
    private boolean checkFakes = true;
    /**
     * контролируется ли слив рейтинга в течении дня
     */
    @Value("${pvp.checkPourDowners:true}")
    private boolean checkPourDowners = true;

    @Value("${pvp.checkClan:true}")
    private boolean checkClan = true;

    @Value("${pvp.warnNegativeRating:-300}")
    private int warnNegativeRating = -300;

    @Value("${pvp.minNegativeRating:-500}")
    private int minNegativeRating = -500;
    /**
     * количество съиграных боёв для выхода из песочницы
     */
    @Value("${pvp.sandboxBattlesDelimiter:0}")
    private int sandboxBattlesDelimiter = 0;
    /**
     * количество рейтинга для выхода из песочницы
     */
    @Value("${pvp.sandboxRatingDelimiter:0}")
    private int sandboxRatingDelimiter = 0;
    /**
     * максимальное значение уровня для попадания в песочницу
     */
    @Value("${pvp.sandboxMaxLevel:15}")
    private int sandboxMaxLevel = 15;
    /**
     * используем множитель при подборе уровня соперника
     */
    @Value("${pvp.useLevelDiffFactor:true}")
    private boolean useLevelDiffFactor = true;
    /**
     * разброс уровня владельцев команд
     */
    @Value("${pvp.levelDiffFactor:0.15}")
    private double levelDiffFactor = 0.15;
    /**
     * диапазон уровней противников (+- к уровню вызывающего) при подборе соперника
     */
    @Value("${pvp.enemyLevelRange:4}")
    private int enemyLevelRange = 4;

    /**
     * разброс "здоровья" команд при подборе противника
     */
    @Value("${pvp.hpDiffFactor:0.2}")
    private double hpDiffFactor = 0.2;

    @Value("${pvp.deltaHpDiffFactor:0.02}")
    private double deltaHpDiffFactor = 0.02;

    @Value("${pvp.maxHpDiffFactor:0.24}")
    private double maxHpDiffFactor = 0.24;
    /**
     * оптимальное сочетания мастерства противников
     */
    @Value("${pvp.bestMatchQuality:0.8}")
    private double bestMatchQuality = 0.8;

    @Value("${pvp.deltaMatchQuality:0.4}")
    private double deltaMatchQuality = 0.4;

    @Value("${pvp.ignoreMatchQualityForNoduelBattles:true}")
    private boolean ignoreMatchQualityForNoduelBattles = true;

    @Value("${pvp.ignoreMatchQualityForDuel300Battles:true}")
    private boolean ignoreMatchQualityForDuel300Battles = true;

    @Value("${pvp.matchDifferentPlatform:true}")
    private boolean matchDifferentPlatform = true;

    @Value("${pvp.mercenariesBattleLobby.winPercent:50}")
    private int mercenariesBattleLobbyWinPercent = 50;

    @Value("${pvp.rankThreshold:30}")
    private int rankThreshold = 30;

    @Value("${pvp.checkIp:true}")
    private boolean checkIp = true;

    public void init(StatCollector statCollector) {
        //boolean checkOpponents = true;
        //boolean checkLastOpponent = true;
        //boolean checkExcluded= true;
        //boolean checkPourDowners= true;
        //int maxBattlesWithSameUsers = 3;
        //int sandboxBattlesDelimiter = 0;
        //boolean useLevelDiffFactor = true;
        //double levelDiffFactor = 0.15;
        //int enemyLevelRange = 4;
        //double hpDiffFactor = 0.2;
        //double deltaHpDiffFactor = 0.02;
        //double maxHpDiffFactor = 0.24;
        //double bestMatchQuality = 0.8;
        //double deltaMatchQuality = 0.4;
        //int minNegativeRating = -500;
        String group = "lobbyConf";
        statCollector.needCollect(group, "checkOpponents", new ValueHolder() {
            @Override
            public long getValue() {
                return 0 + (checkOpponents ? 1 : 0);
            }
        }, false);
        statCollector.needCollect(group, "checkLastOpponent", new ValueHolder() {
            @Override
            public long getValue() {
                return 2 + (checkLastOpponent ? 1 : 0);
            }
        }, false);
        statCollector.needCollect(group, "checkFakes", new ValueHolder() {
            @Override
            public long getValue() {
                return 4 + (checkFakes ? 1 : 0);
            }
        }, false);
        statCollector.needCollect(group, "checkPourDowners", new ValueHolder() {
            @Override
            public long getValue() {
                return 6 + (checkPourDowners ? 1 : 0);
            }
        }, false);
        statCollector.needCollect(group, "minNegativeRating", new ValueHolder() {
            @Override
            public long getValue() {
                return minNegativeRating;
            }
        }, false);
        statCollector.needCollect(group, "maxBattlesWithSameUser", new ValueHolder() {
            @Override
            public long getValue() {
                return maxBattlesWithSameUser;
            }
        }, false);
        statCollector.needCollect(group, "sandboxBattlesDelimiter", new ValueHolder() {
            @Override
            public long getValue() {
                return sandboxBattlesDelimiter;
            }
        }, false);
        statCollector.needCollect(group, "sandboxRatingDelimiter", new ValueHolder() {
            @Override
            public long getValue() {
                return sandboxRatingDelimiter;
            }
        }, false);
        statCollector.needCollect(group, "sandboxMaxLevel", new ValueHolder() {
            @Override
            public long getValue() {
                return sandboxMaxLevel;
            }
        }, false);
        statCollector.needCollect(group, "useLevelDiffFactor", new ValueHolder() {
            @Override
            public long getValue() {
                return 8 + (useLevelDiffFactor ? 1 : 0);
            }
        }, false);
        statCollector.needCollect(group, "levelDiffFactor", new ValueHolder() {
            @Override
            public long getValue() {
                return (int) (levelDiffFactor * 1000);
            }
        }, false);
        statCollector.needCollect(group, "enemyLevelRange", new ValueHolder() {
            @Override
            public long getValue() {
                return enemyLevelRange;
            }
        }, false);
        statCollector.needCollect(group, "hpDiffFactor", new ValueHolder() {
            @Override
            public long getValue() {
                return (int) (hpDiffFactor * 1000);
            }
        }, false);
        statCollector.needCollect(group, "deltaHpDiffFactor", new ValueHolder() {
            @Override
            public long getValue() {
                return (int) (deltaHpDiffFactor * 1000);
            }
        }, false);
        statCollector.needCollect(group, "maxHpDiffFactor", new ValueHolder() {
            @Override
            public long getValue() {
                return (int) (maxHpDiffFactor * 1000);
            }
        }, false);
        statCollector.needCollect(group, "bestMatchQuality", new ValueHolder() {
            @Override
            public long getValue() {
                return (int) (bestMatchQuality * 1000);
            }
        }, false);
        statCollector.needCollect(group, "deltaMatchQuality", new ValueHolder() {
            @Override
            public long getValue() {
                return (int) (deltaMatchQuality * 1000);
            }
        }, false);
        statCollector.needCollect(group, "maxSurrenderedBattlesToSameUser", new ValueHolder() {
            @Override
            public long getValue() {
                return maxSurrenderedBattlesToSameUser;
            }
        }, false);
    }

    private final Map<BattleWager, LobbyWagerDef> lobbyWagerDefs = new EnumMap<>(Arrays.stream(BattleWager.values()).map(LobbyWagerDef::new).collect(Collectors.toMap(LobbyWagerDef::getWager, i -> i)));

    @Autowired(required = false)
    public void setLobbyWagerDefs(List<LobbyWagerDef> lobbyWagerDefs){
        lobbyWagerDefs.forEach(def -> this.lobbyWagerDefs.put(def.wager, def));
    }

    public boolean isMatchBySameIp(BattleWager wager){
       return !checkIp || lobbyWagerDefs.get(wager).matchBySameIp;
    }

//====================== Getters and Setters =================================================================================================================================================

    public boolean isCheckIp() {
        return checkIp;
    }

    public void setCheckIp(boolean checkIp) {
        this.checkIp = checkIp;
    }

    public boolean isCheckOpponents() {
        return checkOpponents;
    }

    public boolean isCheckLastOpponent() {
        return checkLastOpponent;
    }

    public void setCheckLastOpponent(boolean checkLastOpponent) {
        this.checkLastOpponent = checkLastOpponent;
    }

    public boolean isCheckFakes() {
        return checkFakes;
    }

    public void setCheckFakes(boolean checkFakes) {
        this.checkFakes = checkFakes;
    }

    public boolean isCheckPourDowners() {
        return checkPourDowners;
    }

    public void setCheckPourDowners(boolean checkPourDowners) {
        this.checkPourDowners = checkPourDowners;
    }

    public int getSandboxBattlesDelimiter() {
        return sandboxBattlesDelimiter;
    }

    public void setCheckOpponents(boolean checkOpponents) {
        this.checkOpponents = checkOpponents;
    }

    public double getLevelDiffFactor() {
        return levelDiffFactor;
    }

    public void setLevelDiffFactor(double levelDiffFactor) {
        this.levelDiffFactor = levelDiffFactor;
    }

    public int getEnemyLevelRange() {
        return enemyLevelRange;
    }

    public void setEnemyLevelRange(int enemyLevelRange) {
        this.enemyLevelRange = enemyLevelRange;
    }

    public int getSandboxBattlesDelimiter(BattleWager wager, int level) {
        // оставляем песочницу только на ставке 15 и на уровнях < 21
        return wager == BattleWager.WAGER_15_DUEL && level < sandboxMaxLevel ? sandboxBattlesDelimiter : 0;
    }

    public int getSandboxRatingDelimiter(BattleWager wager, int level) {
        // оставляем песочницу только на ставке 15 и на уровнях < 21
        return wager == BattleWager.WAGER_15_DUEL && level < sandboxMaxLevel ? sandboxRatingDelimiter : 0;
    }

    public void setMaxSurrenderedBattlesToSameUser(int maxSurrenderedBattlesToSameUser) {
        this.maxSurrenderedBattlesToSameUser = maxSurrenderedBattlesToSameUser;
    }

    public void setSandboxBattlesDelimiter(int sandboxBattlesDelimiter) {
        this.sandboxBattlesDelimiter = sandboxBattlesDelimiter;
    }

    public void setSandboxMaxLevel(int sandboxMaxLevel) {
        this.sandboxMaxLevel = sandboxMaxLevel;
    }

    public double getHpDiffFactor() {
        return hpDiffFactor;
    }

    public void setHpDiffFactor(double hpDiffFactor) {
        this.hpDiffFactor = hpDiffFactor;
    }

    public boolean isUseLevelDiffFactor() {
        return useLevelDiffFactor;
    }

    public void setUseLevelDiffFactor(boolean useLevelDiffFactor) {
        this.useLevelDiffFactor = useLevelDiffFactor;
    }

    public double getBestMatchQuality() {
        return bestMatchQuality;
    }

    public void setBestMatchQuality(double bestMatchQuality) {
        this.bestMatchQuality = bestMatchQuality;
    }

    public double getDeltaMatchQuality() {
        return deltaMatchQuality;
    }

    public void setDeltaMatchQuality(double deltaMatchQuality) {
        this.deltaMatchQuality = deltaMatchQuality;
    }

    public double getDeltaHpDiffFactor() {
        return deltaHpDiffFactor;
    }

    public void setDeltaHpDiffFactor(double deltaHpDiffFactor) {
        this.deltaHpDiffFactor = deltaHpDiffFactor;
    }

    public double getMaxHpDiffFactor() {
        return maxHpDiffFactor;
    }

    public void setMaxHpDiffFactor(double maxHpDiffFactor) {
        this.maxHpDiffFactor = maxHpDiffFactor;
    }

    public int getMaxBattlesWithSameUser() {
        return maxBattlesWithSameUser;
    }

    public void setMaxBattlesWithSameUser(int maxBattlesWithSameUser) {
        this.maxBattlesWithSameUser = maxBattlesWithSameUser;
    }

    public boolean isIgnoreMatchQualityForNoduelBattles() {
        return ignoreMatchQualityForNoduelBattles;
    }

    public boolean isIgnoreMatchQualityForDuel300Battles() {
        return ignoreMatchQualityForDuel300Battles;
    }

    public void setIgnoreMatchQualityForDuel300Battles(boolean ignoreMatchQualityForDuel300Battles) {
        this.ignoreMatchQualityForDuel300Battles = ignoreMatchQualityForDuel300Battles;
    }

    public void setIgnoreMatchQualityForNoduelBattles(boolean ignoreMatchQualityForNoduelBattles) {
        this.ignoreMatchQualityForNoduelBattles = ignoreMatchQualityForNoduelBattles;
    }

    public int getWarnNegativeRating() {
        return warnNegativeRating;
    }

    public void setWarnNegativeRating(int warnNegativeRating) {
        this.warnNegativeRating = warnNegativeRating;
    }

    public int getMinNegativeRating() {
        return minNegativeRating;
    }

    public void setMinNegativeRating(int minNegativeRating) {
        this.minNegativeRating = minNegativeRating;
    }

    public int getMercenariesBattleLobbyWinPercent() {
        return mercenariesBattleLobbyWinPercent;
    }

    public void setMercenariesBattleLobbyWinPercent(int mercenariesBattleLobbyWinPercent) {
        this.mercenariesBattleLobbyWinPercent = mercenariesBattleLobbyWinPercent;
    }

    public int getMaxSurrenderedBattlesToSameUser() {
        return maxSurrenderedBattlesToSameUser;
    }

    public int getSandboxMaxLevel() {
        return sandboxMaxLevel;
    }

    public boolean isMatchDifferentPlatform() {
        return matchDifferentPlatform;
    }

    public void setMatchDifferentPlatform(boolean matchDifferentPlatform) {
        this.matchDifferentPlatform = matchDifferentPlatform;
    }

    public int getSandboxRatingDelimiter() {
        return sandboxRatingDelimiter;
    }

    public void setSandboxRatingDelimiter(int sandboxRatingDelimiter) {
        this.sandboxRatingDelimiter = sandboxRatingDelimiter;
    }

    public int getRankThreshold() {
        return rankThreshold;
    }

    public void setRankThreshold(int rankThreshold) {
        this.rankThreshold = rankThreshold;
    }

    public boolean isCheckClan() {
        return checkClan;
    }

    public void setCheckClan(boolean checkClan) {
        this.checkClan = checkClan;
    }
}
