package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.structures.DepositStructure;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;

import java.util.Date;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 20.04.2016 16:58
 *         <p>
 * Отсылается в случае успешного открытия вклада
 */
@Command(10132)
public class OpenDepositResult implements SecuredResponse {

    public DepositStructure deposit;

    /**
     * первая часть, которую игрок уже получил
     */
    public int firstPart;

    public String sessionKey;

    public OpenDepositResult() {
    }

    public OpenDepositResult(DepositStructure deposit, int firstPart, String sessionKey) {
        this.deposit = deposit;
        this.firstPart = firstPart;
        this.sessionKey = sessionKey;
    }

    public OpenDepositResult(MoneyType moneyType, int[] dividendsByDays, Date startDate, int progress, int firstPart, String sessionKey) {
        this(new DepositStructure(moneyType.getType(), dividendsByDays, startDate, progress), firstPart, sessionKey);
    }

    @Override
    public String toString() {
        return "OpenDepositResult{" +
                "deposit=" + deposit +
                ", firstPart=" + firstPart +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }
}
