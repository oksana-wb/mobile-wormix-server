package com.pragmatix.achieve.common;

import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.07.11 11:02
 */
public enum GrantAwardResultEnum implements TypeableEnum {

    INDEFINITE(-1),
    OK(0),
    PROFILE_NOT_FOUND(1),
    ITEM_NOT_FOUND(2),
    ERROR(3),
    MIN_REQUIREMENTS_ERROR(4);

    int type;

    GrantAwardResultEnum(int type) {
        this.type = type;
    }

    @Override
    public int getType() {
        return type;
    }

    public static GrantAwardResultEnum valueOf(int type) {
        for(GrantAwardResultEnum resultEnum : values()) {
            if(resultEnum.getType() == type) {
                return resultEnum;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }
}
