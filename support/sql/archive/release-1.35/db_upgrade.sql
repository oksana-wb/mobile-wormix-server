CREATE TABLE wormswar.season_total
(
  season date NOT NULL,
  profile_id integer NOT NULL,
  rank smallint NOT NULL,
  rank_points integer NOT NULL,
  top_place smallint NOT NULL DEFAULT 0,
  granted boolean NOT NULL DEFAULT false,
  CONSTRAINT season_total_pkey PRIMARY KEY (season, profile_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.season_total
  OWNER TO smos;

ALTER TABLE wormswar.backpack_conf DROP COLUMN id;
ALTER TABLE wormswar.backpack_conf ADD PRIMARY KEY (profile_id);
ALTER TABLE wormswar.backpack_conf DROP COLUMN config_id;
ALTER TABLE wormswar.backpack_conf ADD COLUMN config1 bytea;
ALTER TABLE wormswar.backpack_conf ADD COLUMN config2 bytea;
ALTER TABLE wormswar.backpack_conf ADD COLUMN active_config smallint NOT NULL default 1;
ALTER TABLE wormswar.backpack_conf ADD COLUMN seasons_best_rank bytea NOT NULL DEFAULT ''::bytea;

truncate wormswar.quest_progress;


ALTER TABLE  achieve.worms_achievements ADD COLUMN reward_chest smallint;
