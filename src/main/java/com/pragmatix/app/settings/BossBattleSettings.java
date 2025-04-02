package com.pragmatix.app.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.02.12 12:06
 */
public class BossBattleSettings extends SimpleBattleSettings {

    private BossBattleWinAward firstWinBattleAward;

    private BossBattleWinAward nextWinBattleAward;

    @JsonIgnore
    private byte missionTeamSize;

    @JsonIgnore
    private boolean newBoss;

    @Override
    public boolean isBossBattle() {
        return true;
    }

    @Override
    public boolean isNewBossBattle() {
        return newBoss;
    }

    public void setNewBoss(boolean newBoss) {
        this.newBoss = newBoss;
    }

    public BossBattleWinAward getFirstWinBattleAward() {
        return firstWinBattleAward;
    }

    public void setFirstWinBattleAward(BossBattleWinAward firstWinBattleAward) {
        this.firstWinBattleAward = firstWinBattleAward;
    }

    public BossBattleWinAward getNextWinBattleAward() {
        return nextWinBattleAward;
    }

    public void setNextWinBattleAward(BossBattleWinAward nextWinBattleAward) {
        this.nextWinBattleAward = nextWinBattleAward;
    }

    public byte getMissionTeamSize() {
        return missionTeamSize;
    }

    public void setMissionTeamSize(byte missionTeamSize) {
        this.missionTeamSize = missionTeamSize;
    }
}
