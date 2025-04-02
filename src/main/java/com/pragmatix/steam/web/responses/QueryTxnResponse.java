package com.pragmatix.steam.web.responses;


import com.pragmatix.steam.web.request.OrderItemStructure;
import com.pragmatix.steam.web.request.QueryTxnRequest;

import java.util.List;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 09.03.2017 13:00
 *         <p>
 * Ответ на запрос статуса внутриигровой покупки Steam
 * @see QueryTxnRequest
 */
public class QueryTxnResponse extends SteamWebResponse<QueryTxnRequest> {

    /**
     * Unique 64-bit ID for order
     * @see QueryTxnRequest#orderid
     */
    public long orderid;

    /**
     * Unique 64-bit Steam transaction ID
     * @see InitTxnResponse#transid
     */
    public long transid;

    /**
     * 64 bit SteamID of user
     * @see com.pragmatix.df.server.social.steam.request.InitTxnRequest#steamid
     */
    public String steamid;

    /**
     * Status of order
     */
    public TxnStatus status;

    /**
     * ISO 4217 currency code
     * @see com.pragmatix.df.server.social.steam.request.InitTxnRequest#currency
     */
    public String currency;

    /**
     * Time of transaction, for example "2010-01-01T00:00:00Z"
     */
    public String time;

    /**
     * ISO 3166-1-alpha-2 country code
     * @see GetUserInfoResponse#country
     */
    public String country;

    /**
     * US State. Empty for non-US countries.
     * @see GetUserInfoResponse#state
     */
    public String usstate;

    /**
     * One or more items of shopping cart
     */
    public List<OrderItemStructure> items;

    // for json deserialization
    public QueryTxnResponse() { }

    @Override
    public String toString() {
        return "QueryTxnResponse{" +
                super.toString() +
                ", orderid=" + orderid +
                ", transid=" + transid +
                ", steamid=" + steamid +
                ", status=" + status +
                ", currency='" + currency + '\'' +
                ", time='" + time + '\'' +
                ", country='" + country + '\'' +
                (usstate != null && !usstate.isEmpty() ? ", usstate='" + usstate + '\'' : "") +
                ", items=" + OrderItemStructure.mkString(items, currency) +
                "} ";
    }
}
