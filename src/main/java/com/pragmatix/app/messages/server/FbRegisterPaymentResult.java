package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.client.facebook.FbRegisterMobilePayment;
import com.pragmatix.app.messages.client.facebook.FbRegisterPayment;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

/**
 * Facebook: результат регистрация платежа
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.09.13 16:41
 */
@Command(1099)
public class FbRegisterPaymentResult implements SecuredResponse {

    /**
     * идентификатор платежа оригинального запроса
     */
    public String itemId;

    public SimpleResultEnum result;

    /**
     * id платежной транзакции. Генерируется сервером
     */
    public int paymentId;

    /**
     * ключ текущей сессии
     */
    public String sessionKey;

    public FbRegisterPaymentResult() {
    }

    public FbRegisterPaymentResult(SimpleResultEnum result, FbRegisterPayment request, int paymentId) {
        this.paymentId = paymentId;
        this.sessionKey = request.sessionKey;
        this.itemId = request.itemId;
        this.result = result;
    }

    public FbRegisterPaymentResult(SimpleResultEnum result, FbRegisterMobilePayment request, int paymentId) {
        this.paymentId = paymentId;
        this.sessionKey = request.sessionKey;
        this.itemId = request.pricepointId;
        this.result = result;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "FbRegisterPaymentResult{" +
                "itemId='" + itemId + '\'' +
                ", result=" + result +
                ", paymentId=" + paymentId +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
