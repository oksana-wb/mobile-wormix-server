package com.pragmatix.app.model;

import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.services.SearchTheHouseService;
import com.pragmatix.pvp.BattleWager;

/**
 * Структура содержит данные об игроке, актуальные в течении суток
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.01.12 16:51
 */
public class ProfileDailyStructure {

    /**
     * проходили ли миссию с супер боссами сегодня
     */
    private static final byte SUCCESSED_SUPER_BOSS_MISSION = 0b0000010;
    /**
     * совершал ли сегодня платеж
     */
    private static final byte MAKE_PAYMENT = 0b0000100;
    /**
     * получал ли сегодня спец предложение
     */
    private static final byte RECEIVED_SPECIAL_DEAL = 0b0001000;
    /**
     * забирал ли сегодня очередную часть депозита
     */
    private static final byte DIVIDEND_PAID = 0b0010000;
    /**
     * храним здесь булевские величины
     */
    private byte flags;
    /**
     * суточный рейтинг
     */
    private int[] dailyRatings;
    /**
     * скольким друзьям игрок сегодня прокачал реакцию
     */
    private byte howManyPumped;
    /**
     * количество доступных ключей (попыток) для обыска друга
     */
    private byte searchKeys = SearchTheHouseService.MAX_SEARCH_KEYS_BY_DAY;
    /**
     * id шляпы которая была на голове, до призовой шапки
     */
    private short prevHat;
    /**
     * артефакт который был выбран до призового артефакта
     */
    private short prevKit;
    /**
     * выданные награда, пока игрок был offline
     */
    private LoginAwardStructure[] offlineAwards;

    private byte mercenariesBattleSeries;

    // ставочный наградной билет
    public static final int WagerWinAwardTokenDefault = 1;
    private int wagerWinAwardToken = WagerWinAwardTokenDefault;

    // боссовый наградной билет
    public static final int BossWinAwardTokenDefault = 1;
    private int bossWinAwardToken = BossWinAwardTokenDefault;

    public int getDailyRating(BattleWager battleWager) {
        return dailyRatings != null && battleWager != null ? dailyRatings[battleWager.ordinal()] : 0;
    }

    public int[] getDailyRatings() {
        return dailyRatings;
    }

    public void setDailyRating(int dailyRating, BattleWager battleWager) {
        if(dailyRatings == null) {
            dailyRatings = new int[BattleWager.values().length];
        }
        dailyRatings[battleWager.ordinal()] = dailyRating;
    }

    public byte getHowManyPumped() {
        return howManyPumped;
    }

    public void setHowManyPumped(byte howManyPumped) {
        this.howManyPumped = howManyPumped;
    }

    public byte getSearchKeys() {
        return searchKeys;
    }

    public void setSearchKeys(byte searchKeys) {
        this.searchKeys = searchKeys;
    }

    public short getPrevHat() {
        return prevHat;
    }

    public void setPrevHat(short prevHat) {
        this.prevHat = prevHat;
    }

    public short getPrevKit() {
        return prevKit;
    }

    public void setPrevKit(short prevKit) {
        this.prevKit = prevKit;
    }

    public boolean isSuccessedSuperBossMission() {
        return  (flags & SUCCESSED_SUPER_BOSS_MISSION) != 0;
    }

    public void setSuccessedSuperBossMission(boolean value) {
        flags = value ? (byte)(flags | SUCCESSED_SUPER_BOSS_MISSION) : (byte)(flags & ~SUCCESSED_SUPER_BOSS_MISSION);
    }

    public boolean isMakePayment() {
        return  (flags & MAKE_PAYMENT) != 0;
    }

    public void setMakePayment(boolean value) {
        flags = value ? (byte)(flags | MAKE_PAYMENT) : (byte)(flags & ~MAKE_PAYMENT);
    }

    public boolean isReceivedSpecialDeal() {
        return  (flags & RECEIVED_SPECIAL_DEAL) != 0;
    }

    public void setReceivedSpecialDeal(boolean value) {
        flags = value ? (byte)(flags | RECEIVED_SPECIAL_DEAL) : (byte)(flags & ~RECEIVED_SPECIAL_DEAL);
    }

    public LoginAwardStructure[] getOfflineAwards() {
        return offlineAwards;
    }

    public void setOfflineAwards(LoginAwardStructure[] offlineAwards) {
        this.offlineAwards = offlineAwards;
    }

    public byte getMercenariesBattleSeries() {
        return mercenariesBattleSeries;
    }

    public void setMercenariesBattleSeries(byte mercenariesBattleSeries) {
        this.mercenariesBattleSeries = mercenariesBattleSeries;
    }

    public boolean isDividendPaid() {
        return (flags & DIVIDEND_PAID) != 0;
    }

    public void setDividendPaid(boolean value) {
        flags = value ? (byte)(flags | DIVIDEND_PAID) : (byte)(flags & ~DIVIDEND_PAID);
    }

    public int getWagerWinAwardToken() {
        return wagerWinAwardToken;
    }

    public void setWagerWinAwardToken(int wagerWinAwardToken) {
        this.wagerWinAwardToken = wagerWinAwardToken;
    }

    public int getBossWinAwardToken() {
        return bossWinAwardToken;
    }

    public void setBossWinAwardToken(int bossWinAwardToken) {
        this.bossWinAwardToken = bossWinAwardToken;
    }

    public byte getFlags() {
        return flags;
    }

    public void setFlags(byte flags) {
        this.flags = flags;
    }

}
