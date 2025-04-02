ALTER TABLE clan.clan_member ADD COLUMN donation INTEGER;
ALTER TABLE clan.clan_member ADD COLUMN donation_curr_season INTEGER;
ALTER TABLE clan.clan_member ADD COLUMN donation_prev_season INTEGER;
ALTER TABLE clan.clan_member ADD COLUMN cashed_medals INTEGER;

ALTER TABLE clan.clan ADD COLUMN medal_price smallint;
ALTER TABLE clan.clan ADD COLUMN cashed_medals integer;

ALTER TABLE clan.clan_member_backup_rating ADD COLUMN donation INTEGER;
ALTER TABLE clan.clan_member_backup_rating ADD COLUMN donation_curr_season INTEGER;
ALTER TABLE clan.clan_member_backup_rating ADD COLUMN donation_prev_season INTEGER;
ALTER TABLE clan.clan_member_backup_rating ADD COLUMN cashed_medals INTEGER;

DROP TRIGGER clan_member_before_delete ON clan.clan_member CASCADE;

-- clan.clan_member_insert_trigger() --
CREATE OR REPLACE FUNCTION clan.clan_member_insert_trigger()
    RETURNS TRIGGER AS
    $BODY$
    DECLARE
        save RECORD;
    BEGIN
        SELECT
            season_rating,
            donation,
            donation_curr_season,
            donation_prev_season,
            cashed_medals
        FROM clan.clan_member_backup_rating
        INTO save
        WHERE social_id = NEW.social_id AND profile_id = NEW.profile_id AND clan_id = NEW.clan_id;

        NEW.season_rating := coalesce(save.season_rating, 0);
        NEW.donation := coalesce(save.donation, 0);
        NEW.donation_curr_season := coalesce(save.donation_curr_season, 0);
        NEW.donation_prev_season := coalesce(save.donation_prev_season, 0);
        NEW.cashed_medals := coalesce(save.cashed_medals, 0);

        DELETE FROM clan.clan_member_backup_rating
        WHERE social_id = NEW.social_id AND profile_id = NEW.profile_id;

        RETURN NEW;
    END;
    $BODY$
LANGUAGE plpgsql VOLATILE
COST 100;
ALTER FUNCTION clan.clan_member_insert_trigger()
OWNER TO smos;

-- clan.audit --
CREATE TABLE clan.audit
(
  id serial NOT NULL,
  date timestamp without time zone NOT NULL DEFAULT now(),
  clan_id integer NOT NULL,
  action smallint NOT NULL,
  publisher_id integer NOT NULL,
  member_id integer,
  param integer,
  treas integer NOT NULL,
  CONSTRAINT audit_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE clan.audit
  OWNER TO smos;

CREATE INDEX audit_clan_id_idx
  ON clan.audit
  USING btree
  (clan_id);


ALTER TABLE wormswar.app_params ADD COLUMN vk_auth_secret character varying(128) not null default '';
--vkontakte only
update wormswar.app_params set vk_auth_secret='kjWSsTKnUXtOcgRmWJMPhpoF';



