CREATE TABLE wormswar.user_profile_meta (
  profile_id INTEGER NOT NULL PRIMARY KEY,
  last_comebacked_time TIMESTAMP WITHOUT TIME ZONE,
  CONSTRAINT user_profile_meta_id_fkey FOREIGN KEY (profile_id)
      REFERENCES wormswar.user_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
OIDS=FALSE
);
ALTER TABLE wormswar.user_profile_meta
  OWNER TO smos;

CREATE TABLE wormswar.ranks
(
  profile_id integer NOT NULL,
  rank_points integer NOT NULL,
  best_rank smallint NOT NULL,
  CONSTRAINT ranks_pkey PRIMARY KEY (profile_id),
  CONSTRAINT ranks_profile_id_fkey FOREIGN KEY (profile_id)
      REFERENCES wormswar.user_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.ranks
  OWNER TO smos;

CREATE INDEX ranks_rank_points_idx
  ON wormswar.ranks
  USING btree
  (rank_points DESC);

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

ALTER TABLE clan.clan_member ADD COLUMN host_profile_id INTEGER;
ALTER TABLE clan.clan_member ADD COLUMN daily_rating CHARACTER VARYING;

--== Реагенты ==--
ALTER TABLE wormswar.reagents RENAME book TO prize_key;
ALTER TABLE wormswar.reagents ADD COLUMN mutagen integer;