package com.pragmatix.app.settings;


/**
 * Настройки по зачислению опыта и денег
 * User: denis
 * Date: 06.12.2009
 * Time: 3:43:00
 */
public class SimpleBattleSettings {

    /**
     * количество денег которое начисляеться при взятии нового уровня
     */
    private int moneyForNewLevel;

    /**
     * если игрок проиграл
     */
    private int notWinnerExp;

    /**
     * количество денег при поражении
     */
    private int notWinnerMoney;

    /**
     * максимальный бонус опыта, за уровень
     * не взависимости от победы или поражения
     */
    private int maxExpBonus;

    private BattleAward awardMyLevel;

    private BattleAward awardHighLevel;

    private BattleAward awardLowLevel;

    /**
     * уменьшать ли количество боев доступных игроку
     */
    private boolean decrementBattleCount = true;

    /**
     * отслеживать ли прохождение боя
     */
    private boolean trackedBattle;

    /**
     * минимальный уроветь с которого доступна миссия
     */
    private int minLevel;

    /**
     * максимальный уроветь с которого доступна миссия
     */
    private int maxLevel;

    public boolean isLearningBattle() {
        return false;
    }

    public boolean isBossBattle() {
        return false;
    }

    public boolean isNewBossBattle() {
        return false;
    }

    public SimpleBattleSettings() {
    }

    public int getMaxExpBonus() {
        return maxExpBonus;
    }

    public void setMaxExpBonus(int maxExpBonus) {
        this.maxExpBonus = maxExpBonus;
    }

    public BattleAward getAwardMyLevel() {
        return awardMyLevel;
    }

    public void setAwardMyLevel(BattleAward awardMyLevel) {
        this.awardMyLevel = awardMyLevel;
    }

    public int getNotWinnerExp() {
        return notWinnerExp;
    }

    public void setNotWinnerExp(int notWinnerExp) {
        this.notWinnerExp = notWinnerExp;
    }

    public int getNotWinnerMoney() {
        return notWinnerMoney;
    }

    public void setNotWinnerMoney(int notWinnerMoney) {
        this.notWinnerMoney = notWinnerMoney;
    }

    public BattleAward getAwardHighLevel() {
        // если нет настроек для уровня выше - возвращаем настройки для своего уровня
        return awardHighLevel != null ? awardHighLevel : awardMyLevel;
    }

    public void setAwardHighLevel(BattleAward awardHighLevel) {
        this.awardHighLevel = awardHighLevel;
    }

    public BattleAward getAwardLowLevel() {
        // если нет настроек для уровня ниже - возвращаем настройки для своего уровня
        return awardLowLevel != null ? awardLowLevel : awardMyLevel;
    }

    public void setAwardLowLevel(BattleAward awardLowLevel) {
        this.awardLowLevel = awardLowLevel;
    }

    public boolean isDecrementBattleCount() {
        return decrementBattleCount;
    }

    public void setDecrementBattleCount(boolean decrementBattleCount) {
        this.decrementBattleCount = decrementBattleCount;
    }

    //виртуальные свойства

    public void setMyLevelWinnerExp(int winnerExp) {
        if(awardMyLevel == null) {
            awardMyLevel = new BattleAward();
        }
        awardMyLevel.setWinnerExp(winnerExp);
    }

    public void setMyLevelWinnerMoney(int winnerMoney) {
        if(awardMyLevel == null) {
            awardMyLevel = new BattleAward();
        }
        awardMyLevel.setWinnerMoney(winnerMoney);
    }

    public void setMyLevelDrawGameExp(int drawGameExp) {
        if(awardMyLevel == null) {
            awardMyLevel = new BattleAward();
        }
        awardMyLevel.setDrawGameExp(drawGameExp);
    }

    public void setMyLevelDrawGameMoney(int drawGameMoney) {
        if(awardMyLevel == null) {
            awardMyLevel = new BattleAward();
        }
        awardMyLevel.setDrawGameMoney(drawGameMoney);
    }

    public boolean isTrackedBattle() {
        return trackedBattle;
    }

    public void setTrackedBattle(boolean trackedBattle) {
        this.trackedBattle = trackedBattle;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }
}

