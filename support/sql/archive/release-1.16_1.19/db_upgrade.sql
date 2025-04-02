ALTER TABLE achieve.worms_achievements ADD COLUMN s_killed_robots smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN drop_water_first_turn smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN keymaster smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN crafter smallint;

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

CREATE TABLE wormswar.referral_link
(
  id serial NOT NULL,
  token character varying(16) NOT NULL,
  start timestamp without time zone NOT NULL,
  finish timestamp without time zone,
  "limit" integer NOT NULL DEFAULT 0,
  ruby integer NOT NULL DEFAULT 0,
  fuzy integer NOT NULL DEFAULT 0,
  battles integer NOT NULL DEFAULT 0,
  reaction integer NOT NULL DEFAULT 0,
  reagents character varying(256) NOT NULL DEFAULT ''::character varying,
  weapons character varying(256) NOT NULL DEFAULT ''::character varying,
  visitors integer NOT NULL DEFAULT 0,
  CONSTRAINT referral_link_pkey PRIMARY KEY (id),
  CONSTRAINT referral_link_code_key UNIQUE (token)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.referral_link
  OWNER TO smos;