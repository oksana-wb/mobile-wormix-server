package com.pragmatix.app.messages.server;

import com.pragmatix.app.model.AnyMoneyAddition;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 21.04.2016 10:01
 *         <p>
 * Возвращает дивиденды на сегодня
 *
 * @see com.pragmatix.app.messages.client.GetDividend
 * @see com.pragmatix.app.controllers.PaymentController#onGetDividend(com.pragmatix.app.messages.client.GetDividend, com.pragmatix.app.model.UserProfile)
 */
@Command(10133)
public class GetDividendResult implements AnyMoneyAddition, SecuredResponse {

    public SimpleResultEnum result;

    /**
     * Полученные фузы
     */
    public int money;

    /**
     * Полученные рубины
     */
    public int realMoney;

    public String sessionKey;

    public GetDividendResult() {
    }

    public GetDividendResult(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public GetDividendResult(int money, int realMoney, String sessionKey) {
        this.money = money;
        this.realMoney = realMoney;
        this.sessionKey = sessionKey;
    }

    @Override
    public String toString() {
        return "GetDividendResult{" +
                "result=" + result +
                ", money=" + money +
                ", realMoney=" + realMoney +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }

    @Override
    public int getMoney() {
        return money;
    }

    @Override
    public void setMoney(int money) {
        this.money = money;
    }

    @Override
    public int getRealMoney() {
        return realMoney;
    }

    @Override
    public void setRealMoney(int realMoney) {
        this.realMoney = realMoney;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }
}
