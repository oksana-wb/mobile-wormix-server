package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.01.12 17:05
 */
public enum PaymentStatusEnum implements TypeableEnum {
    // платеж зарегистрирован, но не обработан
    TOUCH(-1),
    // платеж обработан успешно
    SUCCESS(0, true),
    // платеж не подтвержден
    FAILURE(1),
    // во время обработки платежа произошли ошибки
    ERROR(2),
    // результат платежа не известен (read timeout например)
    INDEFINITE(3),
    // платеж проведен вручную
    MANUAL(4),
    ;

    private int type;

    private boolean completed = false;

    private PaymentStatusEnum(int type) {
        this.type = type;
    }

    private PaymentStatusEnum(int type, boolean completed) {
        this.type = type;
        this.completed = completed;
    }

    @Override
    public int getType() {
        return type;
    }

    public boolean isCompleted() {
        return completed;
    }

}

