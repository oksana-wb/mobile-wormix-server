package com.pragmatix.arena.mercenaries;

import com.pragmatix.gameapp.common.TypeableEnum;

public enum MercenariesErrorEnum implements TypeableEnum {

    OK(0),
    // ошибка на сервере
    ERROR(1),
    // в команде должеы быть выбраны все наёмники и они должны быть уникальны
    INVALID_TEAM(2),
    // арена и так открыта
    ALREADY_OPEN(3),
    // не достаточно боёв
    NO_ENOUGH_BATTLES(4),
    // арена закрыта для игрока
    HAS_NO_TICKET(5),
    // попытки на сегодня кончились
    HAS_NO_ATTEMPTS(6),
    ;

    public final int type;

    MercenariesErrorEnum(int type) {
        this.type = type;
    }

    @Override
    public int getType() {
        return type;
    }

    public static MercenariesErrorEnum valueOf(int type){
        for(MercenariesErrorEnum errorEnum : values()) {
            if(errorEnum.type == type)
                return errorEnum;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }

}

