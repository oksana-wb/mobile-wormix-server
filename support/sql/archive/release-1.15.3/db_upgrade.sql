CREATE TABLE stat.profile_stat
(
  profile_id integer NOT NULL,
  flash_version smallint,
  CONSTRAINT profile_stat_pkey PRIMARY KEY (profile_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE stat.profile_stat
  OWNER TO smos;

ALTER TABLE achieve.worms_achievements ADD COLUMN s_killed_robots smallint;
