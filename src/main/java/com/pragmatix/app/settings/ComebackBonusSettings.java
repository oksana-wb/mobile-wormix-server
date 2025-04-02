package com.pragmatix.app.settings;

import org.springframework.stereotype.Component;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 02.10.12 17:49
 */
public class ComebackBonusSettings extends GenericAward {

    /**
     * сколько дней нужно отсутствовать в игре чтобы получить бонус за возврат
     */
    private int absetDays;

    public int getAbsetDays() {
        return absetDays;
    }

    public void setAbsetDays(int absetDays) {
        this.absetDays = absetDays;
    }

}
