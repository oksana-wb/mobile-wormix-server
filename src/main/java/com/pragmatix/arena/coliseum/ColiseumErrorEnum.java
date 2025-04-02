package com.pragmatix.arena.coliseum;

import com.pragmatix.gameapp.common.TypeableEnum;

public enum ColiseumErrorEnum implements TypeableEnum {
    // ошибка на сервере
    ERROR(1),
    // гладиаторска арена не доступна (по времени)
    IS_CLOSED(2),
    // команда уже заполнена
    TEAM_IS_FULL(3),
    // индекс должен быть в диапазоне 0-2
    INVALID_TEAM_MEMBER_INDEX(4),
    //  гладиаторска арена и так открыта
    ALREDY_OPEN(5),
    //  не достаточно рубинов
    NO_ENOUGH_MONEY(6),
    // гладиаторска арена закрыта для игрока
    HAS_NO_TICKET(7),
    ;

    public final int type;

    ColiseumErrorEnum(int type) {
        this.type = type;
    }

    @Override
    public int getType() {
        return type;
    }

}

