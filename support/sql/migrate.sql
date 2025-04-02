ALTER TABLE wormswar.app_params ADD COLUMN level_min integer NOT NULL DEFAULT 1;
ALTER TABLE wormswar.app_params ADD COLUMN level_max integer NOT NULL DEFAULT 99;