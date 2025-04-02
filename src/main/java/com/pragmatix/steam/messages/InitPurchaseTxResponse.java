package com.pragmatix.steam.messages;

import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 13.03.2017 11:24
 */
@Command(6002)
public class InitPurchaseTxResponse {

    public int purchaseOrderId;

    public String purchaseTxId;

    public String countryCode;

    public String currencyCode;

    public String socialParams;

    /** код ошибки (либо 0 в случае успеха) */
    public int errorCode;

    /** сообщение об ошибке (либо null в случае успеха) */
    public String errorMessage;

    @Override
    public String toString() {
        return "InitPurchaseTxResponse{" +
                "purchaseOrderId=" + purchaseOrderId +
                ", purchaseTxId='" + purchaseTxId + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", currencyCode='" + currencyCode + '\'' +
                ", socialParams='" + socialParams + '\'' +
                ", errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }

}
