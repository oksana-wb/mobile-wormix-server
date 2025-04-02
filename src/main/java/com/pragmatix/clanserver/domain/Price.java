package com.pragmatix.clanserver.domain;

/**
 * Author: Vladimir
 * Date: 12.04.13 15:11
 */
public class Price {
    public static final int CURRENCY_RUBY = 0;
    public static final int CURRENCY_FUSY = 1;

    public int currency;

    public int amount;

    public Product product;

    public Object details;

    public Price(int currency, int amount, Product product, Object details) {
        this.currency = currency;
        this.amount = amount;
        this.product = product;
        this.details = details;
    }
}
