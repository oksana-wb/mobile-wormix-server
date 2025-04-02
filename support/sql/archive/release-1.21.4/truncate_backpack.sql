--pgsql -a -f truncate_backpack wormswar

select now();
delete FROM wormswar.backpack_item WHERE profile_id > 1000000000 or weapon_count > 30000;

select now();
COPY (SELECT profile_id::integer, min(weapon_count)::smallint, weapon_id::smallint FROM wormswar.backpack_item_0 group by 1, 3) TO '/home/postgres/truncate/live_backpack_item_0.copy';
select now();
COPY (SELECT profile_id::integer, min(weapon_count)::smallint, weapon_id::smallint FROM wormswar.backpack_item_1 group by 1, 3) TO '/home/postgres/truncate/live_backpack_item_1.copy';
select now();
COPY (SELECT profile_id::integer, min(weapon_count)::smallint, weapon_id::smallint FROM wormswar.backpack_item_2 group by 1, 3) TO '/home/postgres/truncate/live_backpack_item_2.copy';
select now();
COPY (SELECT profile_id::integer, min(weapon_count)::smallint, weapon_id::smallint FROM wormswar.backpack_item_3 group by 1, 3) TO '/home/postgres/truncate/live_backpack_item_3.copy';

truncate wormswar.backpack_item_0;
truncate wormswar.backpack_item_1;
truncate wormswar.backpack_item_2;
truncate wormswar.backpack_item_3;

ALTER TABLE wormswar.backpack_item DROP COLUMN id;
ALTER TABLE wormswar.backpack_item ALTER COLUMN profile_id TYPE integer;
ALTER TABLE wormswar.backpack_item ALTER COLUMN weapon_count TYPE smallint;
ALTER TABLE wormswar.backpack_item ALTER COLUMN weapon_id TYPE smallint;

ALTER TABLE wormswar.backpack_item_0 DROP COLUMN id;
ALTER TABLE wormswar.backpack_item_1 DROP COLUMN id;
ALTER TABLE wormswar.backpack_item_2 DROP COLUMN id;
ALTER TABLE wormswar.backpack_item_3 DROP COLUMN id;

ALTER TABLE wormswar.backpack_item_0 DROP CONSTRAINT backpack_item_fkey_0;
ALTER TABLE wormswar.backpack_item_1 DROP CONSTRAINT backpack_item_fkey_1;
ALTER TABLE wormswar.backpack_item_2 DROP CONSTRAINT backpack_item_fkey_2;
ALTER TABLE wormswar.backpack_item_3 DROP CONSTRAINT backpack_item_fkey_3;

DROP INDEX wormswar.backpack_profile_id_index_0;
DROP INDEX wormswar.backpack_profile_id_index_1;
DROP INDEX wormswar.backpack_profile_id_index_2;
DROP INDEX wormswar.backpack_profile_id_index_3;

select now();
COPY wormswar.backpack_item_0 FROM '/home/postgres/truncate/live_backpack_item_0.copy';
select now();
COPY wormswar.backpack_item_1 FROM '/home/postgres/truncate/live_backpack_item_1.copy';
select now();
COPY wormswar.backpack_item_2 FROM '/home/postgres/truncate/live_backpack_item_2.copy';
select now();
COPY wormswar.backpack_item_3 FROM '/home/postgres/truncate/live_backpack_item_3.copy';

select now();
ALTER TABLE wormswar.backpack_item_0 ADD PRIMARY KEY (profile_id, weapon_id);
ALTER TABLE wormswar.backpack_item_1 ADD PRIMARY KEY (profile_id, weapon_id);
ALTER TABLE wormswar.backpack_item_2 ADD PRIMARY KEY (profile_id, weapon_id);
ALTER TABLE wormswar.backpack_item_3 ADD PRIMARY KEY (profile_id, weapon_id);

select now();
ALTER TABLE wormswar.backpack_item_0  ADD CONSTRAINT backpack_item_fkey_0 FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.backpack_item_1  ADD CONSTRAINT backpack_item_fkey_1 FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.backpack_item_2  ADD CONSTRAINT backpack_item_fkey_2 FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.backpack_item_3  ADD CONSTRAINT backpack_item_fkey_3 FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;

DROP FUNCTION wormswar.backpack_item_insert(bigint, integer, integer);

CREATE OR REPLACE FUNCTION wormswar.backpack_item_insert(profileid integer, weaponcount smallint, weaponid smallint)
  RETURNS integer AS
$BODY$
BEGIN
  IF ($1 % 4 = 0) THEN
    INSERT INTO wormswar.backpack_item_0 VALUES ($1, $2, $3);
  ELSEIF ($1 % 4 = 1) THEN
    INSERT INTO wormswar.backpack_item_1 VALUES ($1, $2, $3);
  ELSEIF ($1 % 4 = 2) THEN
    INSERT INTO wormswar.backpack_item_2 VALUES ($1, $2, $3);
  ELSE
    INSERT INTO wormswar.backpack_item_3 VALUES ($1, $2, $3);
  END IF;
  RETURN 1;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION wormswar.backpack_item_insert(integer, smallint, smallint)
  OWNER TO smos;

--select * from pg_stat_activity where datname = 'wormswar'