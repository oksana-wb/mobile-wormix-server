ALTER TABLE achieve.worms_achievements ADD COLUMN bool_achievements bytea;

ALTER TABLE wormswar.reagents ADD COLUMN medal integer;
ALTER TABLE wormswar.reagents ADD COLUMN book integer;

ALTER TABLE clan.clan ADD COLUMN season_rating integer NOT NULL DEFAULT 0;
ALTER TABLE clan.clan_member ADD COLUMN season_rating integer NOT NULL DEFAULT 0;

-- статистика, когда и откуда пришел игрок
CREATE TABLE wormswar.creation_date
(
  id bigint NOT NULL,
  creation_date timestamp without time zone NOT NULL DEFAULT now(),
  CONSTRAINT creation_date_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.creation_date
  OWNER TO smos;

CREATE OR REPLACE FUNCTION wormswar.user_profile_insert_trigger()
  RETURNS trigger AS
$BODY$
BEGIN
    INSERT INTO wormswar.creation_date VALUES (NEW.id, now());
    RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION wormswar.user_profile_insert_trigger()
  OWNER TO smos;

CREATE TRIGGER user_profile_before_insert
  BEFORE INSERT
  ON wormswar.user_profile
  FOR EACH ROW
  EXECUTE PROCEDURE wormswar.user_profile_insert_trigger();

ALTER TABLE wormswar.creation_date ADD COLUMN referrer character varying(256);

