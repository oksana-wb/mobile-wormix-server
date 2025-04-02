ALTER TABLE achieve.worms_achievements ADD COLUMN s_killed_robots smallint;

CREATE TABLE wormswar.backpack_conf
(
  id serial NOT NULL,
  profile_id bigint NOT NULL,
  config_id smallint DEFAULT 0,
  config bytea,
  CONSTRAINT backpack_conf_pkey PRIMARY KEY (id),
  CONSTRAINT backpack_conf_profile_id_fkey FOREIGN KEY (profile_id)
      REFERENCES wormswar.user_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.backpack_conf
  OWNER TO smos;