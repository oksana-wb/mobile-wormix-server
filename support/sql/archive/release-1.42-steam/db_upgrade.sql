DROP TABLE wormswar.cheater_statistic;

CREATE TABLE wormswar.cheater_statistic
(
  id serial,
  date timestamp without time zone NOT NULL,
  profile_id bigint NOT NULL,
  action_type smallint NOT NULL,
  action_param character varying(64) NOT NULL,
  count integer NOT NULL,
  note character varying(1024) NOT NULL,
  CONSTRAINT cheater_statistic_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.cheater_statistic
  OWNER TO smos;

DROP SEQUENCE admin_profile_sequence;
DROP SEQUENCE backpack_sequence;
DROP SEQUENCE black_login_statistic_sequence;
DROP SEQUENCE cheater_statistic_id_sequence;
DROP SEQUENCE experience_sequence;
DROP SEQUENCE friend_invates_sequence;
DROP SEQUENCE pvp_battle_statistic_sequence;
DROP SEQUENCE stuff_sequence;
DROP SEQUENCE weapon_sequence;
DROP SEQUENCE worm_group_sequence;