package com.pragmatix.steam.web.responses;


import com.pragmatix.steam.web.request.InitTxnRequest;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 09.03.2017 9:27
 *         <p>
 * Ответ на запрос на создание внутриигровой покупки Steam
 * @see InitTxnRequest
 */
public class InitTxnResponse extends SteamWebResponse<InitTxnRequest> {

    /**
     * Unique 64-bit ID for order
     * @see InitTxnRequest#orderid
     */
    public long orderid;

    /**
     * Unique transaction ID
     */
    public String transid;

    public transient String socialParams;

    // for json deserialization
    public InitTxnResponse() { }

    @Override
    public String toString() {
        return "InitTxnResponse{" +
                super.toString() +
                ", orderid=" + orderid +
                ", transid=" + transid +
                "}";
    }
}
