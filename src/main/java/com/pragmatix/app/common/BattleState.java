package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * Тип боя
 * <p/>
 * Created by IntelliJ IDEA.
 * User: denver
 * Date: 12.03.2010
 * Time: 2:20:04
 */
public enum BattleState implements TypeableEnum {

    // type по порядку начиная с 0
    INDEFINITE(0),
    NOT_IN_BATTLE(1),
    WAIT_START_BATTLE(2),
    SIMPLE(3),
    IN_BATTLE_PVP(4),;

    private int type;

    BattleState(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static BattleState valueOf(int type) {
        try {
            return BattleState.values()[type];
        } catch (Exception e) {
        }
        return INDEFINITE;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }
}
