ALTER TABLE achieve.worms_achievements
    ADD COLUMN callback_friends SMALLINT;
ALTER TABLE achieve.worms_achievements
    ADD COLUMN exotic_weapon_master SMALLINT;
ALTER TABLE achieve.worms_achievements
    ADD COLUMN reward_chest SMALLINT;
ALTER TABLE achieve.worms_achievements
    ADD COLUMN buy_skin SMALLINT;
ALTER TABLE achieve.worms_achievements
    ADD COLUMN s_killed_aliens INTEGER;
ALTER TABLE achieve.worms_achievements
    ADD COLUMN update_date TIMESTAMP WITHOUT TIME ZONE;

COPY (SELECT
          profile_id,
          create_time,
          time_sequence,
          invested_award_points,
          bool_achievements,
          update_date,

          burned_enemies,
          destroyed_square,
          drowned_opponents,
          wager_winner,
          diplomat,
          wind_kills,
          massive_damage,
          graves_sank,
          one_hp_win,
          immobile_kills,
          zero_looses_victory,
          flew_miles,
          double_killls,
          medkit_kills,
          gathered_supplies,
          kamikaze,
          roped_miles,
          fuzzes_spent,
          rubies_spent,
          pumped_reaction,
          rubies_found,
          made_photo,
          made_video,
          game_visits,
          idol,
          inquisitor,
          hatlover,
          partisan,
          collector,
          drop_water_first_turn,
          keymaster,
          crafter,
          craft_legendary,
          superboss_defeated,
          posts_made,
          buy_race,
          coliseum_win,
          with_friend_win,
          coliseum_win_10,
          callback_friends,
          exotic_weapon_master,
          reward_chest,
          buy_skin,

          65535 & s_pvp_played         AS s_pvp_played,
          65535 & s_pvp_won            AS s_pvp_won,
          65535 & s_pvp_draws          AS s_pvp_draws,
          65535 & s_missions_played    AS s_missions_played,
          65535 & s_missions_won       AS s_missions_won,
          65535 & s_wagers_earned      AS s_wagers_earned,
          65535 & s_missions_earned    AS s_missions_earned,
          65535 & s_rubies_found       AS s_rubies_found,
          65535 & s_solo_damage        AS s_solo_damage,
          65535 & s_team_damage        AS s_team_damage,
          65535 & s_solo_damage_taken  AS s_solo_damage_taken,
          65535 & s_team_damage_taken  AS s_team_damage_taken,
          65535 & s_friendly_fire      AS s_friendly_fire,
          65535 & s_sniper_shots       AS s_sniper_shots,
          65535 & s_wind_shots         AS s_wind_shots,
          65535 & s_killed_worms       AS s_killed_worms,
          65535 & s_killed_boxers      AS s_killed_boxers,
          65535 & s_killed_demons      AS s_killed_demons,
          65535 & s_killed_rabbits     AS s_killed_rabbits,
          65535 & s_killed_zombies     AS s_killed_zombies,
          65535 & s_killed_revived     AS s_killed_revived,
          65535 & s_enemies_drowned    AS s_enemies_drowned,
          65535 & s_units_lost         AS s_units_lost,
          65535 & s_collected_medkits  AS s_collected_medkits,
          65535 & s_collected_crates   AS s_collected_crates,
          65535 & s_collected_stars    AS s_collected_stars,
          65535 & s_destroyed_supplies AS s_destroyed_supplies,
          65535 & s_graves_sank        AS s_graves_sank,
          65535 & s_killed_robots      AS s_killed_robots,
          65535 & s_killed_rhinos      AS s_killed_rhinos,
          65535 & s_killed_boars       AS s_killed_boars,
          65535 & s_killed_aliens      AS s_killed_aliens

      FROM achieve.worms_achievements) TO '/home/postgres/truncate/mobile_achievements.copy';

--DROP TABLE achieve.worms_achievements;

CREATE TABLE achieve.worms_achievements
(
    profile_id            CHARACTER VARYING(256)      NOT NULL,
    create_time           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    time_sequence         INTEGER                     NOT NULL DEFAULT 0,
    invested_award_points SMALLINT                    NOT NULL DEFAULT 0,
    bool_achievements     BYTEA,
    update_date           TIMESTAMP WITHOUT TIME ZONE,

    burned_enemies        SMALLINT,
    destroyed_square      SMALLINT,
    drowned_opponents     SMALLINT,
    wager_winner          SMALLINT,
    diplomat              SMALLINT,
    wind_kills            SMALLINT,
    massive_damage        SMALLINT,
    graves_sank           SMALLINT,
    one_hp_win            SMALLINT,
    immobile_kills        SMALLINT,
    zero_looses_victory   SMALLINT,
    flew_miles            SMALLINT,
    double_killls         SMALLINT,
    medkit_kills          SMALLINT,
    gathered_supplies     SMALLINT,
    kamikaze              SMALLINT,
    roped_miles           SMALLINT,
    fuzzes_spent          SMALLINT,
    rubies_spent          SMALLINT,
    pumped_reaction       SMALLINT,
    rubies_found          SMALLINT,
    made_photo            SMALLINT,
    made_video            SMALLINT,
    game_visits           SMALLINT,
    idol                  SMALLINT,
    inquisitor            SMALLINT,
    hatlover              SMALLINT,
    partisan              SMALLINT,
    collector             SMALLINT,
    drop_water_first_turn SMALLINT,
    keymaster             SMALLINT,
    crafter               SMALLINT,
    craft_legendary       SMALLINT,
    superboss_defeated    SMALLINT,
    posts_made            SMALLINT,
    buy_race              SMALLINT,
    coliseum_win          SMALLINT,
    with_friend_win       SMALLINT,
    coliseum_win_10       SMALLINT,
    callback_friends      SMALLINT,
    exotic_weapon_master  SMALLINT,
    reward_chest          SMALLINT,
    buy_skin              SMALLINT,

    s_pvp_played          INTEGER,
    s_pvp_won             INTEGER,
    s_pvp_draws           INTEGER,
    s_missions_played     INTEGER,
    s_missions_won        INTEGER,
    s_wagers_earned       INTEGER,
    s_missions_earned     INTEGER,
    s_rubies_found        INTEGER,
    s_solo_damage         INTEGER,
    s_team_damage         INTEGER,
    s_solo_damage_taken   INTEGER,
    s_team_damage_taken   INTEGER,
    s_friendly_fire       INTEGER,
    s_sniper_shots        INTEGER,
    s_wind_shots          INTEGER,
    s_killed_worms        INTEGER,
    s_killed_boxers       INTEGER,
    s_killed_demons       INTEGER,
    s_killed_rabbits      INTEGER,
    s_killed_zombies      INTEGER,
    s_killed_revived      INTEGER,
    s_enemies_drowned     INTEGER,
    s_units_lost          INTEGER,
    s_collected_medkits   INTEGER,
    s_collected_crates    INTEGER,
    s_collected_stars     INTEGER,
    s_destroyed_supplies  INTEGER,
    s_graves_sank         INTEGER,
    s_killed_robots       INTEGER,
    s_killed_rhinos       INTEGER,
    s_killed_boars        INTEGER,
    s_killed_aliens       INTEGER
)
WITH (
OIDS = FALSE
);
ALTER TABLE achieve.worms_achievements
    OWNER TO smos;

COPY achieve.worms_achievements FROM '/home/postgres/truncate/mobile_achievements.copy';

ALTER TABLE achieve.worms_achievements
    ADD CONSTRAINT worms_achievemens_pkey PRIMARY KEY (profile_id);

