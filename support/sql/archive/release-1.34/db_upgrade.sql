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

