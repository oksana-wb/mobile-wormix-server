--0IAJKMf9QMZF
CREATE ROLE promo_raid LOGIN ENCRYPTED PASSWORD 'md5cee0a838789dff9acbbf969aafc5515a' NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;

--mmZOVm2I6NES
CREATE ROLE promo_wormix LOGIN ENCRYPTED PASSWORD 'md53e075483f3a0ec32d5927b67011cf9a1' NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;

CREATE DATABASE promo WITH OWNER = postgres;
CREATE SCHEMA wormix AUTHORIZATION promo_wormix;

CREATE TABLE wormix.registered_keys
(
    key           CHARACTER VARYING(16)       NOT NULL,
    create_date   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    action        SMALLINT                    NOT NULL DEFAULT 0,
    uid           BIGINT                      NOT NULL,
    activate_date TIMESTAMP WITHOUT TIME ZONE,
    profile_id    INTEGER,
    social_net    SMALLINT,
    CONSTRAINT registered_keys_pkey PRIMARY KEY (key),
    CONSTRAINT registered_keys_action_profile_id_social_net_key UNIQUE (action, profile_id, social_net)
)
WITH (
OIDS = FALSE
);
ALTER TABLE wormix.registered_keys
    OWNER TO promo_wormix;

GRANT USAGE ON SCHEMA wormix TO promo_raid;
GRANT SELECT ON wormix.registered_keys TO promo_raid;
GRANT INSERT ON wormix.registered_keys TO promo_raid;

GRANT EXECUTE ON FUNCTION wormix.get_promo_key(SMALLINT, BIGINT) TO promo_raid;
