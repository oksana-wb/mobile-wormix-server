package com.pragmatix.app.common;


import com.pragmatix.gameapp.common.TypeableEnum;

import javax.validation.constraints.Null;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 02.02.2015 17:02
 */
public enum PaymentType implements TypeableEnum {

    UNDEFINED(-1),
    REAL_MONEY(0),
    MONEY(1),
    RENAME(2),
    CLAN_DONATE(3),
    WIPE(4),
    BUNDLE(5),
    VIP(6),
    DEPOSIT(7),
    ;

    public final int type;

    PaymentType(int type) {
        this.type = type;
    }

    @Null
    public static PaymentType valueOf(int type) {
        for(PaymentType paymentType : values()) {
            if(paymentType.type == type)
                return paymentType;
        }
        return null;
    }

    @Override
    public int getType() {
        return type;
    }
}
