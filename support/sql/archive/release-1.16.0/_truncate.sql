select now();

-- чистим таблицу возвратов
delete from wormswar.callback_friend where date < now() - interval '60 days';

-- заполняем донатеров
truncate _donaters;
insert into _donaters select distinct profile_id from payment_statistic_parent where coalesce(payment_status, 0) = 0 ;

-- заполняем тех кого хотим удалить
truncate _loss_profiles;
-- 2017-08-01
insert into _loss_profiles
select id, level, last_login_time from wormswar.user_profile up where
not ((last_login_time > '2017-06-01') -- 2 мес. назад
or (last_login_time > '2016-08-01' and last_login_time < '2014-06-01' and level > 1) -- год назад
or (last_login_time > '2015-08-01' and last_login_time < '2016-08-01' and level > 5) -- 2 года назад
or (last_login_time < '2014-08-01' and level > 8) -- > 3-х лет назад
);

-- заполняем "возвращенцев"
truncate _callbacks;
insert into _callbacks
select distinct profile_id from (
select profile_id from wormswar.callback_friend
union
select friend_id as profile_id from wormswar.callback_friend
) as a;

-- удаляем команды в которых оба участника будут удалены
with a as (
select * from wormswar.worm_groups where
exists (select loss_profile from _loss_profiles where loss_profile = team_member_1)
and exists (select loss_profile from _loss_profiles where loss_profile = team_member_2)
and team_member_3 is null and team_member_4 is null
), b as (
 select profile_id from a where not (
exists (select * from wormswar.ban_list where team_member_1 = profile_id)
or exists (select * from _callbacks where team_member_1 = profile_id)
or exists (select * from _donaters where team_member_1 = profile_id)

or exists (select * from wormswar.ban_list where team_member_2 = profile_id)
or exists (select * from _callbacks where team_member_2 = profile_id)
or exists (select * from _donaters where team_member_2 = profile_id)
)
)
delete from wormswar.worm_groups where profile_id in (select profile_id from b);

-- заполняем тех кто у кого-нибудь в команде
truncate _team_members_all;
insert into _team_members_all
select distinct team_member from (
select team_member_1 as team_member from wormswar.worm_groups where team_member_1 is not null
union
select team_member_2 as team_member from wormswar.worm_groups where team_member_2 is not null
union
select team_member_3 as team_member from wormswar.worm_groups where team_member_3 is not null
union
select team_member_4 as team_member from wormswar.worm_groups where team_member_4 is not null
) as a;

-- заполняем тех кого будем оставлять
truncate _live_profiles;
insert into _live_profiles
select id from wormswar.user_profile up where 
not exists (select * from _loss_profiles where up.id = loss_profile)
or exists (select * from _team_members_all where up.id = team_member)
or exists (select * from wormswar.ban_list where up.id = profile_id)
or exists (select * from _callbacks where up.id = profile_id)
or exists (select * from _donaters where up.id = profile_id);
--Запрос успешно выполнен: 9964001 строк изменено за 389275 мс.

-- дампим таблицы
COPY (SELECT * FROM wormswar.user_profile up WHERE exists (select * from _live_profiles where up.id = live_profile)) TO '/home/postgres/truncate/live_user_profiles.copy';
COPY (SELECT * FROM wormswar.backpack_item_0 bi WHERE exists (select * from _live_profiles where bi.profile_id = live_profile) and weapon_id > 4) TO '/home/postgres/truncate/live_backpack_item_0.copy';
COPY (SELECT * FROM wormswar.backpack_item_1 bi WHERE exists (select * from _live_profiles where bi.profile_id = live_profile) and weapon_id > 4) TO '/home/postgres/truncate/live_backpack_item_1.copy';
COPY (SELECT * FROM wormswar.backpack_item_2 bi WHERE exists (select * from _live_profiles where bi.profile_id = live_profile) and weapon_id > 4) TO '/home/postgres/truncate/live_backpack_item_2.copy';
COPY (SELECT * FROM wormswar.backpack_item_3 bi WHERE exists (select * from _live_profiles where bi.profile_id = live_profile) and weapon_id > 4) TO '/home/postgres/truncate/live_backpack_item_3.copy';

--## удаляем зависимости ##--
ALTER TABLE wormswar.backpack_item DROP CONSTRAINT backpack_item_profile_id_fkey;

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

-- user_profile deps --
ALTER TABLE wormswar.callback_friend DROP CONSTRAINT callback_friend_fk_friend_id;
ALTER TABLE wormswar.ban_list DROP CONSTRAINT ban_list_profile_id_fkey;
ALTER TABLE wormswar.payment_statistic DROP CONSTRAINT payment_statistic_fk_profile_id;
ALTER TABLE wormswar.backpack_conf DROP CONSTRAINT backpack_conf_profile_id_fkey;
ALTER TABLE wormswar.worm_groups DROP CONSTRAINT worm_groups_profile_id_fkey;
ALTER TABLE wormswar.worm_groups DROP CONSTRAINT worm_groups_team_member_1_fkey;
ALTER TABLE wormswar.worm_groups DROP CONSTRAINT worm_groups_team_member_2_fkey;
ALTER TABLE wormswar.worm_groups DROP CONSTRAINT worm_groups_team_member_3_fkey;
ALTER TABLE wormswar.worm_groups DROP CONSTRAINT worm_groups_team_member_4_fkey;

