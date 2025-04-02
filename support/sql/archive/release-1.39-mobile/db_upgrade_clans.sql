-- DROP SCHEMA clan CASCADE;

CREATE SCHEMA clan AUTHORIZATION smos;

CREATE SEQUENCE clan.audit_id_seq
INCREMENT 1
MINVALUE 1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER TABLE clan.audit_id_seq
    OWNER TO smos;

CREATE SEQUENCE clan.clan_id_seq
INCREMENT 1
MINVALUE 1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER TABLE clan.clan_id_seq
    OWNER TO smos;

CREATE OR REPLACE FUNCTION clan.clan_member_delete_trigger()
    RETURNS TRIGGER AS
$BODY$
BEGIN
    IF OLD.season_rating > 0 OR OLD.donation > 0 OR OLD.donation_curr_season > 0 OR OLD.donation_prev_season > 0 OR OLD.cashed_medals > 0
    THEN
        INSERT INTO clan.clan_member_backup_rating
        VALUES (OLD.social_id, OLD.profile_id, OLD.clan_id, OLD.season_rating, now(), OLD.donation, OLD.donation_curr_season, OLD.donation_prev_season, OLD.cashed_medals);
    END IF;
    RETURN OLD;
END;
$BODY$
LANGUAGE plpgsql VOLATILE
COST 100;
ALTER FUNCTION clan.clan_member_delete_trigger()
OWNER TO smos;

CREATE TABLE clan.audit
(
    id           SERIAL                      NOT NULL,
    date         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    clan_id      INTEGER                     NOT NULL,
    action       SMALLINT                    NOT NULL,
    publisher_id INTEGER                     NOT NULL,
    member_id    INTEGER,
    param        INTEGER,
    treas        INTEGER                     NOT NULL,
    CONSTRAINT audit_pkey PRIMARY KEY (id)
)
WITH (
OIDS = FALSE
);
ALTER TABLE clan.audit
    OWNER TO smos;

CREATE INDEX audit_clan_id_idx
    ON clan.audit
    USING BTREE
    (clan_id);

CREATE TABLE clan.clan
(
    id                  SERIAL                   NOT NULL,
    name                CHARACTER VARYING(128)   NOT NULL,
    level               INTEGER                  NOT NULL DEFAULT 1,
    create_date         TIMESTAMP WITH TIME ZONE NOT NULL,
    emblem              BYTEA                    NOT NULL,
    description         CHARACTER VARYING(1024),
    normal_name         CHARACTER VARYING(128)   NOT NULL,
    size                INTEGER                  NOT NULL DEFAULT 0,
    rating              INTEGER                  NOT NULL DEFAULT 0,
    last_update         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    review_state        SMALLINT                 NOT NULL DEFAULT 0,
    join_rating         INTEGER                  NOT NULL DEFAULT (-1),
    season_rating       INTEGER                  NOT NULL DEFAULT 0,
    prev_top_place      INTEGER                  NOT NULL DEFAULT 0,
    closed              BOOLEAN,
    treas               INTEGER,
    medal_price         SMALLINT,
    medal_price_changed BOOLEAN,
    cashed_medals       INTEGER,
    CONSTRAINT clan_pkey PRIMARY KEY (id),
    CONSTRAINT clan_normal_name_key UNIQUE (normal_name),
    CONSTRAINT clan_chk_level CHECK (level > 0 AND level <= 5),
    CONSTRAINT clan_chk_review_state CHECK (review_state >= (-1) AND review_state <= 1)
)
WITH (
OIDS = FALSE
);
ALTER TABLE clan.clan
    OWNER TO smos;

CREATE OR REPLACE FUNCTION clan.clan_trg_last_update()
    RETURNS TRIGGER AS
$BODY$
BEGIN
    NEW.last_update = now();

    RETURN NEW;
END;
$BODY$
LANGUAGE plpgsql VOLATILE
COST 100;
ALTER FUNCTION clan.clan_trg_last_update()
OWNER TO smos;

CREATE TRIGGER clan_trg_last_update
BEFORE INSERT OR UPDATE
    ON clan.clan
FOR EACH ROW
EXECUTE PROCEDURE clan.clan_trg_last_update();

