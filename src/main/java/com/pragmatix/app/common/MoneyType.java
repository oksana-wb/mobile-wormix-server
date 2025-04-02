package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 04.05.11 10:08
 */
public enum MoneyType implements TypeableEnum {
    /**
     * реалы
     */
    REAL_MONEY(0),
    /**
     * игровые деньги
     */
    MONEY(1),
    /**
     * боевые жетоны
     */
    BATTLES(2),
    /**
     * реагенты
     */
    REAGENTS(3),;

    private int type;

    MoneyType(int type) {
        this.type = type;
    }

    @Override
    public int getType() {
        return type;
    }

    public static MoneyType valueOf(int type) {
        switch (type) {
            case 1:
                return MONEY;
            case 2:
                return BATTLES;
            case 3:
                return REAGENTS;
            default:
                return REAL_MONEY;
        }
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }

}
