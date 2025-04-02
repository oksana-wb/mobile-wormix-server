DELETE FROM wormswar.store;

ALTER TABLE clan.clan ADD COLUMN closed BOOLEAN;
ALTER TABLE clan.clan ADD COLUMN treas INTEGER;

ALTER TABLE clan.clan_member ADD COLUMN last_login_time TIMESTAMP WITHOUT TIME ZONE;