package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.Identifiable;

import java.io.Serializable;
import java.util.Date;

/**
 * User: denis
 * Date: 13.11.2009
 * Time: 1:02:45
 */
public class UserProfileEntity implements Identifiable<Long>, Serializable {
    /**
     * id записи
     * соответствует id в социалке
     */
    private Long id;

    /**
     * количество игровых денег
     */
    public int money;
    /**
     * количество рельных денег
     */
    public int realmoney;

    /**
     * Рейтинг игрока
     */
    public int rating;

    public short armor;

    public short attack;

    /**
     * количество доступных боёв
     */
    public int battlesCount;

    public short level;

    public short experience;

    /**
     * id шляпы которая на голове
     */
    public short hat;

    /**
     * раса
     */
    public Short race;

    /**
     * снаряжение
     */
    public Short kit;

    /**
     * время последнего боя
     */
    public Date lastBattleTime;

    /**
     * время когда последний раз логинились
     */
    public Date lastLoginTime;

    /**
     * время когда последний раз обыскивали домик
     * если null, то никогда
     */
    public Date lastSearchTime;

    /**
     * количество непрерывных заходов (каждый день), максимум ограничен 5-тю
     */
    public Byte loginSequence;

    /**
     * скорость реакции
     */
    public Integer reactionRate;

    /**
     * массив шапок, амулетов и тд
     */
    public short[] stuff;

    /**
     * массив предметов имеющих срок действия
     */
    public byte[] temporalStuff;

    /**
     * id последней пройденной мисии
     */
    public Short currentMission;

    /**
     * id последней пройденной мисии
     */
    public Short currentNewMission;

    public short[] recipes;

    public Short comebackedFriends;

    public Short locale = 1;

    public String name;

    public Byte renameAct;

    public Byte renameVipAct;

    public Date logoutTime;

    public Short pickUpDailyBonus;

    public Short races;

    public Integer selectRaceTime;

    public byte[] skins;

    public Date vipExpiryTime;

    public Date lastPaymentDate;

    public Integer vipSubscriptionId;

    private String countryCode;

    private String currencyCode;

    private Date levelUpTime;

    private Short releaseAward;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return количество игровых денег игрока
     */
    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    /**
     * @return количество реальных денег игрока
     */
    public int getRealmoney() {
        return realmoney;
    }

    public void setRealmoney(int realmoney) {
        this.realmoney = realmoney;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public short getArmor() {
        return armor;
    }

    public void setArmor(short armor) {
        this.armor = armor;
    }

    public short getAttack() {
        return attack;
    }

    public void setAttack(short attack) {
        this.attack = attack;
    }

    public int getBattlesCount() {
        return battlesCount;
    }

    public void setBattlesCount(int battlesCount) {
        this.battlesCount = battlesCount;
    }

    public short getLevel() {
        return level;
    }

    public void setLevel(short level) {
        this.level = level;
    }

    public short getExperience() {
        return experience;
    }

    public void setExperience(short experience) {
        this.experience = experience;
    }

    public short getHat() {
        return hat;
    }

    public void setHat(short hat) {
        this.hat = hat;
    }

    public Short getRace() {
        return race;
    }

    public void setRace(Short race) {
        this.race = race;
    }

    public Short getKit() {
        return kit;
    }

    public void setKit(Short kit) {
        this.kit = kit;
    }

    public Date getLastBattleTime() {
        return lastBattleTime;
    }

    public void setLastBattleTime(Date lastBattleTime) {
        this.lastBattleTime = lastBattleTime;
    }

    public Date getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public Date getLastSearchTime() {
        return lastSearchTime;
    }

    public void setLastSearchTime(Date lastSearchTime) {
        this.lastSearchTime = lastSearchTime;
    }

    public short[] getStuff() {
        return stuff;
    }

    public byte[] getTemporalStuff() {
        return temporalStuff;
    }

    public void setStuff(short[] stuff) {
        this.stuff = stuff;
    }

    public Byte getLoginSequence() {
        return loginSequence;
    }

    public Integer getReactionRate() {
        return reactionRate;
    }

    public void setReactionRate(Integer reactionRate) {
        this.reactionRate = reactionRate;
    }

    public void setLoginSequence(Byte loginSequence) {
        this.loginSequence = loginSequence;
    }

    public Short getCurrentMission() {
        return currentMission;
    }

    public void setCurrentMission(Short currentMission) {
        this.currentMission = currentMission;
    }

    public short[] getRecipes() {
        return recipes;
    }

    public void setTemporalStuff(byte[] temporalStuff) {
        this.temporalStuff = temporalStuff;
    }

    public void setRecipes(short[] recipes) {
        this.recipes = recipes;
    }

    public Short getComebackedFriends() {
        return comebackedFriends;
    }

    public void setComebackedFriends(Short comebackedFriends) {
        this.comebackedFriends = comebackedFriends;
    }

    public Short getCurrentNewMission() {
        return currentNewMission;
    }

    public void setCurrentNewMission(Short currentNewMission) {
        this.currentNewMission = currentNewMission;
    }

    public Short getLocale() {
        return locale;
    }

    public void setLocale(Short locale) {
        this.locale = locale;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Byte getRenameAct() {
        return renameAct;
    }

    public void setRenameAct(Byte renameAct) {
        this.renameAct = renameAct;
    }

    public Date getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(Date logoutTime) {
        this.logoutTime = logoutTime;
    }

    public Short getPickUpDailyBonus() {
        return pickUpDailyBonus;
    }

    public void setPickUpDailyBonus(Short pickUpDailyBonus) {
        this.pickUpDailyBonus = pickUpDailyBonus;
    }

    public Short getRaces() {
        return races;
    }

    public void setRaces(Short races) {
        this.races = races;
    }

    public Integer getSelectRaceTime() {
        return selectRaceTime;
    }

    public void setSelectRaceTime(Integer selectRaceTime) {
        this.selectRaceTime = selectRaceTime;
    }

    public byte[] getSkins() {
        return skins;
    }

    public void setSkins(byte[] skins) {
        this.skins = skins;
    }

    public Date getVipExpiryTime() {
        return vipExpiryTime;
    }

    public void setVipExpiryTime(Date vipExpiryTime) {
        this.vipExpiryTime = vipExpiryTime;
    }

    public Date getLastPaymentDate() {
        return lastPaymentDate;
    }

    public void setLastPaymentDate(Date lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }

    public Byte getRenameVipAct() {
        return renameVipAct;
    }

    public void setRenameVipAct(Byte renameVipAct) {
        this.renameVipAct = renameVipAct;
    }

    public Integer getVipSubscriptionId() {
        return vipSubscriptionId;
    }

    public void setVipSubscriptionId(Integer subscriptionId) {
        this.vipSubscriptionId = subscriptionId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Date getLevelUpTime() {
        return levelUpTime;
    }

    public void setLevelUpTime(Date levelUpTime) {
        this.levelUpTime = levelUpTime;
    }

    public Short getReleaseAward() {
        return releaseAward;
    }

    public void setReleaseAward(Short releaseAward) {
        this.releaseAward = releaseAward;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserProfileEntity that = (UserProfileEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("UserProfileEntity{id=%d}", id);
    }

}
