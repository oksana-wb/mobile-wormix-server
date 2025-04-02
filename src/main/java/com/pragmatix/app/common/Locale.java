package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 04.05.11 12:22
 */
public enum Locale implements TypeableEnum {

    NONE(0),
    RU(1),
    EN(2),
    ZH(3);

    private int type;

    Locale(int type) {
        this.type = type;
    }

    @Override
    public int getType() {
        return type;
    }

    public static Locale valueOf(int type) {
        switch (type) {
            case 1:
                return RU;
            case 2:
                return EN;
            case 3:
                return ZH;
            default:
                return NONE;
        }
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }

    }
