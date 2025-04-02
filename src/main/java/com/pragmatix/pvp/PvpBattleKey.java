package com.pragmatix.pvp;

public enum PvpBattleKey {
    // все реализованные бои со своими ставками
    FRIEND_PvP(PvpBattleType.FRIEND_PvP, BattleWager.NO_WAGER),
    FRIEND_DUEL_20(PvpBattleType.FRIEND_PvP, BattleWager.FRIEND_20_DUEL),
    FRIEND_ROPE_RACE(PvpBattleType.FRIEND_PvP, BattleWager.FRIEND_ROPE_RACE),

    PvE_FRIEND(PvpBattleType.PvE_FRIEND, BattleWager.PvE_FRIEND),
    PvE_PARTNER(PvpBattleType.PvE_PARTNER, BattleWager.PvE_PARTNER),

    WAGER_PvP_DUEL_15(PvpBattleType.WAGER_PvP_DUEL, BattleWager.WAGER_15_DUEL),
    WAGER_PvP_DUEL_20(PvpBattleType.WAGER_PvP_DUEL, BattleWager.WAGER_20_DUEL),
    WAGER_PvP_DUEL_50(PvpBattleType.WAGER_PvP_DUEL, BattleWager.WAGER_50_DUEL),
    WAGER_PvP_DUEL_300(PvpBattleType.WAGER_PvP_DUEL, BattleWager.WAGER_300_DUEL),
    WAGER_PvP_3_FOR_ALL(PvpBattleType.WAGER_PvP_3_FOR_ALL, BattleWager.WAGER_50_3_FOR_ALL),
    WAGER_PvP_2x2(PvpBattleType.WAGER_PvP_2x2, BattleWager.WAGER_50_2x2),

    GLADIATOR_DUEL(PvpBattleType.WAGER_PvP_DUEL, BattleWager.GLADIATOR_DUEL),
    ROPE_RACE_QUEST(PvpBattleType.PvE_PARTNER, BattleWager.ROPE_RACE_QUEST),
    FRIEND_ROPE_RACE_QUEST(PvpBattleType.PvE_FRIEND, BattleWager.FRIEND_ROPE_RACE_QUEST),
    MERCENARIES_DUEL(PvpBattleType.WAGER_PvP_DUEL, BattleWager.MERCENARIES_DUEL),;
    public final PvpBattleType battleType;

    public final BattleWager battleWager;

    PvpBattleKey(PvpBattleType battleType, BattleWager battleWager) {
        this.battleType = battleType;
        this.battleWager = battleWager;
    }

    public static PvpBattleKey valueOf(PvpBattleType battleType, BattleWager battleWager) {
        for(PvpBattleKey pvpBattleKey : PvpBattleKey.values()) {
            if(pvpBattleKey.battleType == battleType && pvpBattleKey.battleWager == battleWager) {
                return pvpBattleKey;
            }
        }
        if(battleType == PvpBattleType.WAGER_PvP_2x2 && battleWager == BattleWager.WAGER_50_2x2_FRIENDS)
            return WAGER_PvP_2x2;

        throw new IllegalArgumentException("бой (тип:" + battleType + "/ставка:" + battleWager + ") не реализован!");
    }
}