-- user_profile --
ALTER TABLE wormswar.user_profile DROP CONSTRAINT user_profile_pkey;
DROP INDEX wormswar.ind_user_profile_id_level;
DROP INDEX wormswar.rating_index;
DROP TRIGGER user_profile_before_insert ON wormswar.user_profile;

truncate wormswar.user_profile;
truncate wormswar.backpack_item_0;
truncate wormswar.backpack_item_1;
truncate wormswar.backpack_item_2;
truncate wormswar.backpack_item_3;

-- ## RESTORE ## --

COPY wormswar.user_profile FROM '/home/postgres/truncate/live_user_profiles.copy';
COPY wormswar.backpack_item_0 FROM '/home/postgres/truncate/live_backpack_item_0.copy';
COPY wormswar.backpack_item_1 FROM '/home/postgres/truncate/live_backpack_item_1.copy';
COPY wormswar.backpack_item_2 FROM '/home/postgres/truncate/live_backpack_item_2.copy';
COPY wormswar.backpack_item_3 FROM '/home/postgres/truncate/live_backpack_item_3.copy';

-- ## восстанавливаем зависимости ##--
-- user_profile --
ALTER TABLE wormswar.user_profile ADD CONSTRAINT user_profile_pkey PRIMARY KEY(id);
CREATE INDEX ind_user_profile_id_level ON wormswar.user_profile USING btree (id, level);
CREATE INDEX rating_index ON wormswar.user_profile USING btree (rating);
CREATE TRIGGER user_profile_before_insert BEFORE INSERT ON wormswar.user_profile FOR EACH ROW EXECUTE PROCEDURE wormswar.user_profile_insert_trigger();

-- user_profile deps --
ALTER TABLE wormswar.callback_friend ADD CONSTRAINT callback_friend_fk_friend_id FOREIGN KEY (friend_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.ban_list ADD CONSTRAINT ban_list_profile_id_fkey FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.payment_statistic ADD CONSTRAINT payment_statistic_fk_profile_id FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.worm_groups ADD CONSTRAINT worm_groups_profile_id_fkey FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.worm_groups ADD CONSTRAINT worm_groups_team_member_1_fkey FOREIGN KEY (team_member_1) REFERENCES wormswar.user_profile (id) MATCH SIMPLE  ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.worm_groups ADD CONSTRAINT worm_groups_team_member_2_fkey FOREIGN KEY (team_member_2) REFERENCES wormswar.user_profile (id) MATCH SIMPLE  ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.worm_groups ADD CONSTRAINT worm_groups_team_member_3_fkey FOREIGN KEY (team_member_3) REFERENCES wormswar.user_profile (id) MATCH SIMPLE  ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE wormswar.worm_groups ADD CONSTRAINT worm_groups_team_member_4_fkey FOREIGN KEY (team_member_4) REFERENCES wormswar.user_profile (id) MATCH SIMPLE  ON UPDATE NO ACTION ON DELETE NO ACTION;


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

-- чистим реагенты
COPY (select * from wormswar.reagents where exists (select live_profile from _live_profiles where live_profile = profile_id)) TO '/home/postgres/truncate/reagents.copy';
truncate wormswar.reagents;
ALTER TABLE wormswar.reagents DROP CONSTRAINT reagents_pkey;
COPY wormswar.reagents FROM '/home/postgres/truncate/reagents.copy';
ALTER TABLE wormswar.reagents ADD CONSTRAINT reagents_pkey PRIMARY KEY(profile_id);

--  чистим мастерство
delete from wormswar.true_skill where not exists (select live_profile from _live_profiles where live_profile = profile_id);

-- чистим таблицу регистраций
delete from wormswar.creation_date where not exists (select live_profile from _live_profiles where live_profile = id);

delete from wormswar.backpack_conf where not exists (select live_profile from _live_profiles where live_profile = profile_id);
ALTER TABLE wormswar.backpack_conf ADD CONSTRAINT backpack_conf_profile_id_fkey FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
/*

psql -f ./_truncate.sql -e wormswar

select pg_size_pretty( 
sum(pg_column_size(id)) + 
sum(pg_column_size(armor)) + 
sum(pg_column_size(attack)) + 
sum(pg_column_size(battles_count)) + 
sum(pg_column_size(experience)) + 
sum(pg_column_size(last_battle_time)) + 
sum(pg_column_size(level)) + 
sum(pg_column_size(money)) + 
sum(pg_column_size(realmoney)) +
sum(pg_column_size(last_login_time)) + 
sum(pg_column_size(rating)) + 
sum(pg_column_size(last_search_time)) + 
sum(pg_column_size(hat)) + 
sum(pg_column_size(stuff)) + 
sum(pg_column_size(login_sequence)) +
sum(pg_column_size(reaction_rate)) +
sum(pg_column_size(current_mission)) +
sum(pg_column_size(recipes)) +
sum(pg_column_size(comebacked_friends))
)
from wormswar.user_profile 

select pg_size_pretty(pg_table_size('wormswar.user_profile'))
*/