package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Результат обыска домика игрока
 * User: denis
 * Date: 01.08.2010
 * Time: 20:41:09
 */
@Command(10021)
public class SearchTheHouseResult implements SecuredResponse {

    public enum ResultEnum implements TypeableEnum {
        /**
         * в домике пусто
         */
        EMPTY(0),

        /**
         * в домике нашли рубины
         */
        REAL_MONEY(1),

        /**
         * в домике нашли фузы
         */
        MONEY(2),

        /**
         * ошибка на сервере, например нету такого друга в бд
         */
        ERROR(3),

        /**
         * игрок не заходил последние 60-ть дней
         */
        ABANDONED(4),

        /**
         * исчерпан лимит обысков на сегодня
         */
        KEY_LIMIT_EXCEED(5),

        /**
         * исчерпан лимит рубинов
         */
        RUBY_LIMIT_EXEED(6),

        /**
         * в домике нашли реагент
         */
        REAGENT(7),;

        private int type;

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
     * результат обыска
     */
    @Resize(TypeSize.BYTE)
    public ResultEnum result;

    /**
     * количество найденых денег или id найденного реагента
     */
    public int value;

    public byte availableSearchKeys;

    @Resize(TypeSize.UINT32)
    public long friendId;

    public SearchTheHouseResult() {
    }

    public SearchTheHouseResult(ResultEnum result, int value, byte availableSearchKeys) {
        this.result = result;
        this.value = value;
        this.availableSearchKeys = availableSearchKeys;
    }

    public SearchTheHouseResult(ResultEnum result, int value, byte availableSearchKeys, long friendId) {
        this.result = result;
        this.value = value;
        this.availableSearchKeys = availableSearchKeys;
        this.friendId = friendId;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "SearchTheHouseResult{" +
                "result=" + result +
                ", value=" + value +
                ", friendId=" + friendId +
                ", availableSearchKeys=" + availableSearchKeys +
                '}';
    }
}
