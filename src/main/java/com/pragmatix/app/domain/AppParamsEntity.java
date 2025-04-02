package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.Identifiable;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Date;

/**
 * Created: 26.04.11 18:22
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class AppParamsEntity implements Identifiable<Integer>, Serializable {

    @Id
    private Integer id;

    private Date startBonusDay;
    private Date endBonusDay;
    private int levelMin = 1;
    private int levelMax = 30;

    private int bonusMoney = 0;
    private int bonusRealMoney = 0;
    private int bonusBattlesCount = 0;
    private String message = "";
    private String messageEn = "";
    private String bonusReagentsMass = "";
    private int bonusReagentsCount = 0;
    private int keysCount = 0;
    private int wagerToken = 0;
    private int bossToken = 0;

    private String reagents = "";
    private String weaponShoots = "";
    private String temporalItems = "";
    
    private int reaction = 0;

    private String appVersion = "0.0.1.0";
    private String vkAuthSecret = "";

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getStartBonusDay() {
        return startBonusDay;
    }

    public void setStartBonusDay(Date startBonusDay) {
        this.startBonusDay = startBonusDay;
    }

    public Date getEndBonusDay() {
        return endBonusDay;
    }

    public void setEndBonusDay(Date endBonusDay) {
        this.endBonusDay = endBonusDay;
    }

    public int getLevelMin() {
        return levelMin;
    }

    public void setLevelMin(int levelMin) {
        this.levelMin = levelMin;
    }

    public int getLevelMax() {
        return levelMax;
    }

    public void setLevelMax(int levelMax) {
        this.levelMax = levelMax;
    }

    public int getBonusMoney() {
        return bonusMoney;
    }

    public void setBonusMoney(int bonusMoney) {
        this.bonusMoney = bonusMoney;
    }

    public int getBonusRealMoney() {
        return bonusRealMoney;
    }

    public void setBonusRealMoney(int bonusRealMoney) {
        this.bonusRealMoney = bonusRealMoney;
    }

    public int getBonusBattlesCount() {
        return bonusBattlesCount;
    }

    public void setBonusBattlesCount(int bonusBattlesCount) {
        this.bonusBattlesCount = bonusBattlesCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String bonusMessageRu) {
        this.message = bonusMessageRu;
    }

    public String getMessageEn() {
        return messageEn;
    }

    public void setMessageEn(String bonusMessageEn) {
        this.messageEn = bonusMessageEn == null ? "" : bonusMessageEn;
    }

    public String getBonusReagentsMass() {
        return bonusReagentsMass;
    }

    public void setBonusReagentsMass(String bonusReagentsMass) {
        this.bonusReagentsMass = bonusReagentsMass;
    }

    public int getBonusReagentsCount() {
        return bonusReagentsCount;
    }

    public void setBonusReagentsCount(int bonusReagentsCount) {
        this.bonusReagentsCount = bonusReagentsCount;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getVkAuthSecret() {
        return vkAuthSecret;
    }

    public void setVkAuthSecret(String vkAuthSecret) {
        this.vkAuthSecret = vkAuthSecret;
    }

    public int getKeysCount() {
        return keysCount;
    }

    public void setKeysCount(int keysCount) {
        this.keysCount = keysCount;
    }

    public int getWagerToken() {
        return wagerToken;
    }

    public void setWagerToken(int wagerToken) {
        this.wagerToken = wagerToken;
    }

    public int getBossToken() {
        return bossToken;
    }

    public void setBossToken(int bossToken) {
        this.bossToken = bossToken;
    }

    public String getReagents() {
        return reagents;
    }

    public void setReagents(String reagents) {
        this.reagents = reagents;
    }

    public String getWeaponShoots() {
        return weaponShoots;
    }

    public void setWeaponShoots(String weaponShoots) {
        this.weaponShoots = weaponShoots;
    }

    public String getTemporalItems() {
        return temporalItems;
    }

    public void setTemporalItems(String temporalItems) {
        this.temporalItems = temporalItems;
    }

    public int getReaction() {
        return reaction;
    }

    public void setReaction(int reaction) {
        this.reaction = reaction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppParamsEntity that = (AppParamsEntity) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id;
    }

}
