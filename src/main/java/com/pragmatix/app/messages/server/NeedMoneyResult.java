package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.serialization.annotations.Command;

/**
 * команда посылаеться с сервера и говорит
 * о статусе перевода денег на счет приложения
 * <p/>
 * User: denis
 * Date: 13.12.2009
 * Time: 3:57:00
 */
@Command(10008)
public class NeedMoneyResult {

    public enum ResultEnum implements TypeableEnum {
        /**
         * Успешный статус
         */
        SUCCESS(0),
        /**
         * ошибка
         */
        ERROR(1),
        /**
         * если недостаточно средств
         */
        NOT_ENOUGH_MONEY(3),
        /**
         * транзакция уже проведена
         */
        ALREADY_PURCHASED(4),;

        public final int type;

        ResultEnum(int type) {
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

    /**
     * Количество реально зачисленных денег
     */
    public int value;

    public ResultEnum result;

    public NeedMoneyResult(int value, ResultEnum result) {
        this.value = value;
        this.result = result;
    }

    public NeedMoneyResult(ResultEnum result) {
        this.result = result;
        this.value = 0;
    }

    public NeedMoneyResult() {
    }

    @Override
    public String toString() {
        return "NeedMoneyResult" +
                "{value=" + value +
                ", result=" + result +
                '}';
    }

}
