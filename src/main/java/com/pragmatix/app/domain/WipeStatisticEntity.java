
package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.Identifiable;

import java.io.Serializable;
import java.util.Date;

/**
 * Статистика по наградам игрока
 * User: denis
 * Date: 01.08.2010
 * Time: 21:38:47
 */

public class WipeStatisticEntity implements Identifiable<Long>, Serializable {

    private Long id;

    /**
     * id профайла которому(который) обнулили профиль
     */
    private long profileId;

    /**
     * фузы которые у него были (+ стоимость предметов)
     */
    private int money;

    /**
     * рубины которые у него были (+ стоимость предметов)
     */
    private int realmoney;

    /**
     * время
     */
    private Date date;
    /**
     * login админа который обнулил (если через админку)
     */
    private String adminLogin;
    /**
     * уровень который был у игрока
     */
    private int level;
    /**
     * рейтинг
     */
    private int rating;
    /**
     * скорость реакции
     */
    private int reactionRate;

    private String cmd;

    private String profileStructure;

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getRealmoney() {
        return realmoney;
    }

    public void setRealmoney(int realmoney) {
        this.realmoney = realmoney;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAdminLogin() {
        return adminLogin;
    }

    public void setAdminLogin(String adminLogin) {
        this.adminLogin = adminLogin;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public int getReactionRate() {
        return reactionRate;
    }

    public void setReactionRate(int reactionRate) {
        this.reactionRate = reactionRate;
    }

    public String getProfileStructure() {
        return profileStructure;
    }

    public void setProfileStructure(String profileStructure) {
        this.profileStructure = profileStructure;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }
}
