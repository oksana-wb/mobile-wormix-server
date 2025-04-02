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

CREATE TABLE wormswar.purchase_tx
(
    id                SERIAL,
    tx_init_date      TIMESTAMP WITHOUT TIME ZONE,
    foreign_tx_id     CHARACTER VARYING(48),
    user_profile_id   INTEGER                     NOT NULL,
    social_id         INTEGER                     NOT NULL,
    social_user_id    CHARACTER VARYING(48)       NOT NULL,
    country           CHARACTER VARYING(3)        NOT NULL,
    currency          CHARACTER VARYING(3)        NOT NULL,
    product_category  CHARACTER VARYING(32)       NOT NULL,
    product_code      CHARACTER VARYING(32)       NOT NULL,
    purchase_quantity INTEGER                     NOT NULL,
    purchase_cost     INTEGER                     NOT NULL,
    tx_submit_date    TIMESTAMP WITHOUT TIME ZONE,
    tx_stage          CHARACTER VARYING(16)       NOT NULL,
    tx_stage_date     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    error_code        INTEGER                     NOT NULL,
    error_message     CHARACTER VARYING(256),
    CONSTRAINT purchase_tx_pkey PRIMARY KEY (id)
)
WITH (
OIDS = FALSE
);
ALTER TABLE wormswar.purchase_tx
    OWNER TO smos;

-- Index: public.purchase_tx_init_date_idx

-- DROP INDEX public.purchase_tx_init_date_idx;

CREATE INDEX purchase_tx_init_date_idx
ON wormswar.purchase_tx
USING BTREE
(tx_init_date);

-- Index: public.purchase_tx_user_profile_id_idx

-- DROP INDEX public.purchase_tx_user_profile_id_idx;

CREATE INDEX purchase_tx_user_profile_id_idx
ON wormswar.purchase_tx
USING BTREE
(user_profile_id);
