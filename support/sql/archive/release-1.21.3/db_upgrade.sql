ALTER TABLE clan.clan_member ADD COLUMN donation_curr_season_comeback INTEGER;
ALTER TABLE clan.clan_member ADD COLUMN donation_prev_season_comeback INTEGER;

ALTER TABLE clan.clan_member_backup_rating ADD COLUMN donation_curr_season_comeback INTEGER;
ALTER TABLE clan.clan_member_backup_rating ADD COLUMN donation_prev_season_comeback INTEGER;

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
            donation_curr_season_comeback,
            donation_prev_season_comeback,
            cashed_medals
        FROM clan.clan_member_backup_rating
        INTO save
        WHERE social_id = NEW.social_id AND profile_id = NEW.profile_id AND clan_id = NEW.clan_id;

        NEW.season_rating := coalesce(save.season_rating, 0);
        NEW.donation := coalesce(save.donation, 0);
        NEW.donation_curr_season := coalesce(save.donation_curr_season, 0);
        NEW.donation_prev_season := coalesce(save.donation_prev_season, 0);
        NEW.donation_curr_season_comeback := coalesce(save.donation_curr_season_comeback, 0);
        NEW.donation_prev_season_comeback := coalesce(save.donation_prev_season_comeback, 0);
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

update clan.clan_member set donation_curr_season_comeback = donation_curr_season, donation_prev_season_comeback = donation_prev_season;

update clan.clan_member_backup_rating set donation_curr_season_comeback = donation_curr_season, donation_prev_season_comeback = donation_prev_season;



