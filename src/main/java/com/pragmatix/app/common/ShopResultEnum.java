package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 04.05.11 10:53
 */
public enum ShopResultEnum implements TypeableEnum {
    INDEFINITE(-1),
    /**
     * Успешный статус покупки вещей
     */
    SUCCESS(0),
    /**
     * ошибка при покупки
     */
    ERROR(1),
    /**
     * ошибка в минимальных требованиях
     */
    MIN_REQUIREMENTS_ERROR(2),
    /**
     * если недостаточно средств для совершения покупки
     */
    NOT_ENOUGH_MONEY(3),
    /**
     * не пройдена дополнительная проверка (например подтверждение по СМС)
     */
    CONFIRM_FAILURE(4),
    /**
     * недостаточно боевых жетонов
     */
    NOT_ENOUGH_BATTLES(5),
    /**
     * недостаточно реагентов
     */
    NOT_ENOUGH_REAGENTS(6),
    /**
     * предмет не доступен для покупки
     */
    NOT_FOR_SALE(7),
    ;

    ShopResultEnum(int type) {
        this.type = type;
    }

    private int type;

    public boolean isSuccess() {
        return type == 0;
    }

    @Override
    public int getType() {
        return type;
    }

    public static ShopResultEnum valueOf(int type) {
        switch (type) {
            case -1:
                return INDEFINITE;
            case 0:
                return SUCCESS;
            case 1:
                return ERROR;
            case 2:
                return MIN_REQUIREMENTS_ERROR;
            case 3:
                return NOT_ENOUGH_MONEY;
            case 4:
                return CONFIRM_FAILURE;
            case 5:
                return NOT_ENOUGH_BATTLES;
            case 6:
                return NOT_ENOUGH_REAGENTS;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }
}
