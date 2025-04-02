package com.pragmatix.steam.messages;

import com.pragmatix.serialization.annotations.Command;

import java.util.List;

/**
 * @see ProductsInfoRequest
 */
@Command(6006)
public class ProductsInfoResponse {

    public String countryCode;

    public String currencyCode;

    public List<SteamProductInfo> products;

    @Override
    public String toString() {
        return "PurchaseInfoResponse{" +
                "countryCode='" + countryCode + '\'' +
                ", currencyCode='" + currencyCode + '\'' +
                ", products=" + products  +
                '}';
    }

}
