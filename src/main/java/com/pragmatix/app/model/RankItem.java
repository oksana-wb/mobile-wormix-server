package com.pragmatix.app.model;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 19.05.2016 12:33
 */
public class RankItem {

    public final int rank;
    public final int pointsToNext;
    public final int maxVictoryPoints;
    public final float victoryBonus;
    public final float defeatPenalty;

    public int needPoints;

    public RankItem(int rank, int pointsToNext, int maxVictoryPoints, float victoryBonus, float defeatPenalty) {
        this.rank = rank;
        this.pointsToNext = pointsToNext;
        this.maxVictoryPoints = maxVictoryPoints;
        this.victoryBonus = victoryBonus;
        this.defeatPenalty = defeatPenalty;
    }

    public int getVictoryPoints(int baseVictoryPoints) {
        return Math.min(this.maxVictoryPoints, baseVictoryPoints);
    }

    public int getDefeatPoints() {
        return -Math.round(this.maxVictoryPoints * this.defeatPenalty);
    }

    @Override
    public String toString() {
        return "RankItem{" +
                "rank=" + rank +
                ", needPoints=" + needPoints +
                ", pointsToNext=" + pointsToNext +
                ", maxVictoryPoints=" + maxVictoryPoints +
                ", victoryBonus=" + victoryBonus +
                ", defeatPenalty=" + defeatPenalty +
                '}';
    }
}
