package com.pragmatix.app.messages.structures;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.rating.RankService;
import com.pragmatix.clan.structures.ClanMemberStructure;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Resize;
import com.pragmatix.serialization.annotations.Serialize;
import com.pragmatix.serialization.annotations.Structure;
import io.vavr.Tuple2;
import io.vavr.Tuple3;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Упрощенный профиль игрока
 */
@Structure
public class SimpleProfileStructure implements Serializable {

    private static final long serialVersionUID = -2016997175233349496L;
    /**
     * id профайла на сервере
     */
    @Resize(TypeSize.UINT32)
    public long id;
    /**
     * Изменённое имя, отображаемое в PVP
     */
    public String name;
    /**
     * броня червя
     */
    public int armor;
    /**
     * атака червя, увеличивает силу удара
     */
    public int attack;
    /**
     * уровень червя
     */
    public int level;
    /**
     * Количество игровых денег
     */
    public int money;
    /**
     * Количество реалов
     */
    public int realMoney;
    /**
     * реакция топера
     */
    public int reactionRate;
    /**
     * Рейтинг игрока
     */
    public int rating;
    /**
     * количество червей в команде
     */
    public int groupCount;
    /**
     * шапка
     */
    public short hat;
    /**
     * раса
     */
    public short race;
    /**
     * снаряжение
     */
    public short kit;
    /**
     * Строковый ID
     */
    public String profileStringId;

    transient public ClanMemberStructure clanMember;

    public byte rank;

    public byte skin;
    /**
     *  время когда истекает действие VIP аккаутна (в секундах)
     */
    @Serialize(ifExpr = "com.pragmatix.app.settings.AppParams.IS_MOBILE()")
    public int vipExpiryTime;

    public SimpleProfileStructure() {
    }

    public SimpleProfileStructure(UserProfile profile, Callable<Tuple3<ClanMemberStructure, Byte, Byte>> clanMember_rank_skin) {
        this.id = profile.getId();
        this.profileStringId = profile.getProfileStringId();
        init(profile, clanMember_rank_skin);
    }

    public void init(UserProfile profile, Callable<Tuple3<ClanMemberStructure, Byte, Byte>> clanMember_rank_skin) {
        this.name = profile.getName();

        this.hat = profile.getHat();
        this.race = profile.getRace();
        this.kit = profile.getKit();
        this.armor = profile.getArmor();
        this.attack = profile.getAttack();
        this.level = profile.getLevel();
        this.money = profile.getMoney();
        this.realMoney = profile.getRealMoney();
        this.reactionRate = profile.getReactionRate();
        this.rating = profile.getRating();
        this.groupCount = profile.getWormsGroup() != null ? profile.getActiveTeamMembersCount() : 1;
        try {
            Tuple3<ClanMemberStructure, Byte, Byte> tuple = clanMember_rank_skin.call();
            this.clanMember = tuple._1;
            this.rank = tuple._2;
            this.skin = tuple._3;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.vipExpiryTime = profile.getVipExpiryTime();
    }

    public void wipe() {
        this.id = 0;
        this.profileStringId = "";
        this.name = null;

        this.hat = 0;
        this.race = 0;
        this.kit = 0;
        this.armor = 0;
        this.attack = 0;
        this.level = 0;
        this.money = 0;
        this.realMoney = 0;
        this.reactionRate = 0;
        this.rating = 0;
        this.groupCount = 0;

        this.clanMember = new ClanMemberStructure(null);

        this.rank = RankService.INIT_RANK_VALUE;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        SimpleProfileStructure that = (SimpleProfileStructure) o;

        if(id != that.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "SimpleProfileStructure{" +
                "id=" + (profileStringId == null || profileStringId.isEmpty() ? "" : profileStringId + ":") + id +
                ", groupCount=" + groupCount +
                ", hat=" + hat +
                ", race=" + race +
                ", kit=" + kit +
                '}';
    }

    public long getProfileId() {
        return id;
    }

    public void setProfileId(long id) {
        this.id = id;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        this.armor = armor;
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getRealMoney() {
        return realMoney;
    }

    public void setRealMoney(int realMoney) {
        this.realMoney = realMoney;
    }

    public int getReactionRate() {
        return reactionRate;
    }

    public void setReactionRate(int reactionRate) {
        this.reactionRate = reactionRate;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(int groupCount) {
        this.groupCount = groupCount;
    }

    public short getHat() {
        return hat;
    }

    public void setHat(short hat) {
        this.hat = hat;
    }

    public short getRace() {
        return race;
    }

    public void setRace(short race) {
        this.race = race;
    }

    public short getKit() {
        return kit;
    }

    public void setKit(short kit) {
        this.kit = kit;
    }

    public String getProfileStringId() {
        return profileStringId;
    }

    public void setProfileStringId(String profileStringId) {
        this.profileStringId = profileStringId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVipExpiryTime() {
        return vipExpiryTime;
    }

    public void setVipExpiryTime(int vipExpiryTime) {
        this.vipExpiryTime = vipExpiryTime;
    }

}
