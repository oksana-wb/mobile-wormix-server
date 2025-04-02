--pgsql -a -f truncate_backpack.sql wormswar

select now();

COPY (SELECT id, profile_id, weapon_count, case
    when weapon_id = 6 then 7
    when weapon_id = 8 then 9
    when weapon_id = 13 then 14
    when weapon_id = 15 then 24
    when weapon_id = 20 then 19
    when weapon_id = 22 then 26
    else weapon_id
end as weapon_id FROM wormswar.backpack_item_0 WHERE weapon_id != 21 and weapon_id != 17 and weapon_id != 18 and weapon_id != 25) TO '/home/postgres/truncate/live_backpack_item_0.copy';
COPY (SELECT id, profile_id, weapon_count, case
    when weapon_id = 6 then 7
    when weapon_id = 8 then 9
    when weapon_id = 13 then 14
    when weapon_id = 15 then 24
    when weapon_id = 20 then 19
    when weapon_id = 22 then 26
    else weapon_id
end as weapon_id FROM wormswar.backpack_item_1 WHERE weapon_id != 21 and weapon_id != 17 and weapon_id != 18 and weapon_id != 25) TO '/home/postgres/truncate/live_backpack_item_1.copy';
COPY (SELECT id, profile_id, weapon_count, case
    when weapon_id = 6 then 7
    when weapon_id = 8 then 9
    when weapon_id = 13 then 14
    when weapon_id = 15 then 24
    when weapon_id = 20 then 19
    when weapon_id = 22 then 26
    else weapon_id
end as weapon_id FROM wormswar.backpack_item_2 WHERE weapon_id != 21 and weapon_id != 17 and weapon_id != 18 and weapon_id != 25) TO '/home/postgres/truncate/live_backpack_item_2.copy';
COPY (SELECT id, profile_id, weapon_count, case
    when weapon_id = 6 then 7
    when weapon_id = 8 then 9
    when weapon_id = 13 then 14
    when weapon_id = 15 then 24
    when weapon_id = 20 then 19
    when weapon_id = 22 then 26
    else weapon_id
end as weapon_id FROM wormswar.backpack_item_3 WHERE weapon_id != 21 and weapon_id != 17 and weapon_id != 18 and weapon_id != 25) TO '/home/postgres/truncate/live_backpack_item_3.copy';

truncate wormswar.backpack_item_0;
truncate wormswar.backpack_item_1;
truncate wormswar.backpack_item_2;
truncate wormswar.backpack_item_3;

--## удаляем зависимости ##--
--ALTER TABLE wormswar.backpack_item DROP CONSTRAINT backpack_item_profile_id_fkey;

-- wormswar.backpack_item_0
ALTER TABLE wormswar.backpack_item_0 DROP CONSTRAINT backpack_item_pkey_0;
ALTER TABLE wormswar.backpack_item_0 DROP CONSTRAINT backpack_item_fkey_0;
ALTER TABLE wormswar.backpack_item_0 DROP CONSTRAINT backpack_item_0_profile_id_check;
DROP INDEX wormswar.backpack_profile_id_index_0;
-- wormswar.backpack_item_1
ALTER TABLE wormswar.backpack_item_1 DROP CONSTRAINT backpack_item_pkey_1;
ALTER TABLE wormswar.backpack_item_1 DROP CONSTRAINT backpack_item_fkey_1;
ALTER TABLE wormswar.backpack_item_1 DROP CONSTRAINT backpack_item_1_profile_id_check;
DROP INDEX wormswar.backpack_profile_id_index_1;
-- wormswar.backpack_item_2
ALTER TABLE wormswar.backpack_item_2 DROP CONSTRAINT backpack_item_pkey_2;
ALTER TABLE wormswar.backpack_item_2 DROP CONSTRAINT backpack_item_fkey_2;
ALTER TABLE wormswar.backpack_item_2 DROP CONSTRAINT backpack_item_2_profile_id_check;
DROP INDEX wormswar.backpack_profile_id_index_2;
-- wormswar.backpack_item_3
ALTER TABLE wormswar.backpack_item_3 DROP CONSTRAINT backpack_item_pkey_3;
ALTER TABLE wormswar.backpack_item_3 DROP CONSTRAINT backpack_item_fkey_3;
ALTER TABLE wormswar.backpack_item_3 DROP CONSTRAINT backpack_item_3_profile_id_check;
DROP INDEX wormswar.backpack_profile_id_index_3;

