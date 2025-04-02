package com.pragmatix.steam.messages;

import com.pragmatix.app.common.PaymentType;
import com.pragmatix.app.messages.server.IncomingPayment;
import com.pragmatix.app.messages.server.NeedMoneyResult;
import com.pragmatix.serialization.annotations.Command;

/**
 * Author: Vladimir
 * Date: 13.03.2017 11:24
 */
@Command(6004)
public class CommitPurchaseTxResponse extends IncomingPayment {

    public NeedMoneyResult.ResultEnum result = NeedMoneyResult.ResultEnum.ERROR;

    public int purchaseOrderId;

    public String purchaseTxId;

    public CommitPurchaseTxResponse() {
    }

    public CommitPurchaseTxResponse(CommitPurchaseTxRequest request) {
        this.purchaseOrderId = request.purchaseOrderId;
        this.purchaseTxId = request.purchaseTxId;
        this.sessionKey = request.sessionKey;
    }

    @Override
    public String toString() {
        return "CommitPurchaseTxResponse{" +
                "result=" + result +
                ", purchaseOrderId=" + purchaseOrderId +
                ", purchaseTxId='" + purchaseTxId + '\'' +
                ", item=" + item +
                ", paymentType=" + paymentType +
                ", count=" + count +
                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }

}
