package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.PaymentType;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

import java.util.List;

/**
 * Команда которая передает информацию клиенту о входящем платеже
 */
@Command(10031)
public class IncomingPayment implements SecuredResponse {
    /**
     * тип платежа
     */
    public PaymentType paymentType = PaymentType.UNDEFINED;
    /**
     * код платежа
     */
    public String item;
    /**
     * количество
     */
    public int count;

    public List<GenericAwardStructure> awards;
    /**
     * дополнительный параметр
     */
    public String note;

    public String sessionKey;

    public IncomingPayment() {
    }

    public IncomingPayment(PaymentType paymentType, String item, int count, List<GenericAwardStructure> awards, String note, String sessionKey) {
        this.paymentType = paymentType;
        this.item = item;
        this.count = count;
        this.awards = awards;
        this.note = note;
        this.sessionKey = sessionKey;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "IncomingPayment{" +
                "paymentType=" + paymentType +
                ", item=" + item +
                ", count=" + count +
                ", awards=" + awards +
                ", note=" + note +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
