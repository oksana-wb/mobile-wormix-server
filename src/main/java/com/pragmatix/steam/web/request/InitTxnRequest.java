package com.pragmatix.steam.web.request;


import com.pragmatix.steam.SteamAPIInterface;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 09.03.2017 9:26
 *         <p>
 * Запрос на создание внутриигровой покупки Steam
 *
 * This command allows you to create a shopping cart of one or more items for a user.
 * The cost and descriptions of these items will be displayed to the user for their approval.
 *
 * @see <a href="https://partner.steamgames.com/documentation/MicroTxn#InitTxn">MicroTxn#InitTxn</a>
 * @see com.pragmatix.df.server.social.steam.responses.InitTxnResponse
 */
public class InitTxnRequest extends SteamWebRequest {

    /**
     * Unique 64-bit ID for order
     */
    public long orderid;

    /**
     * SteamID of client
     */
    public String steamid;

    /**
     * Language code of item descriptions (for example, "EN")
     */
    public String language;

    /**
     * ISO 4217 currency code of prices (for example, "USD")
     *
     * @see com.pragmatix.df.server.social.steam.responses.GetUserInfoResponse#currency
     */
    public String currency;

    /**
     * One or more items of shopping cart
     */
    public List<OrderItemStructure> lineitems;

    @Override
    public SteamAPIInterface.Method getMethod() {
        return SteamAPIInterface.Method.InitTxn;
    }

    @Override
    public Map<String, String> toMap(int appId) {
        Map<String, String> res = new LinkedHashMap<>();
        res.put("orderid", String.valueOf(orderid));
        res.put("steamid", steamid);
        res.put("appid", String.valueOf(appId));
        res.put("itemcount", String.valueOf(lineitems.size()));
        res.put("language", language);
        res.put("currency", currency);
        itemsToMap(lineitems, OrderItemStructure::toMap, res);
        return res;
    }

    @Override
    public String toString() {
        return "InitTxnRequest{" +
                "orderid=" + orderid +
                ", steamid='" + steamid + '\'' +
                ", language='" + language + '\'' +
                ", currency='" + currency + '\'' +
                ", lineitems=" + OrderItemStructure.mkString(lineitems, currency) +
                '}';
    }
}
