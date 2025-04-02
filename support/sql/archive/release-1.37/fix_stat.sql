copy (SELECT profile_id,
    65535 & s_pvp_played,
    65535 & s_pvp_won,
    65535 & s_pvp_draws,
    65535 & s_missions_played,
    65535 & s_missions_won,
    65535 & s_wagers_earned,
    65535 & s_missions_earned,
    65535 & s_rubies_found,
    65535 & s_solo_damage,
    65535 & s_team_damage,
    65535 & s_solo_damage_taken,
    65535 & s_team_damage_taken,
    65535 & s_friendly_fire,
    65535 & s_sniper_shots,
    65535 & s_wind_shots,
    65535 & s_killed_worms,
    65535 & s_killed_boxers,
    65535 & s_killed_demons,
    65535 & s_killed_rabbits,
    65535 & s_killed_zombies,
    65535 & s_killed_revived,
    65535 & s_enemies_drowned,
    65535 & s_units_lost,
    65535 & s_collected_medkits,
    65535 & s_collected_crates,
    65535 & s_collected_stars,
    65535 & s_destroyed_supplies,
    65535 & s_graves_sank,
    65535 & s_killed_robots,
    65535 & s_killed_rhinos,
    65535 & s_killed_boars
FROM achieve.worms_achievements WHERE
    (s_pvp_played < 0 OR
    s_pvp_won < 0 OR
    s_pvp_draws < 0 OR
    s_missions_played < 0 OR
    s_missions_won < 0 OR
    s_wagers_earned < 0 OR
    s_missions_earned < 0 OR
    s_rubies_found < 0 OR
    s_solo_damage < 0 OR
    s_team_damage < 0 OR
    s_solo_damage_taken < 0 OR
    s_team_damage_taken < 0 OR
    s_friendly_fire < 0 OR
    s_sniper_shots < 0 OR
    s_wind_shots < 0 OR
    s_killed_worms < 0 OR
    s_killed_boxers < 0 OR
    s_killed_demons < 0 OR
    s_killed_rabbits < 0 OR
    s_killed_zombies < 0 OR
    s_killed_revived < 0 OR
    s_enemies_drowned < 0 OR
    s_units_lost < 0 OR
    s_collected_medkits < 0 OR
    s_collected_crates < 0 OR
    s_collected_stars < 0 OR
    s_destroyed_supplies < 0 OR
    s_graves_sank < 0 OR
    s_killed_robots < 0 OR
    s_killed_rhinos < 0 OR
    s_killed_boars < 0)
    and profile_id not like '%w%') TO '/home/postgres/truncate/ok_worms_stat.copy';

CREATE TABLE tmp.worms_stat (
    profile_id CHARACTER VARYING(32) NOT NULL,
    s_pvp_played integer,
    s_pvp_won integer,
    s_pvp_draws integer,
    s_missions_played integer,
    s_missions_won integer,
    s_wagers_earned integer,
    s_missions_earned integer,
    s_rubies_found integer,
    s_solo_damage integer,
    s_team_damage integer,
    s_solo_damage_taken integer,
    s_team_damage_taken integer,
    s_friendly_fire integer,
    s_sniper_shots integer,
    s_wind_shots integer,
    s_killed_worms integer,
    s_killed_boxers integer,
    s_killed_demons integer,
    s_killed_rabbits integer,
    s_killed_zombies integer,
    s_killed_revived integer,
    s_enemies_drowned integer,
    s_units_lost integer,
    s_collected_medkits integer,
    s_collected_crates integer,
    s_collected_stars integer,
    s_destroyed_supplies integer,
    s_graves_sank integer,
    s_killed_robots integer,
    s_killed_rhinos integer,
    s_killed_boars integer
);

ALTER TABLE tmp.worms_stat  OWNER TO smos;

COPY tmp.worms_stat FROM '/home/postgres/truncate/ok_worms_stat.copy';

ALTER TABLE tmp.worms_stat ADD CONSTRAINT worms_stat_pkey PRIMARY KEY(profile_id);