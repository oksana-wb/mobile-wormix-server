package com.pragmatix.steam.domain;

import com.pragmatix.app.common.PaymentType;

import java.util.Map;

import static com.pragmatix.app.common.PaymentType.*;

/**
 * Author: Vladimir
 * Date: 10.03.2017 10:49
 */
public class SteamProduct {

    public enum SteamProductCategory {
        ruby(REAL_MONEY),
        fuzy(MONEY),
        rename(RENAME),
        vip(VIP),
        daily_ruby(DEPOSIT),
        daily_fuzy(DEPOSIT),
        stickers(BUNDLE),
        craft_box(BUNDLE),
        clan_donate(CLAN_DONATE),
        ;

        public final PaymentType paymentType;

        SteamProductCategory(PaymentType paymentType) {
            this.paymentType = paymentType;
        }
    }

    public final Integer id;
    public final SteamProductCategory category;
    public final String code;
    public final int count;
    public final int paymentAmountComeback;
    public final Map<String, String> descriptions;

    public SteamProduct(int id, SteamProductCategory category, String code, Map<String, String> descriptions, int count, int paymentAmountComeback) {
        this.id = id;
        this.category = category;
        this.code = code;
        this.descriptions = descriptions;
        this.count = count;
        this.paymentAmountComeback = paymentAmountComeback;
    }

    public String getDescription(String langCode) {
        return descriptions.get(langCode.toUpperCase());
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        SteamProduct that = (SteamProduct) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id;
    }

    public static String codeKey(String category, String code) {
        return category + "." + code;
    }

    public static String defKey(String category, short defId, int amount) {
        return category + "." + defId + "." + amount;
    }

    public static String codeTagName(String code) {
        String res;
        int ix = code.indexOf('+');
        if(ix > 0) {
            res = code.substring(0, ix);
        } else {
            res = code;
        }
        return res;
    }

    @Override
    public String toString() {
        return code;
    }
}
