package com.pragmatix.steam.domain;

/**
 * Author: Vladimir
 * Date: 10.03.2017 10:46
 */
public class SteamCurrency {
    public final String code;
    public final String name;
    public final int centRounding;
    public final String[] segments;

    public SteamCurrency(String code, String name, int centRounding, String[] segments) {
        this.code = code;
        this.name = name;
        this.centRounding = centRounding;
        this.segments = segments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SteamCurrency that = (SteamCurrency) o;

        return code.equals(that.code);

    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }
}
