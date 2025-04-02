delete from wormswar.mercenaries where num = 0;

ALTER TABLE wormswar.mercenaries ADD COLUMN total_win integer;
ALTER TABLE wormswar.mercenaries ADD COLUMN total_defeat integer;
ALTER TABLE wormswar.mercenaries ADD COLUMN total_draw integer;