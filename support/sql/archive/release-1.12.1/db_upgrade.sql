CREATE TABLE clan.clan_member_backup_rating
(
  social_id smallint NOT NULL,
  profile_id integer NOT NULL,
  clan_id integer NOT NULL,
  season_rating integer NOT NULL,
  backup_date timestamp without time zone,
  CONSTRAINT clan_member_backup_rating_pkey PRIMARY KEY (profile_id, social_id),
  CONSTRAINT clan_member_backup_rating_clan_id_fkey FOREIGN KEY (clan_id)
      REFERENCES clan.clan (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE clan.clan_member_backup_rating
  OWNER TO smos;


CREATE OR REPLACE FUNCTION clan.clan_member_delete_trigger()
  RETURNS trigger AS
$BODY$
BEGIN
  if OLD.season_rating > 0 THEN
      BEGIN
        INSERT INTO clan.clan_member_backup_rating VALUES (OLD.social_id, OLD.profile_id, OLD.clan_id, OLD.season_rating, now());
      EXCEPTION
        WHEN foreign_key_violation THEN
          -- do nothing
      END;
  END IF;
  RETURN OLD;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION clan.clan_member_delete_trigger()
  OWNER TO smos;


CREATE OR REPLACE FUNCTION clan.clan_member_insert_trigger()
  RETURNS trigger AS
$BODY$
BEGIN
  select coalesce((select season_rating from clan.clan_member_backup_rating where social_id = NEW.social_id and profile_id = NEW.profile_id and clan_id = NEW.clan_id), 0) INTO NEW.season_rating;

  delete from clan.clan_member_backup_rating where social_id = NEW.social_id and profile_id = NEW.profile_id;

  RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION clan.clan_member_insert_trigger()
  OWNER TO smos;


CREATE TRIGGER clan_member_before_insert
  BEFORE INSERT
  ON clan.clan_member
  FOR EACH ROW
  EXECUTE PROCEDURE clan.clan_member_insert_trigger();


CREATE TRIGGER clan_member_before_delete
  BEFORE DELETE
  ON clan.clan_member
  FOR EACH ROW
  EXECUTE PROCEDURE clan.clan_member_delete_trigger();
