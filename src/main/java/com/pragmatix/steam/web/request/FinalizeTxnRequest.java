package com.pragmatix.steam.web.request;


import com.pragmatix.steam.SteamAPIInterface;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 09.03.2017 12:08
 *         <p>
 * Запрос завершения внутриигровой покупки Steam, созданной {@link InitTxnRequest}
 *
 * @see <a href="https://partner.steamgames.com/documentation/MicroTxn#FinalizeTxn">MicroTxn#FinalizeTxn</a>
 * @see com.pragmatix.df.server.social.steam.responses.FinalizeTxnResponse
 */
public class FinalizeTxnRequest extends SteamWebRequest {

    /**
     * Unique 64-bit ID for order
     *
     * @see InitTxnRequest#orderid
     */
    public long orderid;

    public FinalizeTxnRequest() {
    }

    public FinalizeTxnRequest(long orderid) {
        this.orderid = orderid;
    }

    /**
     * @return метод Steam Web API, которому соответствует данный класс
     */
    @Override
    public SteamAPIInterface.Method getMethod() {
        return SteamAPIInterface.Method.FinalizeTxn;
    }

    @Override
    public Map<String, String> toMap(int appId) {
        Map<String, String> res = new LinkedHashMap<>();
        res.put("orderid", String.valueOf(orderid));
        res.put("appid", String.valueOf(appId));
        return res;
    }

    @Override
    public String toString() {
        return "FinalizeTxnRequest{" +
                "orderid=" + orderid +
                "}";
    }
}
