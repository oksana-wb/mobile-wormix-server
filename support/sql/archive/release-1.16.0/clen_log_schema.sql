-- select table_name from information_schema.tables where table_schema = 'log' and table_name like 'award_%'
-- order by 1
delete from log.award_statistic_03_11_2010 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_16_12_2010 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_04_02_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_06_04_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_10_03_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_11_11_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_12_05_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_29_09_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_31_07_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2011_12_23 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2012_02_09 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2012_04_12 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2012_05_24 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2012_07_30 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2012_09_12 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2012_10_31 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2012_12_21 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2013_02_26 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2013_03_29 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2013_06_05 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2013_08_02 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2013_09_01 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2013_10_16 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.award_statistic_2013_12_23 where not exists (select live_profile from _live_profiles where live_profile = profile_id);

-- select table_name from information_schema.tables where table_schema = 'log' and table_name like 'shop_%'
-- order by 1
COPY (select * from log.shop_statistic_03_11_2010 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_03_11_2010.copy';
truncate log.shop_statistic_03_11_2010;
ALTER TABLE log.shop_statistic_03_11_2010 DROP CONSTRAINT shop_statistic_03_11_2010_pkey;
DROP INDEX log.shop_statistic_03_11_2010_i_profile_id;
COPY log.shop_statistic_03_11_2010 FROM '/home/postgres/truncate/log/shop_statistic_03_11_2010.copy';
ALTER TABLE log.shop_statistic_03_11_2010 ADD CONSTRAINT shop_statistic_03_11_2010_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_03_11_2010_i_profile_id ON log.shop_statistic_03_11_2010 USING btree (profile_id);

delete from log.shop_statistic_16_12_2010 where not exists (select live_profile from _live_profiles where live_profile = profile_id);

COPY (select * from log.shop_statistic_04_02_2011 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_04_02_2011.copy';
truncate log.shop_statistic_04_02_2011;
ALTER TABLE log.shop_statistic_04_02_2011 DROP CONSTRAINT shop_statistic_04_02_2011_pkey;
DROP INDEX log.shop_statistic_04_02_2011_i_profile_id;
COPY log.shop_statistic_04_02_2011 FROM '/home/postgres/truncate/log/shop_statistic_04_02_2011.copy';
ALTER TABLE log.shop_statistic_04_02_2011 ADD CONSTRAINT shop_statistic_04_02_2011_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_04_02_2011_i_profile_id ON log.shop_statistic_04_02_2011 USING btree (profile_id);

delete from log.shop_statistic_06_04_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);

COPY (select * from log.shop_statistic_10_03_2011 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_10_03_2011.copy';
truncate log.shop_statistic_10_03_2011;
ALTER TABLE log.shop_statistic_10_03_2011 DROP CONSTRAINT shop_statistic_10_03_2011_pkey;
DROP INDEX log.shop_statistic_10_03_2011_i_profile_id;
COPY log.shop_statistic_10_03_2011 FROM '/home/postgres/truncate/log/shop_statistic_10_03_2011.copy';
ALTER TABLE log.shop_statistic_10_03_2011 ADD CONSTRAINT shop_statistic_10_03_2011_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_10_03_2011_i_profile_id ON log.shop_statistic_10_03_2011 USING btree (profile_id);

delete from log.shop_statistic_11_11_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.shop_statistic_12_05_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.shop_statistic_29_09_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);

COPY (select * from log.shop_statistic_31_07_2011 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_31_07_2011.copy';
truncate log.shop_statistic_31_07_2011;
ALTER TABLE log.shop_statistic_31_07_2011 DROP CONSTRAINT shop_statistic_31_07_2011_pkey;
DROP INDEX log.shop_statistic_31_07_2011_i_profile_id;
COPY log.shop_statistic_31_07_2011 FROM '/home/postgres/truncate/log/shop_statistic_31_07_2011.copy';
ALTER TABLE log.shop_statistic_31_07_2011 ADD CONSTRAINT shop_statistic_31_07_2011_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_31_07_2011_i_profile_id ON log.shop_statistic_31_07_2011 USING btree (profile_id);

delete from log.shop_statistic_2011_12_23 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.shop_statistic_2012_02_09 where not exists (select live_profile from _live_profiles where live_profile = profile_id);

COPY (select * from log.shop_statistic_2012_04_12 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2012_04_12.copy';
truncate log.shop_statistic_2012_04_12;
ALTER TABLE log.shop_statistic_2012_04_12 DROP CONSTRAINT shop_statistic_2012_04_12_pkey;
DROP INDEX log.shop_statistic_2012_04_12_i_profile_id;
COPY log.shop_statistic_2012_04_12 FROM '/home/postgres/truncate/log/shop_statistic_2012_04_12.copy';
ALTER TABLE log.shop_statistic_2012_04_12 ADD CONSTRAINT shop_statistic_2012_04_12_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2012_04_12_i_profile_id ON log.shop_statistic_2012_04_12 USING btree (profile_id);

delete from log.shop_statistic_2012_05_24 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.shop_statistic_2012_07_30 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.shop_statistic_2012_09_12 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.shop_statistic_2012_10_31 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.shop_statistic_2012_12_21 where not exists (select live_profile from _live_profiles where live_profile = profile_id);

COPY (select * from log.shop_statistic_2013_02_26 where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/log/shop_statistic_2013_02_26.copy';
truncate log.shop_statistic_2013_02_26;
ALTER TABLE log.shop_statistic_2013_02_26 DROP CONSTRAINT shop_statistic_2013_02_26_pkey;
DROP INDEX log.shop_statistic_2013_02_26_i_profile_id;
COPY log.shop_statistic_2013_02_26 FROM '/home/postgres/truncate/log/shop_statistic_2013_02_26.copy';
ALTER TABLE log.shop_statistic_2013_02_26 ADD CONSTRAINT shop_statistic_2013_02_26_pkey PRIMARY KEY(id);
CREATE INDEX shop_statistic_2013_02_26_i_profile_id ON log.shop_statistic_2013_02_26 USING btree (profile_id);

delete from log.shop_statistic_2013_03_29 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.shop_statistic_2013_06_05 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.shop_statistic_2013_08_02 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.shop_statistic_2013_09_01 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.shop_statistic_2013_10_16 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
delete from log.shop_statistic_2013_12_23 where not exists (select live_profile from _live_profiles where live_profile = profile_id);

