package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

import javax.validation.constraints.Null;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 28.01.2016 12:43
 *         <p>
 *         Тип миссии с ботом: какого он уровня
 */
public enum WhichLevelEnum implements TypeableEnum {
    /**
     * игра со своим уровнем
     */
    MY_LEVEL(0),

    /**
     * игра со следующим уровнем   HighLevel
     */
    HIGH_LEVEL(1),

    /**
     * игра с предыдущим уровнем
     */
    LOW_LEVEL(2),
    ;
    private int type;

    WhichLevelEnum(int type) {
        this.type = type;
    }

    @Null
    public static WhichLevelEnum valueOf(int type) {
        switch (type) {
            case 0:
                return MY_LEVEL;
            case 1:
                return HIGH_LEVEL;
            case 2:
                return LOW_LEVEL;
        }
        return null;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }
}
