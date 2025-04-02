-- select table_name from information_schema.tables where table_schema = 'log' and table_name like 'award_%'
-- order by 1
truncate _live_profiles;
insert into _live_profiles
select id from wormswar.user_profile;

delete from log.award_statistic_03_11_2010 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_03_11_2010;
REINDEX TABLE log.award_statistic_03_11_2010;

delete from log.award_statistic_16_12_2010 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_16_12_2010;
REINDEX TABLE log.award_statistic_16_12_2010;

delete from log.award_statistic_04_02_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_04_02_2011;
REINDEX TABLE log.award_statistic_04_02_2011;

delete from log.award_statistic_06_04_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_06_04_2011;
REINDEX TABLE log.award_statistic_06_04_2011;

delete from log.award_statistic_10_03_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_10_03_2011;
REINDEX TABLE log.award_statistic_10_03_2011;

delete from log.award_statistic_11_11_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_11_11_2011;
REINDEX TABLE log.award_statistic_11_11_2011;

delete from log.award_statistic_12_05_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_12_05_2011;
REINDEX TABLE log.award_statistic_12_05_2011;

delete from log.award_statistic_29_09_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_29_09_2011;
REINDEX TABLE log.award_statistic_29_09_2011;

delete from log.award_statistic_31_07_2011 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_31_07_2011;
REINDEX TABLE log.award_statistic_31_07_2011;

delete from log.award_statistic_2011_12_23 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2011_12_23;
REINDEX TABLE log.award_statistic_2011_12_23;

delete from log.award_statistic_2012_02_09 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2012_02_09;
REINDEX TABLE log.award_statistic_2012_02_09;

delete from log.award_statistic_2012_04_12 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2012_04_12;
REINDEX TABLE log.award_statistic_2012_04_12;

delete from log.award_statistic_2012_05_24 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2012_05_24;
REINDEX TABLE log.award_statistic_2012_05_24;

delete from log.award_statistic_2012_07_30 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2012_07_30;
REINDEX TABLE log.award_statistic_2012_07_30;

delete from log.award_statistic_2012_09_12 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2012_09_12;
REINDEX TABLE log.award_statistic_2012_09_12;

delete from log.award_statistic_2012_10_31 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2012_10_31;
REINDEX TABLE log.award_statistic_2012_10_31;

delete from log.award_statistic_2012_12_21 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2012_12_21;
REINDEX TABLE log.award_statistic_2012_12_21;

delete from log.award_statistic_2013_02_26 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2013_02_26;
REINDEX TABLE log.award_statistic_2013_02_26;

delete from log.award_statistic_2013_03_29 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2013_03_29;
REINDEX TABLE log.award_statistic_2013_03_29;

delete from log.award_statistic_2013_06_05 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2013_06_05;
REINDEX TABLE log.award_statistic_2013_06_05;

delete from log.award_statistic_2013_08_02 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2013_08_02;
REINDEX TABLE log.award_statistic_2013_08_02;

delete from log.award_statistic_2013_09_01 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2013_09_01;
REINDEX TABLE log.award_statistic_2013_09_01;

delete from log.award_statistic_2013_10_16 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2013_10_16;
REINDEX TABLE log.award_statistic_2013_10_16;

delete from log.award_statistic_2013_12_23 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2013_12_23;
REINDEX TABLE log.award_statistic_2013_12_23;

delete from log.award_statistic_2014_03_27 where not exists (select live_profile from _live_profiles where live_profile = profile_id);
VACUUM FULL VERBOSE ANALYZE log.award_statistic_2014_03_27;
REINDEX TABLE log.award_statistic_2014_03_27;

-- def sql = """delete from log.## where not exists (select live_profile from _live_profiles where live_profile = profile_id);
-- VACUUM FULL VERBOSE ANALYZE log.##;
-- REINDEX TABLE log.##;
-- """
--
-- [
--         "award_statistic_2014_03_27",
-- ].each {
--     System.out.println(sql.replaceAll("##", it));
-- }
