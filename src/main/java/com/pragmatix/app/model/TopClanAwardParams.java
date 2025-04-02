package com.pragmatix.app.model;

import com.pragmatix.clanserver.domain.Rank;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.04.2016 9:30
 */
public class TopClanAwardParams {

    public int minRating = 10000;

    public List<Byte> reagents = Collections.EMPTY_LIST;// = {50, 51};

    public int reactionRatio = 15;

    public int experienceRatio = 1;

    public List<SeasonWeaponItem> seasonWeapons = Collections.EMPTY_LIST;

    public List<Item> items = Collections.EMPTY_LIST;// = {{113, 0.8, 8.0}, {108, 0.4, 6.0}, {101.0, 0.4, 6.0}};

    public int getMinRating() {
        return minRating;
    }

    public void setMinRating(int minRating) {
        this.minRating = minRating;
    }

    public List<Byte> getReagents() {
        return reagents;
    }

    public void setReagents(List<Byte> reagents) {
        this.reagents = reagents;
    }

    public int getReactionRatio() {
        return reactionRatio;
    }

    public void setReactionRatio(int reactionRatio) {
        this.reactionRatio = reactionRatio;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public int getExperienceRatio() {
        return experienceRatio;
    }

    public void setExperienceRatio(int experienceRatio) {
        this.experienceRatio = experienceRatio;
    }

    public List<SeasonWeaponItem> getSeasonWeapons() {
        return seasonWeapons;
    }

    public void setSeasonWeapons(List<SeasonWeaponItem> seasonWeapons) {
        this.seasonWeapons = seasonWeapons;
    }

    public static class Item {

        public short leaderItemId;
        public short officerItemId;
        public short soldierItemId;
        public int placeFrom;
        public int placeTo;

        public Optional<Short> getItemId(Rank rank){
            switch (rank){
                case LEADER: return leaderItemId > 0 ? Optional.of(leaderItemId) :  Optional.empty();
                case OFFICER: return officerItemId > 0 ? Optional.of(officerItemId) :  Optional.empty();
                case SOLDIER: return soldierItemId > 0 ? Optional.of(soldierItemId) :  Optional.empty();
            }
            return Optional.empty();
        }

        public int getLeaderItemId() {
            return leaderItemId;
        }

        public void setLeaderItemId(short leaderItemId) {
            this.leaderItemId = leaderItemId;
        }

        public short getOfficerItemId() {
            return officerItemId;
        }

        public void setOfficerItemId(short officerItemId) {
            this.officerItemId = officerItemId;
        }

        public short getSoldierItemId() {
            return soldierItemId;
        }

        public void setSoldierItemId(short soldierItemId) {
            this.soldierItemId = soldierItemId;
        }

        public int getPlaceFrom() {
            return placeFrom;
        }

        public void setPlaceFrom(int placeFrom) {
            this.placeFrom = placeFrom;
        }

        public int getPlaceTo() {
            return placeTo;
        }

        public void setPlaceTo(int placeTo) {
            this.placeTo = placeTo;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "leaderItemId=" + leaderItemId +
                    ", officerItemId=" + officerItemId +
                    ", soldierItemId=" + soldierItemId +
                    ", placeFrom=" + placeFrom +
                    ", placeTo=" + placeTo +
                    '}';
        }
    }

}
