package com.pragmatix.clanserver.domain;

import com.pragmatix.serialization.annotations.EnumKey;

/**
 * Author: Vladimir
 * Date: 23.05.13 15:38
 */
public enum ReviewState {

    NONE(0), APPROVED(1), LOCKED(-1);

    public final int code;

    ReviewState(int code) {
        this.code = code;
    }

    public static ReviewState getByCode(int code) {
        for (ReviewState state: values()) {
            if (state.code == code) {
                return state;
            }
        }
        return NONE;
    }

    @EnumKey
    public int getCode() {
        return code;
    }
}
