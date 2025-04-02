package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.05.12 8:44
 */

public enum PvpBattleResult implements TypeableEnum {

    WINNER(0, 2),
    NOT_WINNER(1, 0),
    DRAW_GAME(2, 1),
    DRAW_DESYNC(3, 2),// бой в этом состоянии не может иметь победителя, поэтому вес его равен победе
    DRAW_SHUTDOWN(4, 1);

    private int type;
    private int weight;

    PvpBattleResult(int type, int weight) {
        this.type = type;
        this.weight = weight;
    }

    @Override
    public int getType() {
        return type;
    }

    public byte byteType() {
        return (byte) type;
    }

    public int getWeight() {
        return weight;
    }

    public static PvpBattleResult valueOf(int type) {
        switch (type) {
            case 0:
                return WINNER;
            case 1:
                return NOT_WINNER;
            case 2:
                return DRAW_GAME;
            case 3:
                return DRAW_DESYNC;
            case 4:
                return DRAW_SHUTDOWN;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }

}