CREATE TABLE __race_purshase
(
    profile_id INTEGER  NOT NULL,
    race       SMALLINT NOT NULL,
    CONSTRAINT __race_purshase_pkey PRIMARY KEY (profile_id, race)
)
WITH (
OIDS =FALSE
);
ALTER TABLE __race_purshase
OWNER TO postgres;

insert into __race_purshase select profile_id, item_id from shop_statistic_parent where date > '2015-07-01' and item_type=8 group by 1, 2;
--Запрос успешно выполнен: 86329 строк изменено за 348173 мс.

ALTER TABLE wormswar.user_profile ADD COLUMN races smallint;

update wormswar.user_profile set races = coalesce(races, 0) | coalesce((select 1 << race from __race_purshase where id=profile_id and race = 2), 0) where id in (select distinct profile_id from __race_purshase);
--Запрос успешно выполнен: 76745 строк изменено за 18786 мс.
update wormswar.user_profile set races = coalesce(races, 0) | coalesce((select 1 << race from __race_purshase where id=profile_id and race = 3), 0) where id in (select distinct profile_id from __race_purshase);
update wormswar.user_profile set races = coalesce(races, 0) | coalesce((select 1 << race from __race_purshase where id=profile_id and race = 4), 0) where id in (select distinct profile_id from __race_purshase);
update wormswar.user_profile set races = coalesce(races, 0) | coalesce((select 1 << race from __race_purshase where id=profile_id and race = 5), 0) where id in (select distinct profile_id from __race_purshase);
update wormswar.user_profile set races = coalesce(races, 0) | coalesce((select 1 << race from __race_purshase where id=profile_id and race = 6), 0) where id in (select distinct profile_id from __race_purshase);
update wormswar.user_profile set races = coalesce(races, 0) | coalesce((select 1 << race from __race_purshase where id=profile_id and race = 7), 0) where id in (select distinct profile_id from __race_purshase);
update wormswar.user_profile set races = coalesce(races, 0) | coalesce((select 1 << race from __race_purshase where id=profile_id and race = 8), 0) where id in (select distinct profile_id from __race_purshase);
update wormswar.user_profile set races = coalesce(races, 0) | coalesce((select 1 << race from __race_purshase where id=profile_id and race = 9), 0) where id in (select distinct profile_id from __race_purshase);
--Запрос успешно выполнен: 76745 строк изменено за 37321 мс.

ALTER TABLE achieve.worms_achievements ADD COLUMN buy_race smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN coliseum_win smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN coliseum_win_10 smallint;
ALTER TABLE achieve.worms_achievements ADD COLUMN with_friend_win smallint;

update achieve.worms_achievements set invested_award_points = 0 where invested_award_points > 0;
--Запрос успешно выполнен: 1021590 строк изменено за 172532 мс.  (3 минуты)

delete from wormswar.backpack_item_0 where weapon_id in (62,63,64,67,68,70,77,78,115);
--Запрос успешно выполнен: 932721 строк изменено за 44003 мс.
delete from wormswar.backpack_item_1 where weapon_id in (62,63,64,67,68,70,77,78,115);
delete from wormswar.backpack_item_2 where weapon_id in (62,63,64,67,68,70,77,78,115);
delete from wormswar.backpack_item_3 where weapon_id in (62,63,64,67,68,70,77,78,115);
--Запрос успешно выполнен: 936411 строк изменено за 145425 мс.
