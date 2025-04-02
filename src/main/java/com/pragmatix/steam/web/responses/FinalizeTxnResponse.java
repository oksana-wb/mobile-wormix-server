package com.pragmatix.steam.web.responses;


import com.pragmatix.steam.web.request.FinalizeTxnRequest;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 09.03.2017 9:27
 *         <p>
 * Ответ на запрос завершения внутриигровой покупки Steam
 * @see FinalizeTxnRequest
 */
public class FinalizeTxnResponse extends SteamWebResponse<FinalizeTxnRequest> {

    /**
     * Unique 64-bit ID for order
     * @see FinalizeTxnRequest#orderid
     */
    public long orderid;

    /**
     * Unique 64-bit Steam transaction ID
     * @see InitTxnResponse#orderid
     */
    public String transid;

    // for json deserialization
    public FinalizeTxnResponse() { }

    @Override
    public String toString() {
        return "FinalizeTxnResponse{" +
                super.toString() +
                ", orderid=" + orderid +
                ", transid=" + transid +
                "}";
    }
}
