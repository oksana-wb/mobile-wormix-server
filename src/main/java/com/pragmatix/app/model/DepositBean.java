package com.pragmatix.app.model;

import com.pragmatix.app.common.MoneyType;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 18.04.2016 13:55
 *         <p>
 * Конфигурация депозита
 */
public class DepositBean {

    private String id;

    /**
     * в какой валюте депозит
     */
    private MoneyType moneyType;

    /**
     * сколько выплачивается в каждый из дней
     */
    private int[] dividendsByDays;
    private String dividendsByDaysStr;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MoneyType getMoneyType() {
        return moneyType;
    }

    public void setMoneyType(MoneyType moneyType) {
        this.moneyType = moneyType;
    }

    public int getPartsCount() {
        return dividendsByDays.length;
    }

    public int[] getDividendsByDays() {
        return dividendsByDays;
    }

    public void setDividendsByDays(int[] dividendsByDays) {
        this.dividendsByDays = dividendsByDays;
    }

    /**
     * первую долю игрок получает сразу при открытии
     */
    public int getImmediateDividend() {
        return dividendsByDays[0];
    }

    public String getDividendsByDaysStr() {
        return dividendsByDaysStr;
    }

    public void setDividendsByDaysStr(String dividendsByDaysStr) {
        this.dividendsByDaysStr = dividendsByDaysStr;
    }

    public int getTotalValue() {
        int res = 0;
        if (dividendsByDays != null) {
            for (int part : dividendsByDays) {
                res += part;
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return "DepositBean{" +
                "id=" + id +
                ", moneyType=" + moneyType +
                ", dividendsByDays=[" + dividendsByDaysStr + ']' +
                '}';
    }
}