COPY wormswar.backpack_item_0 FROM '/home/postgres/truncate/live_backpack_item_0.copy';
COPY wormswar.backpack_item_1 FROM '/home/postgres/truncate/live_backpack_item_1.copy';
COPY wormswar.backpack_item_2 FROM '/home/postgres/truncate/live_backpack_item_2.copy';
COPY wormswar.backpack_item_3 FROM '/home/postgres/truncate/live_backpack_item_3.copy';

-- ## восстанавливаем зависимости ##--
ALTER TABLE wormswar.backpack_item ADD CONSTRAINT backpack_item_profile_id_fkey FOREIGN KEY (profile_id)  REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
-- wormswar.backpack_item_0
ALTER TABLE wormswar.backpack_item_0 ADD CONSTRAINT backpack_item_pkey_0 PRIMARY KEY(id);
ALTER TABLE wormswar.backpack_item_0 ADD CONSTRAINT backpack_item_fkey_0 FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.backpack_item_0 ADD CONSTRAINT backpack_item_0_profile_id_check CHECK ((profile_id % 4::bigint) = 0);
CREATE INDEX backpack_profile_id_index_0 ON wormswar.backpack_item_0 USING btree (profile_id);
ALTER TABLE wormswar.backpack_item_0 CLUSTER ON backpack_profile_id_index_0;
-- wormswar.backpack_item_1
ALTER TABLE wormswar.backpack_item_1 ADD CONSTRAINT backpack_item_pkey_1 PRIMARY KEY(id);
ALTER TABLE wormswar.backpack_item_1 ADD CONSTRAINT backpack_item_fkey_1 FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.backpack_item_1 ADD CONSTRAINT backpack_item_1_profile_id_check CHECK ((profile_id % 4::bigint) = 1);
CREATE INDEX backpack_profile_id_index_1 ON wormswar.backpack_item_1 USING btree (profile_id);
ALTER TABLE wormswar.backpack_item_1 CLUSTER ON backpack_profile_id_index_1;
-- wormswar.backpack_item_2
ALTER TABLE wormswar.backpack_item_2 ADD CONSTRAINT backpack_item_pkey_2 PRIMARY KEY(id);
ALTER TABLE wormswar.backpack_item_2 ADD CONSTRAINT backpack_item_fkey_2 FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.backpack_item_2 ADD CONSTRAINT backpack_item_2_profile_id_check CHECK ((profile_id % 4::bigint) = 2);
CREATE INDEX backpack_profile_id_index_2 ON wormswar.backpack_item_2 USING btree (profile_id);
ALTER TABLE wormswar.backpack_item_2 CLUSTER ON backpack_profile_id_index_2;
-- wormswar.backpack_item_3
ALTER TABLE wormswar.backpack_item_3 ADD CONSTRAINT backpack_item_pkey_3 PRIMARY KEY(id);
ALTER TABLE wormswar.backpack_item_3 ADD CONSTRAINT backpack_item_fkey_3 FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.backpack_item_3 ADD CONSTRAINT backpack_item_3_profile_id_check CHECK ((profile_id % 4::bigint) = 3);
CREATE INDEX backpack_profile_id_index_3 ON wormswar.backpack_item_3 USING btree (profile_id);
ALTER TABLE wormswar.backpack_item_3 CLUSTER ON backpack_profile_id_index_3;

--select * from pg_stat_activity where datname = 'wormswar'