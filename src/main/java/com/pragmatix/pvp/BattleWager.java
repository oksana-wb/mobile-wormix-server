package com.pragmatix.pvp;

import com.pragmatix.serialization.annotations.EnumKey;

import static com.pragmatix.pvp.WagerValue.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 16.11.12 16:54
 */
public enum BattleWager {

    NO_WAGER(0, WV_0, 0, PvpBattleType.FRIEND_PvP),//FRIEND_DUEL
    FRIEND_20_DUEL(15, WV_0, 0, PvpBattleType.FRIEND_PvP),
    FRIEND_ROPE_RACE(17, WV_0, 0, PvpBattleType.FRIEND_PvP, /*questId*/2),

    WAGER_15_DUEL(1, WV_15, 0, PvpBattleType.WAGER_PvP_DUEL),
    WAGER_20_DUEL(6, WV_20, 0, PvpBattleType.WAGER_PvP_DUEL),
    WAGER_50_DUEL(2, WV_50, 0.1, PvpBattleType.WAGER_PvP_DUEL),
    WAGER_300_DUEL(3, WV_300, 0.5, PvpBattleType.WAGER_PvP_DUEL),

    WAGER_50_2x2_FRIENDS(4, WV_50, 1, PvpBattleType.WAGER_PvP_2x2),
    WAGER_50_2x2(7, WV_50, 1, PvpBattleType.WAGER_PvP_2x2),

    WAGER_50_3_FOR_ALL(5, WV_50, 0.5, PvpBattleType.WAGER_PvP_3_FOR_ALL),

    GLADIATOR_DUEL(8, WV_0, 0, PvpBattleType.WAGER_PvP_DUEL),
    ROPE_RACE_QUEST(10, WV_0, 0, PvpBattleType.PvE_PARTNER, /*questId*/2),
    FRIEND_ROPE_RACE_QUEST(16, WV_0, 0, PvpBattleType.PvE_FRIEND, /*questId*/2),
    MERCENARIES_DUEL(11, WV_0, 0, PvpBattleType.WAGER_PvP_DUEL),

    PvE_FRIEND(12, WV_0, 0, PvpBattleType.PvE_FRIEND),
    PvE_PARTNER(14, WV_0, 0, PvpBattleType.PvE_PARTNER);

    private final int id;
    private final WagerValue value;
    private int minLevel = 1;
    private float ratingBonus;
    public final PvpBattleType battleType;
    public final short questId;

    BattleWager(int id, WagerValue value, double ratingBonus, PvpBattleType battleType) {
        this.id = id;
        this.value = value;
        this.ratingBonus = (float) ratingBonus;
        this.battleType = battleType;
        this.questId = 0;
    }

    BattleWager(int id, WagerValue value, double ratingBonus, PvpBattleType battleType, int questId) {
        this.id = id;
        this.value = value;
        this.ratingBonus = (float) ratingBonus;
        this.battleType = battleType;
        this.questId = (short)questId;
    }

    @EnumKey
    public int getId() {
        return id;
    }

    public float getRatingBonus() {
        return ratingBonus;
    }

    public int getValue() {
        return value.value;
    }

    public WagerValue getWagerValue() {
        return value;
    }

    public int getMinLevel() {
        return minLevel;
    }

}
