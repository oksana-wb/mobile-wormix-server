
COPY (select * from achieve.worms_achievements) TO '/home/postgresql/truncate/worms_achievements.copy'; -- 04:21

truncate achieve.worms_achievements;

ALTER TABLE achieve.worms_achievements ALTER COLUMN s_pvp_played TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_pvp_won TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_pvp_draws TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_missions_played TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_missions_won TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_wagers_earned TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_missions_earned TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_rubies_found TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_solo_damage TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_team_damage TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_solo_damage_taken TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_team_damage_taken TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_friendly_fire TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_sniper_shots TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_wind_shots TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_killed_worms TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_killed_boxers TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_killed_demons TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_killed_rabbits TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_killed_zombies TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_killed_revived TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_enemies_drowned TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_units_lost TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_collected_medkits TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_collected_crates TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_collected_stars TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_destroyed_supplies TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_graves_sank TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_killed_robots TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_killed_rhinos TYPE integer;
ALTER TABLE achieve.worms_achievements ALTER COLUMN s_killed_boars TYPE integer;

ALTER TABLE achieve.worms_achievements DROP CONSTRAINT worms_achievemens_pkey;

COPY achieve.worms_achievements FROM '/home/postgresql/truncate/worms_achievements.copy'; -- 03:57

ALTER TABLE achieve.worms_achievements ADD CONSTRAINT worms_achievemens_pkey PRIMARY KEY(profile_id); -- 04:01

ALTER TABLE achieve.worms_achievements ADD COLUMN buy_skin smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN s_killed_aliens integer;

ALTER TABLE achieve.worms_achievements ADD COLUMN update_date timestamp without time zone;

--== Реагенты ==--
ALTER TABLE wormswar.reagents RENAME book TO prize_key;
ALTER TABLE wormswar.reagents ADD COLUMN mutagen integer;
