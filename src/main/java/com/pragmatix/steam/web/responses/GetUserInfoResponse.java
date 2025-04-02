package com.pragmatix.steam.web.responses;


import com.pragmatix.steam.web.request.GetUserInfoRequest;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 07.03.2017 16:14
 *         <p>
 * Ответ на запрос на получение информации о пользователе
 *
 * @see GetUserInfoRequest
 * @see <a href="https://partner.steamgames.com/documentation/MicroTxn#GetUserInfo">MicroTxn#GetUserInfo</a>
 */
public class GetUserInfoResponse extends SteamWebResponse<GetUserInfoRequest> {

    /**
     * US State. Empty ("") for non-US countries.
     */
    public String state;

    /**
     *	ISO 3166-1-alpha-2 country code
     */
    public String country;

    /**
     * ISO 4217 currency code of prices
     */
    public String currency;

    /**
     * Status of the account.
     *
     * Can be either ‘Active’ or ‘Trusted’. Trusted accounts have a have a known, good purchase history within Steam.
     * Use this flag to relax your own fraud checking as appropriate.
     */
    public String status;

    // for json deserialization
    public GetUserInfoResponse() {
    }

    public GetUserInfoResponse(GetUserInfoRequest request) {
        super(request);
    }

    public boolean isTrusted() {
        return "Trusted".equals(status);
    }

    @Override
    public String toString() {
        return "GetUserInfoResponse{" +
                super.toString() +
                ", state='" + state + '\'' +
                ", country='" + country + '\'' +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
