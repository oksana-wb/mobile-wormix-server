package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

import javax.validation.constraints.Null;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 28.01.2016 11:52
 *         <p>
 *         Результат битвы (миссии)
 */
public enum BattleResultEnum implements TypeableEnum {
    /**
     * если игрок выиграл
     */
    WINNER(1),
    /**
     * если игрок отсоединился и не переподключился
     */
    NOT_WINNER_INCORRECT_RECONNECT(-6),
    /**
     * если игрок отсоединился и не переподключился
     */
    NOT_WINNER_RECONNECT_TIMEOUT(-5),
    /**
     * если игрок сдался
     */
    NOT_WINNER_SURRENDER(-4),
    /**
     * если команда не прошла проверку
     */
    NOT_WINNER_CHEAT(-3),
    /**
     * если игрок отсоединился
     */
    NOT_WINNER_DISCONNECT(-2),
    /**
     * если игрок проиграл
     */
    NOT_WINNER(-1),
    /**
     * если ничья
     */
    DRAW_GAME(0),
    ;

    private int type;

    BattleResultEnum(int type) {
        this.type = type;
    }

    @Override
    public int getType() {
        return type;
    }

    @Null
    public static BattleResultEnum valueOf(int type) {
        switch (type) {
            case 1:
                return WINNER;
            case 0:
                return DRAW_GAME;
            case -1:
                return NOT_WINNER;
            case -2:
                return NOT_WINNER_DISCONNECT;
            case -3:
                return NOT_WINNER_CHEAT;
            case -4:
                return NOT_WINNER_SURRENDER;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }
}
