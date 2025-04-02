package com.pragmatix.pvp.services.battletracking;

/**
* @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
*         Created: 22.11.12 16:57
*/
public enum PvpBattleStateEnum {
    /**
     * @see PvpBattleTrackerTimeoutTask
     */
    WaitProfiles("WP", -1, 5, true),//ждем профайлы от Main
    WaitFriendsJoin("WFJ", -1, -1, true),//ждем согласия друзей
    MatchmakingLobby("ML", -1, -1, true),//ждем подбора противника(ов)
    WaitBattleReady("WBR", -1, 20, true),//ждем BattleReady
    EnvironmentInTurn("ET", 30, 55, true),//"ходит" бот
    ReadyToDispatch("R", 5, -1),
    WaitForReplayCommand("WR", 5, 30),
    Synchronized("Syn", -1, -1, true),
    WaitForTurnTransfer("WTT", 25, 70, true),
    WaitEndBattleConfirm("WEC", 10, 30, true),
    EndBattle("End", -1, 5),
    DropBattle("Drop", -1, -1);//особое состояние, в него попадает бой когда его участники были выбраны в качестве противников для другого боя

//== тестовые таймауты ==
//    WaitProfiles("WP", -1, 5, true),//ждем профайлы от Main
//    WaitFriendsJoin("WFJ", -1, -1, true),//ждем согласия друзей
//    MatchmakingLobby("ML", -1, -1, true),//ждем подбора противника(ов)
//    WaitBattleReady("WBR", -1, 20, true),//ждем BattleReady
//    EnvironmentInTurn("ET", 5, 7, true),//"ходит" бот
//    ReadyToDispatch("R", 5, -1),
//    WaitForReplayCommand("WR", 5, 7),
//    Synchronized("Syn", -1, -1, true),
//    WaitForTurnTransfer("WTT", 5, 7, true),
//    WaitEndBattleConfirm("WEC", 5, 7, true),
//    EndBattle("End", -1, 10),
//    DropBattle("Drop", -1, -1);//особое состояние, в него попадает бой когда его участники были выбраны в качестве противников для другого боя

    public final String alias;
    public final int idleTimeoutInSeconds;
    public final int stateTimeoutInSeconds;
    public final boolean synch;

    PvpBattleStateEnum(String alias, int idleTimeoutInSeconds, int stateTimeoutInSeconds) {
        this.alias = alias;
        this.idleTimeoutInSeconds = idleTimeoutInSeconds;
        this.stateTimeoutInSeconds = stateTimeoutInSeconds;
        this.synch = false;
    }

    PvpBattleStateEnum(String alias, int idleTimeoutInSeconds, int stateTimeoutInSeconds, boolean synch) {
        this.alias = alias;
        this.idleTimeoutInSeconds = idleTimeoutInSeconds;
        this.stateTimeoutInSeconds = stateTimeoutInSeconds;
        this.synch = synch;
    }

    public int getIdleTimeoutInSeconds() {
        return idleTimeoutInSeconds;
    }

    public int getStateTimeoutInSeconds() {
        return stateTimeoutInSeconds;
    }

    public boolean isSynch() {
        return synch;
    }
}
