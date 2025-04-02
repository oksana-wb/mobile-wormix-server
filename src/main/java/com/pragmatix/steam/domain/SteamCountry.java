package com.pragmatix.steam.domain;

/**
 * Author: Vladimir
 * Date: 10.03.2017 10:55
 */
public class SteamCountry {
    public final String code;
    public final String name;
    public SteamPriceSegment segment;

    public SteamCountry(String code, String name, SteamPriceSegment segment) {
        this.code = code;
        this.name = name;
        this.segment = segment;
    }

    @Override
    public String toString() {
        return code;
    }
}
