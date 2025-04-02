-- select table_name from information_schema.tables where table_schema = 'log' and table_name like 'shop_%'
-- order by 1
COPY (select * from log.shop_statistic_03_11_2010 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_03_11_2010.copy';
truncate log.shop_statistic_03_11_2010;
ALTER TABLE log.shop_statistic_03_11_2010 DROP CONSTRAINT shop_statistic_03_11_2010_pkey;
DROP INDEX log.shop_statistic_03_11_2010_i_profile_id;
COPY log.shop_statistic_03_11_2010 FROM '/home/postgres/truncate/log/shop_statistic_03_11_2010.copy';
ALTER TABLE log.shop_statistic_03_11_2010 ADD CONSTRAINT shop_statistic_03_11_2010_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_03_11_2010_i_profile_id ON log.shop_statistic_03_11_2010 USING btree (profile_id);

COPY (select * from log.shop_statistic_16_12_2010 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_16_12_2010.copy';
truncate log.shop_statistic_16_12_2010;
ALTER TABLE log.shop_statistic_16_12_2010 DROP CONSTRAINT shop_statistic_16_12_2010_pkey;
DROP INDEX log.shop_statistic_16_12_2010_i_profile_id;
COPY log.shop_statistic_16_12_2010 FROM '/home/postgres/truncate/log/shop_statistic_16_12_2010.copy';
ALTER TABLE log.shop_statistic_16_12_2010 ADD CONSTRAINT shop_statistic_16_12_2010_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_16_12_2010_i_profile_id ON log.shop_statistic_16_12_2010 USING btree (profile_id);

COPY (select * from log.shop_statistic_04_02_2011 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_04_02_2011.copy';
truncate log.shop_statistic_04_02_2011;
ALTER TABLE log.shop_statistic_04_02_2011 DROP CONSTRAINT shop_statistic_04_02_2011_pkey;
DROP INDEX log.shop_statistic_04_02_2011_i_profile_id;
COPY log.shop_statistic_04_02_2011 FROM '/home/postgres/truncate/log/shop_statistic_04_02_2011.copy';
ALTER TABLE log.shop_statistic_04_02_2011 ADD CONSTRAINT shop_statistic_04_02_2011_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_04_02_2011_i_profile_id ON log.shop_statistic_04_02_2011 USING btree (profile_id);

COPY (select * from log.shop_statistic_06_04_2011 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_06_04_2011.copy';
truncate log.shop_statistic_06_04_2011;
ALTER TABLE log.shop_statistic_06_04_2011 DROP CONSTRAINT shop_statistic_06_04_2011_pkey;
DROP INDEX log.shop_statistic_06_04_2011_i_profile_id;
COPY log.shop_statistic_06_04_2011 FROM '/home/postgres/truncate/log/shop_statistic_06_04_2011.copy';
ALTER TABLE log.shop_statistic_06_04_2011 ADD CONSTRAINT shop_statistic_06_04_2011_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_06_04_2011_i_profile_id ON log.shop_statistic_06_04_2011 USING btree (profile_id);

COPY (select * from log.shop_statistic_10_03_2011 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_10_03_2011.copy';
truncate log.shop_statistic_10_03_2011;
ALTER TABLE log.shop_statistic_10_03_2011 DROP CONSTRAINT shop_statistic_10_03_2011_pkey;
DROP INDEX log.shop_statistic_10_03_2011_i_profile_id;
COPY log.shop_statistic_10_03_2011 FROM '/home/postgres/truncate/log/shop_statistic_10_03_2011.copy';
ALTER TABLE log.shop_statistic_10_03_2011 ADD CONSTRAINT shop_statistic_10_03_2011_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_10_03_2011_i_profile_id ON log.shop_statistic_10_03_2011 USING btree (profile_id);

COPY (select * from log.shop_statistic_11_11_2011 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_11_11_2011.copy';
truncate log.shop_statistic_11_11_2011;
ALTER TABLE log.shop_statistic_11_11_2011 DROP CONSTRAINT shop_statistic_11_11_2011_pkey;
DROP INDEX log.shop_statistic_11_11_2011_i_profile_id;
COPY log.shop_statistic_11_11_2011 FROM '/home/postgres/truncate/log/shop_statistic_11_11_2011.copy';
ALTER TABLE log.shop_statistic_11_11_2011 ADD CONSTRAINT shop_statistic_11_11_2011_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_11_11_2011_i_profile_id ON log.shop_statistic_11_11_2011 USING btree (profile_id);

COPY (select * from log.shop_statistic_12_05_2011 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_12_05_2011.copy';
truncate log.shop_statistic_12_05_2011;
ALTER TABLE log.shop_statistic_12_05_2011 DROP CONSTRAINT shop_statistic_12_05_2011_pkey;
DROP INDEX log.shop_statistic_12_05_2011_i_profile_id;
COPY log.shop_statistic_12_05_2011 FROM '/home/postgres/truncate/log/shop_statistic_12_05_2011.copy';
ALTER TABLE log.shop_statistic_12_05_2011 ADD CONSTRAINT shop_statistic_12_05_2011_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_12_05_2011_i_profile_id ON log.shop_statistic_12_05_2011 USING btree (profile_id);

COPY (select * from log.shop_statistic_29_09_2011 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_29_09_2011.copy';
truncate log.shop_statistic_29_09_2011;
ALTER TABLE log.shop_statistic_29_09_2011 DROP CONSTRAINT shop_statistic_29_09_2011_pkey;
DROP INDEX log.shop_statistic_29_09_2011_i_profile_id;
COPY log.shop_statistic_29_09_2011 FROM '/home/postgres/truncate/log/shop_statistic_29_09_2011.copy';
ALTER TABLE log.shop_statistic_29_09_2011 ADD CONSTRAINT shop_statistic_29_09_2011_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_29_09_2011_i_profile_id ON log.shop_statistic_29_09_2011 USING btree (profile_id);

COPY (select * from log.shop_statistic_31_07_2011 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_31_07_2011.copy';
truncate log.shop_statistic_31_07_2011;
ALTER TABLE log.shop_statistic_31_07_2011 DROP CONSTRAINT shop_statistic_31_07_2011_pkey;
DROP INDEX log.shop_statistic_31_07_2011_i_profile_id;
COPY log.shop_statistic_31_07_2011 FROM '/home/postgres/truncate/log/shop_statistic_31_07_2011.copy';
ALTER TABLE log.shop_statistic_31_07_2011 ADD CONSTRAINT shop_statistic_31_07_2011_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_31_07_2011_i_profile_id ON log.shop_statistic_31_07_2011 USING btree (profile_id);

COPY (select * from log.shop_statistic_2011_12_23 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2011_12_23.copy';
truncate log.shop_statistic_2011_12_23;
ALTER TABLE log.shop_statistic_2011_12_23 DROP CONSTRAINT shop_statistic_2011_12_23_pkey;
DROP INDEX log.shop_statistic_2011_12_23_i_profile_id;
COPY log.shop_statistic_2011_12_23 FROM '/home/postgres/truncate/log/shop_statistic_2011_12_23.copy';
ALTER TABLE log.shop_statistic_2011_12_23 ADD CONSTRAINT shop_statistic_2011_12_23_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2011_12_23_i_profile_id ON log.shop_statistic_2011_12_23 USING btree (profile_id);

COPY (select * from log.shop_statistic_2012_02_09 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2012_02_09.copy';
truncate log.shop_statistic_2012_02_09;
ALTER TABLE log.shop_statistic_2012_02_09 DROP CONSTRAINT shop_statistic_2012_02_09_pkey;
DROP INDEX log.shop_statistic_2012_02_09_i_profile_id;
COPY log.shop_statistic_2012_02_09 FROM '/home/postgres/truncate/log/shop_statistic_2012_02_09.copy';
ALTER TABLE log.shop_statistic_2012_02_09 ADD CONSTRAINT shop_statistic_2012_02_09_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2012_02_09_i_profile_id ON log.shop_statistic_2012_02_09 USING btree (profile_id);

COPY (select * from log.shop_statistic_2012_04_12 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2012_04_12.copy';
truncate log.shop_statistic_2012_04_12;
ALTER TABLE log.shop_statistic_2012_04_12 DROP CONSTRAINT shop_statistic_2012_04_12_pkey;
DROP INDEX log.shop_statistic_2012_04_12_i_profile_id;
COPY log.shop_statistic_2012_04_12 FROM '/home/postgres/truncate/log/shop_statistic_2012_04_12.copy';
ALTER TABLE log.shop_statistic_2012_04_12 ADD CONSTRAINT shop_statistic_2012_04_12_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2012_04_12_i_profile_id ON log.shop_statistic_2012_04_12 USING btree (profile_id);

COPY (select * from log.shop_statistic_2012_05_24 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2012_05_24.copy';
truncate log.shop_statistic_2012_05_24;
ALTER TABLE log.shop_statistic_2012_05_24 DROP CONSTRAINT shop_statistic_2012_05_24_pkey;
DROP INDEX log.shop_statistic_2012_05_24_i_profile_id;
COPY log.shop_statistic_2012_05_24 FROM '/home/postgres/truncate/log/shop_statistic_2012_05_24.copy';
ALTER TABLE log.shop_statistic_2012_05_24 ADD CONSTRAINT shop_statistic_2012_05_24_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2012_05_24_i_profile_id ON log.shop_statistic_2012_05_24 USING btree (profile_id);

COPY (select * from log.shop_statistic_2012_07_30 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2012_07_30.copy';
truncate log.shop_statistic_2012_07_30;
ALTER TABLE log.shop_statistic_2012_07_30 DROP CONSTRAINT shop_statistic_2012_07_30_pkey;
DROP INDEX log.shop_statistic_2012_07_30_i_profile_id;
COPY log.shop_statistic_2012_07_30 FROM '/home/postgres/truncate/log/shop_statistic_2012_07_30.copy';
ALTER TABLE log.shop_statistic_2012_07_30 ADD CONSTRAINT shop_statistic_2012_07_30_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2012_07_30_i_profile_id ON log.shop_statistic_2012_07_30 USING btree (profile_id);

COPY (select * from log.shop_statistic_2012_09_12 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2012_09_12.copy';
truncate log.shop_statistic_2012_09_12;
ALTER TABLE log.shop_statistic_2012_09_12 DROP CONSTRAINT shop_statistic_2012_09_12_pkey;
DROP INDEX log.shop_statistic_2012_09_12_i_profile_id;
COPY log.shop_statistic_2012_09_12 FROM '/home/postgres/truncate/log/shop_statistic_2012_09_12.copy';
ALTER TABLE log.shop_statistic_2012_09_12 ADD CONSTRAINT shop_statistic_2012_09_12_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2012_09_12_i_profile_id ON log.shop_statistic_2012_09_12 USING btree (profile_id);

COPY (select * from log.shop_statistic_2012_10_31 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2012_10_31.copy';
truncate log.shop_statistic_2012_10_31;
ALTER TABLE log.shop_statistic_2012_10_31 DROP CONSTRAINT shop_statistic_2012_10_31_pkey;
DROP INDEX log.shop_statistic_2012_10_31_i_profile_id;
COPY log.shop_statistic_2012_10_31 FROM '/home/postgres/truncate/log/shop_statistic_2012_10_31.copy';
ALTER TABLE log.shop_statistic_2012_10_31 ADD CONSTRAINT shop_statistic_2012_10_31_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2012_10_31_i_profile_id ON log.shop_statistic_2012_10_31 USING btree (profile_id);

COPY (select * from log.shop_statistic_2012_12_21 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2012_12_21.copy';
truncate log.shop_statistic_2012_12_21;
ALTER TABLE log.shop_statistic_2012_12_21 DROP CONSTRAINT shop_statistic_2012_12_21_pkey;
DROP INDEX log.shop_statistic_2012_12_21_i_profile_id;
COPY log.shop_statistic_2012_12_21 FROM '/home/postgres/truncate/log/shop_statistic_2012_12_21.copy';
ALTER TABLE log.shop_statistic_2012_12_21 ADD CONSTRAINT shop_statistic_2012_12_21_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2012_12_21_i_profile_id ON log.shop_statistic_2012_12_21 USING btree (profile_id);

COPY (select * from log.shop_statistic_2013_02_26 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2013_02_26.copy';
truncate log.shop_statistic_2013_02_26;
ALTER TABLE log.shop_statistic_2013_02_26 DROP CONSTRAINT shop_statistic_2013_02_26_pkey;
DROP INDEX log.shop_statistic_2013_02_26_i_profile_id;
COPY log.shop_statistic_2013_02_26 FROM '/home/postgres/truncate/log/shop_statistic_2013_02_26.copy';
ALTER TABLE log.shop_statistic_2013_02_26 ADD CONSTRAINT shop_statistic_2013_02_26_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2013_02_26_i_profile_id ON log.shop_statistic_2013_02_26 USING btree (profile_id);

COPY (select * from log.shop_statistic_2013_03_29 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2013_03_29.copy';
truncate log.shop_statistic_2013_03_29;
ALTER TABLE log.shop_statistic_2013_03_29 DROP CONSTRAINT shop_statistic_2013_03_29_pkey;
DROP INDEX log.shop_statistic_2013_03_29_i_profile_id;
COPY log.shop_statistic_2013_03_29 FROM '/home/postgres/truncate/log/shop_statistic_2013_03_29.copy';
ALTER TABLE log.shop_statistic_2013_03_29 ADD CONSTRAINT shop_statistic_2013_03_29_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2013_03_29_i_profile_id ON log.shop_statistic_2013_03_29 USING btree (profile_id);

COPY (select * from log.shop_statistic_2013_06_05 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2013_06_05.copy';
truncate log.shop_statistic_2013_06_05;
ALTER TABLE log.shop_statistic_2013_06_05 DROP CONSTRAINT shop_statistic_2013_06_05_pkey;
DROP INDEX log.shop_statistic_2013_06_05_i_profile_id;
COPY log.shop_statistic_2013_06_05 FROM '/home/postgres/truncate/log/shop_statistic_2013_06_05.copy';
ALTER TABLE log.shop_statistic_2013_06_05 ADD CONSTRAINT shop_statistic_2013_06_05_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2013_06_05_i_profile_id ON log.shop_statistic_2013_06_05 USING btree (profile_id);

COPY (select * from log.shop_statistic_2013_08_02 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2013_08_02.copy';
truncate log.shop_statistic_2013_08_02;
ALTER TABLE log.shop_statistic_2013_08_02 DROP CONSTRAINT shop_statistic_2013_08_02_pkey;
DROP INDEX log.shop_statistic_2013_08_02_i_profile_id;
COPY log.shop_statistic_2013_08_02 FROM '/home/postgres/truncate/log/shop_statistic_2013_08_02.copy';
ALTER TABLE log.shop_statistic_2013_08_02 ADD CONSTRAINT shop_statistic_2013_08_02_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2013_08_02_i_profile_id ON log.shop_statistic_2013_08_02 USING btree (profile_id);

COPY (select * from log.shop_statistic_2013_09_01 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2013_09_01.copy';
truncate log.shop_statistic_2013_09_01;
ALTER TABLE log.shop_statistic_2013_09_01 DROP CONSTRAINT shop_statistic_2013_09_01_pkey;
DROP INDEX log.shop_statistic_2013_09_01_i_profile_id;
COPY log.shop_statistic_2013_09_01 FROM '/home/postgres/truncate/log/shop_statistic_2013_09_01.copy';
ALTER TABLE log.shop_statistic_2013_09_01 ADD CONSTRAINT shop_statistic_2013_09_01_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2013_09_01_i_profile_id ON log.shop_statistic_2013_09_01 USING btree (profile_id);

COPY (select * from log.shop_statistic_2013_10_16 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2013_10_16.copy';
truncate log.shop_statistic_2013_10_16;
ALTER TABLE log.shop_statistic_2013_10_16 DROP CONSTRAINT shop_statistic_2013_10_16_pkey;
DROP INDEX log.shop_statistic_2013_10_16_i_profile_id;
COPY log.shop_statistic_2013_10_16 FROM '/home/postgres/truncate/log/shop_statistic_2013_10_16.copy';
ALTER TABLE log.shop_statistic_2013_10_16 ADD CONSTRAINT shop_statistic_2013_10_16_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2013_10_16_i_profile_id ON log.shop_statistic_2013_10_16 USING btree (profile_id);

COPY (select * from log.shop_statistic_2013_12_23 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2013_12_23.copy';
truncate log.shop_statistic_2013_12_23;
ALTER TABLE log.shop_statistic_2013_12_23 DROP CONSTRAINT shop_statistic_2013_12_23_pkey;
DROP INDEX log.shop_statistic_2013_12_23_i_profile_id;
COPY log.shop_statistic_2013_12_23 FROM '/home/postgres/truncate/log/shop_statistic_2013_12_23.copy';
ALTER TABLE log.shop_statistic_2013_12_23 ADD CONSTRAINT shop_statistic_2013_12_23_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2013_12_23_i_profile_id ON log.shop_statistic_2013_12_23 USING btree (profile_id);

COPY (select * from log.shop_statistic_2014_03_27 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2014_03_27.copy';
truncate log.shop_statistic_2014_03_27;
ALTER TABLE log.shop_statistic_2014_03_27 DROP CONSTRAINT shop_statistic_2014_03_27_pkey;
DROP INDEX log.shop_statistic_2014_03_27_i_profile_id;
COPY log.shop_statistic_2014_03_27 FROM '/home/postgres/truncate/log/shop_statistic_2014_03_27.copy';
ALTER TABLE log.shop_statistic_2014_03_27 ADD CONSTRAINT shop_statistic_2014_03_27_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2014_03_27_i_profile_id ON log.shop_statistic_2014_03_27 USING btree (profile_id);

-- def sql ="""COPY (select * from log.## where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/##.copy';
-- truncate log.##;
-- ALTER TABLE log.## DROP CONSTRAINT ##_pkey;
-- DROP INDEX log.##_i_profile_id;
-- COPY log.## FROM '/home/postgres/truncate/log/##.copy';
-- ALTER TABLE log.## ADD CONSTRAINT ##_pkey PRIMARY KEY(id);
-- CREATE INDEX ##_i_profile_id ON log.## USING btree (profile_id);
-- """
--
-- [
--         "shop_statistic_2014_03_27",
-- ].each {
--     System.out.println(sql.replaceAll("##", it));
-- }

