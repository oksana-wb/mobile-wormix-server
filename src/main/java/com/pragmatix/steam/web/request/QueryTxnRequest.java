package com.pragmatix.steam.web.request;


import com.pragmatix.steam.SteamAPIInterface;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 * Created: 09.03.2017 12:08
 * <p>
 * Запрос статуса внутриигровой покупки Steam, созданной {@link InitTxnRequest}
 * @see com.pragmatix.steam.web.responses.QueryTxnResponse
 */
public class QueryTxnRequest extends SteamWebRequest {

    /**
     * Unique 64-bit ID for order (NB: must specify an orderid OR transid, т.е. ОДНО ИЗ ДВУХ)
     *
     * @see InitTxnRequest#orderid
     */
    public Long orderid;

    /**
     * Unique 64-bit Steam transaction ID (NB: must specify an orderid OR transid, т.е. ОДНО ИЗ ДВУХ)
     *
     * @see com.pragmatix.steam.web.responses.InitTxnResponse#transid
     */
    public Long transid;

    public QueryTxnRequest() {
    }

    public QueryTxnRequest(long orderid) {
        this.orderid = orderid;
    }

    public QueryTxnRequest(Long orderid, Long transid) {
        if(orderid == null && transid == null) {
            throw new IllegalArgumentException("QueryTxnRequest must contain either orderid OR transid");
        }
        this.orderid = orderid;
        this.transid = transid;
    }

    /**
     * @return метод Steam Web API, которому соответствует данный класс
     */
    @Override
    public SteamAPIInterface.Method getMethod() {
        return SteamAPIInterface.Method.QueryTxn;
    }

    @Override
    public Map<String, String> toMap(int appId) {
        Map<String, String> res = new LinkedHashMap<>();
        if(orderid != null) {
            res.put("orderid", orderid.toString());
        }
        if(transid != null) {
            res.put("transid", transid.toString());
        }
        res.put("appid", String.valueOf(appId));
        return res;
    }


    @Override
    public String toString() {
        return "QueryTxnRequest{" +
                (orderid != null ? "orderid=" + orderid : "") +
                (transid != null ? ", transid=" + transid : "") +
                '}';
    }
}
