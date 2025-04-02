package com.pragmatix.clanserver.domain;

/**
 * Author: Vladimir
 * Date: 28.05.13 16:56
 */
public class ClanLevel {
    private static final ClanLevel[] values = new ClanLevel[] {
            new ClanLevel(10, 2, new Price(Price.CURRENCY_RUBY, 0, Product.EXPAND_CLAN, 0)),
            new ClanLevel(20, 4, new Price(Price.CURRENCY_RUBY, 50, Product.EXPAND_CLAN, 1)),
//            new ClanLevel(30, 6, new Price(Price.CURRENCY_RUBY, 100, Product.EXPAND_CLAN, 2)),
//            new ClanLevel(40, 8, new Price(Price.CURRENCY_RUBY, 150, Product.EXPAND_CLAN, 3)),
//            new ClanLevel(50, 10, new Price(Price.CURRENCY_RUBY, 200, Product.EXPAND_CLAN, 4)),
    };

    public static final int FIRST = 1;
    public static final int LAST = values.length + 1;

    public final int capacity;

    public final int officers;
    
    public final Price price;

    private ClanLevel(int capacity, int officers, Price price) {
        this.capacity = capacity;
        this.officers = officers;
        this.price = price;
    }
    
    public static ClanLevel get(int level) {
        return values[level - 1];
    }
}
