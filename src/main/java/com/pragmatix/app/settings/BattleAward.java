package com.pragmatix.app.settings;

/**
 * @author denis
 *         Date: 09.01.2010
 *         Time: 0:23:07
 */
public class BattleAward {
    
    /**
     * если игрок выиграл
     */
    private int winnerExp;

    /**
     * если ничья
     */
    private int drawGameExp;

    /**
     * количество денег при победе
     */
    private int winnerMoney;

    /**
     * количество денег если ничья
     */
    private int drawGameMoney;

    public BattleAward() {
    }

    public int getWinnerExp() {
        return winnerExp;
    }

    public void setWinnerExp(int winnerExp) {
        this.winnerExp = winnerExp;
    }

    public int getDrawGameExp() {
        return drawGameExp;
    }

    public void setDrawGameExp(int drawGameExp) {
        this.drawGameExp = drawGameExp;
    }

    public int getWinnerMoney() {
        return winnerMoney;
    }

    public void setWinnerMoney(int winnerMoney) {
        this.winnerMoney = winnerMoney;
    }

    public int getDrawGameMoney() {
        return drawGameMoney;
    }

    public void setDrawGameMoney(int drawGameMoney) {
        this.drawGameMoney = drawGameMoney;
    }
}
