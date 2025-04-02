package com.pragmatix.achieve.domain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.07.11 18:30
 */
public class WormixAchievements extends ProfileAchievements {

    public static final int MAX_ACHIEVE_INDEX = 42; // id начинаются с 0

    public static final int MAX_STAT_INDEX = 31; // id начинаются с STAT_FIRST_INDEX (50)

    public static final int MAX_BOOL_ACHIEVE_INDEX = 97; // id начинаются с 0

    public enum AchievementName implements IAchievementName {
        burned_enemies(0),
        destroyed_square(1),
        drowned_opponents(2),
        wager_winner(3),
        diplomat(4),
        wind_kills(5),
        massive_damage(6),
        graves_sank(7),
        one_hp_win(8),
        immobile_kills(9),
        zero_looses_victory(10),
        flew_miles(11),
        double_killls(12),
        medkit_kills(13),
        gathered_supplies(14),
        kamikaze(15),
        roped_miles(16),
        fuzzes_spent(17),
        rubies_spent(18),
        pumped_reaction(19),
        rubies_found(20),
        made_photo(21),
        made_video(22),
        game_visits(23),
        idol(24),
        inquisitor(25),
        hatlover(26),
        partisan(27),
        collector(28),
        drop_water_first_turn(29),
        keymaster(30),
        crafter(31),
        craft_legendary(32),
        superboss_defeated(33),
        posts_made(34),
        buy_race(35),
        coliseum_win(36),
        with_friend_win(37),
        coliseum_win_10(38),
        callback_friends(39),
        exotic_weapon_master(40),
        reward_chest(41),
        buy_skin(MAX_ACHIEVE_INDEX),

        s_pvp_played(STAT_FIRST_INDEX, true),
        s_pvp_won(STAT_FIRST_INDEX + 1, true),
        s_pvp_draws(STAT_FIRST_INDEX + 2, true),
        s_missions_played(STAT_FIRST_INDEX + 3, true),
        s_missions_won(STAT_FIRST_INDEX + 4, true),
        s_wagers_earned(STAT_FIRST_INDEX + 5, true),
        s_missions_earned(STAT_FIRST_INDEX + 6, true),
        s_rubies_found(STAT_FIRST_INDEX + 7, true),
        s_solo_damage(STAT_FIRST_INDEX + 8, true),
        s_team_damage(STAT_FIRST_INDEX + 9, true),
        s_solo_damage_taken(STAT_FIRST_INDEX + 10, true),
        s_team_damage_taken(STAT_FIRST_INDEX + 11, true),
        s_friendly_fire(STAT_FIRST_INDEX + 12, true),
        s_sniper_shots(STAT_FIRST_INDEX + 13, true),
        s_wind_shots(STAT_FIRST_INDEX + 14, true),
        s_killed_worms(STAT_FIRST_INDEX + 15, true),
        s_killed_boxers(STAT_FIRST_INDEX + 16, true),
        s_killed_demons(STAT_FIRST_INDEX + 17, true),
        s_killed_rabbits(STAT_FIRST_INDEX + 18, true),
        s_killed_zombies(STAT_FIRST_INDEX + 19, true),
        s_killed_revived(STAT_FIRST_INDEX + 20, true),
        s_enemies_drowned(STAT_FIRST_INDEX + 21, true),
        s_units_lost(STAT_FIRST_INDEX + 22, true),
        s_collected_medkits(STAT_FIRST_INDEX + 23, true),
        s_collected_crates(STAT_FIRST_INDEX + 24, true),
        s_collected_stars(STAT_FIRST_INDEX + 25, true),
        s_destroyed_supplies(STAT_FIRST_INDEX + 26, true),
        s_graves_sank(STAT_FIRST_INDEX + 27, true),
        s_killed_robots(STAT_FIRST_INDEX + 28, true),
        s_killed_rhinos(STAT_FIRST_INDEX + 29, true),
        s_killed_boars(STAT_FIRST_INDEX + 30, true),
        s_killed_aliens(STAT_FIRST_INDEX + MAX_STAT_INDEX, true),
        ;

        private int index;
        private boolean stat = false;

        private static final Map<Integer, AchievementName> enumMap = new HashMap<>();

        AchievementName(int index) {
            this(index, false);
        }

        AchievementName(int index, boolean stat) {
            this.index = index;
            this.stat = stat;
        }

        @Override
        public int getIndex() {
            return index;
        }

        public boolean isStat() {
            return stat;
        }

        public static AchievementName valueOf(int index) {
            return enumMap.get(index);
        }

        @Override
        public String toString() {
            return String.format("%s(%s)", name(), index);
        }
    }

    static {
        for(AchievementName achievementName : AchievementName.values()) {
            AchievementName.enumMap.put(achievementName.getIndex(), achievementName);
        }
    }

    public WormixAchievements(String profileId) {
        super(profileId);
    }

    public String mkString() {
        return "WormixAchievements{" +
                "profileId=" + profileId +
                ", timeSequence=" + timeSequence +
                ", userProfileId=" + userProfileId +
                ", investedAwardPoints=" + investedAwardPoints +
                ", achievements=" + Arrays.toString(achievements) +
                ", statistics=" + Arrays.toString(statistics) +
                ", boolAchievements=" + Arrays.toString(boolAchievements) +
                "}";
    }

    @Override
    public int getMaxAchievementIndex() {
        return MAX_ACHIEVE_INDEX;
    }

    @Override
    public int getMaxStatIndex() {
        return MAX_STAT_INDEX;
    }

    @Override
    public int getMaxBoolAchievementIndex() {
        return MAX_BOOL_ACHIEVE_INDEX;
    }

    @Override
    public IAchievementName getAchievementNameByIndex(int achievementIndex) {
        return AchievementName.valueOf(achievementIndex);
    }

}