CREATE TABLE clan.clan_member
(
    social_id                     SMALLINT                 NOT NULL,
    profile_id                    INTEGER                  NOT NULL,
    clan_id                       INTEGER                  NOT NULL,
    rank                          INTEGER                  NOT NULL,
    name                          CHARACTER VARYING(128)   NOT NULL,
    social_profile_id             CHARACTER VARYING(48)    NOT NULL,
    join_date                     TIMESTAMP WITH TIME ZONE NOT NULL,
    login_date                    TIMESTAMP WITH TIME ZONE,
    logout_date                   TIMESTAMP WITH TIME ZONE,
    rating                        INTEGER                  NOT NULL DEFAULT 0,
    season_rating                 INTEGER                  NOT NULL DEFAULT 0,
    last_login_time               TIMESTAMP WITHOUT TIME ZONE,
    donation                      INTEGER,
    donation_curr_season          INTEGER,
    donation_prev_season          INTEGER,
    cashed_medals                 INTEGER,
    donation_curr_season_comeback INTEGER,
    donation_prev_season_comeback INTEGER,
    expel_permit                  BOOLEAN                  NOT NULL DEFAULT TRUE,
    mute_mode                     BOOLEAN                  NOT NULL DEFAULT FALSE,
    host_profile_id               INTEGER,
    daily_rating                  CHARACTER VARYING,
    CONSTRAINT clan_member_pkey PRIMARY KEY (profile_id, social_id),
    CONSTRAINT clan_member_clan_id_fkey FOREIGN KEY (clan_id)
    REFERENCES clan.clan (id) MATCH SIMPLE
    ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT clan_member_chk_rank CHECK (rank >= 1 AND rank <= 3)
)
WITH (
OIDS = FALSE
);
ALTER TABLE clan.clan_member
    OWNER TO smos;

CREATE INDEX fki_clan_member_clan_id_fkey
    ON clan.clan_member
    USING BTREE
    (clan_id);

CREATE TABLE clan.clan_member_backup_rating
(
    social_id                     SMALLINT NOT NULL,
    profile_id                    INTEGER  NOT NULL,
    clan_id                       INTEGER  NOT NULL,
    season_rating                 INTEGER  NOT NULL,
    backup_date                   TIMESTAMP WITHOUT TIME ZONE,
    donation                      INTEGER,
    donation_curr_season          INTEGER,
    donation_prev_season          INTEGER,
    cashed_medals                 INTEGER,
    donation_curr_season_comeback INTEGER,
    donation_prev_season_comeback INTEGER,
    CONSTRAINT clan_member_backup_rating_pkey PRIMARY KEY (profile_id, social_id)
)
WITH (
OIDS = FALSE
);
ALTER TABLE clan.clan_member_backup_rating
    OWNER TO smos;

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

CREATE TRIGGER clan_member_before_insert
BEFORE INSERT
    ON clan.clan_member
FOR EACH ROW
EXECUTE PROCEDURE clan.clan_member_insert_trigger();

CREATE TABLE clan.member_award
(
    season_id      INTEGER  NOT NULL,
    social_id      SMALLINT NOT NULL,
    profile_id     INTEGER  NOT NULL,
    clan_id        INTEGER  NOT NULL,
    granted        BOOLEAN  NOT NULL DEFAULT FALSE,
    rating         INTEGER  NOT NULL,
    rank           SMALLINT NOT NULL,
    medal_count    SMALLINT NOT NULL,
    reaction_count SMALLINT NOT NULL,
    weapon_count   SMALLINT,
    CONSTRAINT member_award_pkey PRIMARY KEY (season_id, social_id, profile_id)
)
WITH (
OIDS = FALSE
);
ALTER TABLE clan.member_award
    OWNER TO smos;

CREATE TABLE clan.season
(
    id     INTEGER NOT NULL,
    start  DATE    NOT NULL,
    finish DATE    NOT NULL,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT season_pkey PRIMARY KEY (id)
)
WITH (
OIDS = FALSE
);
ALTER TABLE clan.season
    OWNER TO smos;

CREATE TABLE clan.season_total
(
    season_id       INTEGER          NOT NULL,
    clan_id         INTEGER          NOT NULL,
    season_rating   INTEGER          NOT NULL,
    size            INTEGER          NOT NULL,
    place           INTEGER          NOT NULL,
    sum_sqrt_rating DOUBLE PRECISION NOT NULL DEFAULT 0,
    awarded_size    INTEGER          NOT NULL DEFAULT 0,
    CONSTRAINT season_total_pkey PRIMARY KEY (season_id, clan_id),
    CONSTRAINT season_total_season_id_fkey FOREIGN KEY (season_id)
    REFERENCES clan.season (id) MATCH SIMPLE
    ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
OIDS = FALSE
);
ALTER TABLE clan.season_total
    OWNER TO smos;

INSERT INTO clan.season (id, start, finish) VALUES (1, '2017-01-01', '2017-02-01');

