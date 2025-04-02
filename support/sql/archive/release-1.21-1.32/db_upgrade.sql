ALTER TABLE achieve.worms_achievements ADD COLUMN buy_race smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN coliseum_win smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN coliseum_win_10 smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN with_friend_win smallint;

ALTER TABLE wormswar.app_params ADD COLUMN vk_auth_secret character varying(128) not null default '';

ALTER TABLE wormswar.worm_groups
ADD COLUMN team_member_5 INTEGER,
ADD COLUMN team_member_6 INTEGER,
ADD COLUMN team_member_7 INTEGER,
ADD COLUMN team_member_meta_5 BYTEA,
ADD COLUMN team_member_meta_6 BYTEA,
ADD COLUMN team_member_meta_7 BYTEA,
ADD COLUMN extra_group_slots_count SMALLINT,
ADD COLUMN team_member_names CHARACTER VARYING;

ALTER TABLE stat.audit_admin_action ALTER COLUMN note TYPE CHARACTER VARYING;

CREATE TABLE wormswar.mercenaries
(
    profile_id   INTEGER  NOT NULL,
    open         BOOLEAN  NOT NULL DEFAULT FALSE,
    mercenary_01 SMALLINT NOT NULL DEFAULT 0,
    mercenary_02 SMALLINT NOT NULL DEFAULT 0,
    mercenary_03 SMALLINT NOT NULL DEFAULT 0,
    start_series TIMESTAMP WITHOUT TIME ZONE,
    win          SMALLINT NOT NULL DEFAULT 0,
    defeat       SMALLINT NOT NULL DEFAULT 0,
    draw         SMALLINT NOT NULL DEFAULT 0,
    num          INTEGER  NOT NULL DEFAULT 0,
    total_win    INTEGER  NOT NULL DEFAULT 0,
    total_defeat INTEGER  NOT NULL DEFAULT 0,
    total_draw   INTEGER  NOT NULL DEFAULT 0,
    CONSTRAINT mercenaries_pkey PRIMARY KEY (profile_id)
)
WITH (
OIDS =FALSE
);
ALTER TABLE wormswar.mercenaries
OWNER TO smos;

ALTER TABLE achieve.worms_achievements ADD COLUMN craft_legendary smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN superboss_defeated smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN s_killed_rhinos smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN s_killed_boars smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN posts_made smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN callback_friends smallint;

DELETE FROM wormswar.store;

CREATE TABLE wormswar.coliseum
(
    profile_id   INTEGER                     NOT NULL,
    open         BOOLEAN,
    num          INTEGER,
    data         BYTEA,
    create_date  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    start_series TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT coliseum_pkey PRIMARY KEY (profile_id)
)
WITH (
OIDS =FALSE
);
ALTER TABLE wormswar.coliseum
OWNER TO smos;

CREATE TABLE wormswar.bundles
(
  id serial NOT NULL,
  code character varying(255) NOT NULL,
  discount integer NOT NULL,
  votes numeric NOT NULL,
  items character varying(255) DEFAULT ''::character varying,
  race integer NOT NULL DEFAULT (-1),
  disabled boolean NOT NULL DEFAULT false,
  start timestamp without time zone,
  finish timestamp without time zone,
  create_date timestamp without time zone NOT NULL DEFAULT now(),
  update_date timestamp without time zone,
  CONSTRAINT bundles_pkey PRIMARY KEY (id),
  CONSTRAINT bundles_code_key UNIQUE (code)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.bundles
  OWNER TO smos;

ALTER TABLE clan.clan_member ADD COLUMN expel_permit boolean NOT NULL DEFAULT true;
ALTER TABLE clan.clan_member ADD COLUMN mute_mode boolean NOT NULL DEFAULT false;

CREATE TABLE wormswar.quest_progress
(
  profile_id bigint NOT NULL,
  q1 character varying,
  q2 character varying,
  CONSTRAINT quest_progress_pkey PRIMARY KEY (profile_id),
  CONSTRAINT quest_progress_profile_id_fkey FOREIGN KEY (profile_id)
      REFERENCES wormswar.user_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.quest_progress
  OWNER TO smos;

