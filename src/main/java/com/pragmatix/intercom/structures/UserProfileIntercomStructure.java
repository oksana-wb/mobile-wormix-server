package com.pragmatix.intercom.structures;

import com.pragmatix.arena.mercenaries.messages.BackpackItemShortStruct;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * Структура для пересылки полного профиля игрока между серверами
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.05.2017 12:29
 */
@Structure(nullable = true)
public class UserProfileIntercomStructure {

    public long id;
    public String name;
    public int money;
    public int realmoney;
    public int rating;
    public short armor;
    public short attack;
    public int battlesCount;
    public short level;
    public short experience;
    public short hat;
    public short race;
    public short races;
    public int selectRaceTime;
    public short kit;
    public long lastBattleTime;
    public long lastLoginTime;
    public short[] stuff;
    public byte[] temporalStuff;
    public long lastSearchTime;
    public byte loginSequence;
    public int reactionRate;
    public short currentMission;
    public short currentNewMission;
    public short[] recipes;
    public short comebackedFriends;
    public short locale;
    public byte renameAct;
    public byte renameVipAct;
    public int logoutTime;
    public short pickUpDailyBonus;
    public byte[] skins;
    public int vipExpiryTime;

    public int rankPoints;
    public byte bestRank;

    public int[] wormsGroup;
    public List<ByteArrayWrapStructure> teamMembers;
    public String teamMemberNames;
    public short extraGroupSlotsCount;

    public List<BackpackItemShortStruct> backpack;

    public int[] reagents;
    public ProfileTrueSkillStructure trueSkill;
    public ProfileBackpackConfStructure backpackConf;
    public ProfileColiseumStructure coliseum;
    public ProfileMercenariesStructure mercenaries;
    public ProfileQuestStructure quest;
    public String cookies;
    public ProfileAchieveStructure achievements;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMoney() {
        return money;
    }

    public int getRealmoney() {
        return realmoney;
    }

    public int getRating() {
        return rating;
    }

    public short getArmor() {
        return armor;
    }

    public short getAttack() {
        return attack;
    }

    public int getBattlesCount() {
        return battlesCount;
    }

    public short getLevel() {
        return level;
    }

    public short getExperience() {
        return experience;
    }

    public short getHat() {
        return hat;
    }

    public short getRace() {
        return race;
    }

    public short getRaces() {
        return races;
    }

    public int getSelectRaceTime() {
        return selectRaceTime;
    }

    public short getKit() {
        return kit;
    }

    public long getLastBattleTime() {
        return lastBattleTime;
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public short[] getStuff() {
        return stuff;
    }

    public byte[] getTemporalStuff() {
        return temporalStuff;
    }

    public long getLastSearchTime() {
        return lastSearchTime;
    }

    public byte getLoginSequence() {
        return loginSequence;
    }

    public int getReactionRate() {
        return reactionRate;
    }

    public short getCurrentMission() {
        return currentMission;
    }

    public short getCurrentNewMission() {
        return currentNewMission;
    }

    public short[] getRecipes() {
        return recipes;
    }

    public short getComebackedFriends() {
        return comebackedFriends;
    }

    public short getLocale() {
        return locale;
    }

    public byte getRenameAct() {
        return renameAct;
    }

    public byte getRenameVipAct() {
        return renameVipAct;
    }

    public int getLogoutTime() {
        return logoutTime;
    }

    public short getPickUpDailyBonus() {
        return pickUpDailyBonus;
    }

    public byte[] getSkins() {
        return skins;
    }

    public int getVipExpiryTime() {
        return vipExpiryTime;
    }

    public int getRankPoints() {
        return rankPoints;
    }

    public byte getBestRank() {
        return bestRank;
    }

    public int[] getWormsGroup() {
        return wormsGroup;
    }

    public List<ByteArrayWrapStructure> getTeamMembers() {
        return teamMembers;
    }

    public String getTeamMemberNames() {
        return teamMemberNames;
    }

    public short getExtraGroupSlotsCount() {
        return extraGroupSlotsCount;
    }

    public List<BackpackItemShortStruct> getBackpack() {
        return backpack;
    }

    public int[] getReagents() {
        return reagents;
    }

    public ProfileTrueSkillStructure getTrueSkill() {
        return trueSkill;
    }

    public ProfileBackpackConfStructure getBackpackConf() {
        return backpackConf;
    }

    public ProfileColiseumStructure getColiseum() {
        return coliseum;
    }

    public ProfileMercenariesStructure getMercenaries() {
        return mercenaries;
    }

    public ProfileQuestStructure getQuest() {
        return quest;
    }

    public String getCookies() {
        return cookies;
    }

    @Override
    public String toString() {
        return "UserProfileIntercomStructure{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", money=" + money +
                ", realmoney=" + realmoney +
                ", rating=" + rating +
                ", armor=" + armor +
                ", attack=" + attack +
                ", battlesCount=" + battlesCount +
                ", level=" + level +
                ", experience=" + experience +
                ", hat=" + hat +
                ", race=" + race +
                ", races=" + races +
                ", selectRaceTime=" + selectRaceTime +
                ", kit=" + kit +
                ", lastBattleTime=" + lastBattleTime +
//                ", lastLoginTime=" + lastLoginTime +
                ", stuff=" + Arrays.toString(stuff) +
                ", temporalStuff=" + Arrays.toString(temporalStuff) +
                ", lastSearchTime=" + lastSearchTime +
                ", loginSequence=" + loginSequence +
                ", reactionRate=" + reactionRate +
                ", currentMission=" + currentMission +
                ", currentNewMission=" + currentNewMission +
                ", recipes=" + Arrays.toString(recipes) +
                ", comebackedFriends=" + comebackedFriends +
                ", locale=" + locale +
                ", renameAct=" + renameAct +
                ", renameVipAct=" + renameVipAct +
//                ", logoutTime=" + logoutTime +
                ", pickUpDailyBonus=" + pickUpDailyBonus +
                ", skins=" + Arrays.toString(skins) +
                ", vipExpiryTime=" + vipExpiryTime +
                ", rankPoints=" + rankPoints +
                ", bestRank=" + bestRank +
                ", wormsGroup=" + Arrays.toString(wormsGroup) +
//                ", teamMembers=" + teamMembers +
                ", teamMemberNames='" + teamMemberNames + '\'' +
                ", extraGroupSlotsCount=" + extraGroupSlotsCount +
                ", backpack=" + backpack +
                ", reagents=" + Arrays.toString(reagents) +
                ", trueSkill=" + trueSkill +
//                ", backpackConf=" + backpackConf +
                ", coliseum=" + coliseum +
                ", mercenaries=" + mercenaries +
                ", quest=" + quest +
                ", cookies='" + cookies + '\'' +
                '}';
    }
}