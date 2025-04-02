package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.09.13 10:24
 */
public enum RatingType implements TypeableEnum {

    Global(0),
    Daily(1),
    Yesterday(2),
    Seasonal(3),
    ;

    private int type;

    RatingType(int type) {
        this.type = type;
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
