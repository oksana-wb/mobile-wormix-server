package com.pragmatix.steam.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Vladimir
 * Date: 10.03.2017 10:53
 */
public class SteamPriceSegment {

    public final String code;
    public final SteamCurrency currency;
    public final List<SteamCountry> countries;
    public final Map<SteamProduct, Integer> productPrices;

    public SteamPriceSegment(String code, SteamCurrency currency) {
        this.code = code;
        this.currency = currency;
        this.countries = new ArrayList<>();
        this.productPrices = new LinkedHashMap<>();
    }

    public void addCountry(SteamCountry country) {
        countries.add(country);
    }

    public void addProductPrice(SteamProduct product, int price) {
        productPrices.put(product, price);
    }

    public int getProductCost(SteamProduct product) {
        Integer price = productPrices.get(product);
        if(price != null) {
            return price;
        }
        throw new IllegalArgumentException(String.format("Invalid product %s", product));
    }

    @Override
    public String toString() {
        return code;
    }
}
