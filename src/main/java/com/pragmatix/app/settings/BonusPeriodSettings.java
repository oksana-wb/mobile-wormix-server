package com.pragmatix.app.settings;

import com.pragmatix.app.common.Locale;

import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

/**
 * Настройки наград в бонусный период
 * <p>
 * User: denver
 * Date: 13.03.2010
 * Time: 23:42:31
 */
public class BonusPeriodSettings extends GenericAward {

    private final int id;
    
    /**
     * начало бонусного периода (грузится из базы)
     */
    private Date startBonusDay;

    /**
     * окончание бонусного периода (грузится из базы)
     */
    private Date endBonusDay;

    private int levelMin = 1;
    
    private int levelMax = 30;
    
    /**
     * пояснение к бонусу
     */
    final private Map<Locale, String> bonusMessage = new EnumMap<>(Locale.class);

    public String temporalItems = "";

    public BonusPeriodSettings(int id) {
        this.id = id;
    }

    //====================== Getters and Setters =================================================================================================================================================

    public int getId() {
        return id;
    }

    public boolean isValid() {
        return endBonusDay != null && startBonusDay != null;
    }

    public Date getEndBonusDay() {
        return endBonusDay;
    }

    public void setEndBonusDay(Date endBonusDay) {
        this.endBonusDay = endBonusDay;
    }

    public Date getStartBonusDay() {
        return startBonusDay;
    }

    public void setStartBonusDay(Date startBonusDay) {
        this.startBonusDay = startBonusDay;
    }

    public String getBonusMessage(Locale locale) {
        return bonusMessage.get(locale);
    }

    public void setBonusMessage(Locale locale, String bonusMessage) {
        this.bonusMessage.put(locale, bonusMessage);
    }

    public int getLevelMin() {
        return levelMin;
    }

    public int getLevelMax() {
        return levelMax;
    }

    public void setLevelMin(int levelMin) {
        this.levelMin = levelMin;
    }

    public void setLevelMax(int levelMax) {
        this.levelMax = levelMax;
    }
}
