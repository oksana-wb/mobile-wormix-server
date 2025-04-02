CREATE SCHEMA clan
  AUTHORIZATION smos;

CREATE OR REPLACE FUNCTION clan.clan_trg_last_update()
  RETURNS trigger AS
$BODY$
    BEGIN
        NEW.last_update=now();

        RETURN NEW;
    END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

ALTER FUNCTION clan.clan_trg_last_update()
  OWNER TO smos;

CREATE TABLE clan.clan
(
  id serial NOT NULL,
  name character varying(128) NOT NULL,
  level integer NOT NULL DEFAULT 1,
  create_date timestamp with time zone NOT NULL,
  emblem bytea NOT NULL DEFAULT '\000\000\000\000'::bytea,
  description character varying(1024),
  normal_name character varying(128) NOT NULL,
  size integer NOT NULL DEFAULT 0,
  rating integer NOT NULL DEFAULT 0,
  last_update timestamp with time zone NOT NULL DEFAULT now(),
  review_state smallint NOT NULL DEFAULT 0,
  join_rating integer NOT NULL DEFAULT (-1),
  CONSTRAINT clan_pkey PRIMARY KEY (id),
  CONSTRAINT clan_normal_name_key UNIQUE (normal_name),
  CONSTRAINT clan_chk_level CHECK (level > 0 AND level <= 5),
  CONSTRAINT clan_chk_review_state CHECK (review_state >= (-1) AND review_state <= 1)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE clan.clan
  OWNER TO smos;

CREATE TRIGGER clan_trg_last_update
  BEFORE INSERT OR UPDATE
  ON clan.clan
  FOR EACH ROW
  EXECUTE PROCEDURE clan.clan_trg_last_update();

CREATE TABLE clan.clan_member
(
  social_id smallint NOT NULL,
  profile_id integer NOT NULL,
  clan_id integer NOT NULL,
  rank integer NOT NULL,
  name character varying(128) NOT NULL,
  social_profile_id character varying(48) NOT NULL,
  join_date timestamp with time zone NOT NULL,
  login_date timestamp with time zone,
  logout_date timestamp with time zone,
  rating integer NOT NULL DEFAULT 0,
  CONSTRAINT clan_member_pkey PRIMARY KEY (profile_id, social_id),
  CONSTRAINT clan_member_clan_id_fkey FOREIGN KEY (clan_id)
      REFERENCES clan.clan (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT clan_member_chk_rank CHECK (rank >= 1 AND rank <= 3)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE clan.clan_member
  OWNER TO smos;

CREATE INDEX fki_clan_member_clan_id_fkey
  ON clan.clan_member
  USING btree
  (clan_id);

