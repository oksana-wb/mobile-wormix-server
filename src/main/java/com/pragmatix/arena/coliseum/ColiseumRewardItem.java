package com.pragmatix.arena.coliseum;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;

import static com.pragmatix.app.services.ProfileEventsService.Param.weapons;

public class ColiseumRewardItem {

    public static byte MEDAL_REAGENT_ID = (byte) 50;

    public short weaponId;
    public int weaponMin;
    public int weaponMax;
    public int reactionMin;
    public int reactionMax;
    public int fuzeMin;
    public int fuzeMax;
    public int rubyMin;
    public int rubyMax;
    public int medalsMin;
    public int medalsMax;
    public boolean seasonStuff;

    public int randomRewardCount;
    public ColiseumRewardItem[] randomReward;

    public short getWeaponId() {
        return weaponId;
    }

    public void setWeaponId(short weaponId) {
        this.weaponId = weaponId;
    }

    public int getWeaponCount() {
        return weaponMin;
    }

    public void setWeaponCount(int weaponCount) {
        this.weaponMin = weaponCount;
        this.weaponMax = weaponCount;
    }

    public int getWeaponMin() {
        return weaponMin;
    }

    public void setWeaponMin(int weaponMin) {
        this.weaponMin = weaponMin;
    }

    public int getWeaponMax() {
        return weaponMax;
    }

    public void setWeaponMax(int weaponMax) {
        this.weaponMax = weaponMax;
    }

    public int getReactionMin() {
        return reactionMin;
    }

    public void setReactionMin(int reactionMin) {
        this.reactionMin = reactionMin;
    }

    public int getReactionMax() {
        return reactionMax;
    }

    public void setReactionMax(int reactionMax) {
        this.reactionMax = reactionMax;
    }

    public int getFuzeMin() {
        return fuzeMin;
    }

    public void setFuzeMin(int fuzeMin) {
        this.fuzeMin = fuzeMin;
    }

    public int getFuzeMax() {
        return fuzeMax;
    }

    public void setFuzeMax(int fuzeMax) {
        this.fuzeMax = fuzeMax;
    }

    public int getRubyMin() {
        return rubyMin;
    }

    public void setRubyMin(int rubyMin) {
        this.rubyMin = rubyMin;
    }

    public int getRubyMax() {
        return rubyMax;
    }

    public void setRubyMax(int rubyMax) {
        this.rubyMax = rubyMax;
    }

    public int getMedalsMin() {
        return medalsMin;
    }

    public void setMedalsMin(int medalsMin) {
        this.medalsMin = medalsMin;
    }

    public int getMedalsMax() {
        return medalsMax;
    }

    public void setMedalsMax(int medalsMax) {
        this.medalsMax = medalsMax;
    }

    public int getRandomRewardCount() {
        return randomRewardCount;
    }

    public void setRandomRewardCount(int randomRewardCount) {
        this.randomRewardCount = randomRewardCount;
    }

    public List<ColiseumRewardItem> getRandomReward() {
        return null;
    }

    public void setRandomReward(List<ColiseumRewardItem> randomReward) {
        this.randomReward = randomReward.toArray(new ColiseumRewardItem[randomReward.size()]);
    }

    public boolean isSeasonStuff() {
        return seasonStuff;
    }

    public void setSeasonStuff(boolean seasonStuff) {
        this.seasonStuff = seasonStuff;
    }

    @Override
    public String toString() {
        return "ColiseumRewardItem{" +
                "weaponId=" + weaponId +
                ", weaponMin=" + weaponMin +
                ", weaponMax=" + weaponMax +
                ", reactionMin=" + reactionMin +
                ", reactionMax=" + reactionMax +
                ", fuzeMin=" + fuzeMin +
                ", fuzeMax=" + fuzeMax +
                ", rubyMin=" + rubyMin +
                ", rubyMax=" + rubyMax +
                ", medalsMin=" + medalsMin +
                ", medalsMax=" + medalsMax +
                ", randomRewardCount=" + randomRewardCount +
                ", randomReward=" + Arrays.toString(randomReward) +
                '}';
    }
}
