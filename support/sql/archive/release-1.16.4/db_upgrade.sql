ALTER TABLE clan.season_total ADD COLUMN sum_sqrt_rating double precision NOT NULL DEFAULT 0;
ALTER TABLE clan.season_total ADD COLUMN awarded_size integer NOT NULL DEFAULT 0;
