update achieve.worms_achievements set pumped_reaction = 0 where pumped_reaction  > 0;
--Запрос успешно выполнен: 1191799 строк изменено за 40303 мс.
update wormswar.user_profile set reaction_rate = 0 where reaction_rate > 0;
--Запрос успешно выполнен: 520454 строк изменено за 27120 мс.

--DROP TABLE _reaction;
CREATE TABLE _reaction
(
  profile_id integer NOT NULL,
  value integer NOT NULL,
  CONSTRAINT _reaction_pkey PRIMARY KEY (profile_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE _reaction
  OWNER TO postgres;

--DROP TABLE _reaction2;
CREATE TABLE _reaction2
(
  profile_id integer NOT NULL,
  value integer NOT NULL,
  CONSTRAINT _reaction2_pkey PRIMARY KEY (profile_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE _reaction2
  OWNER TO postgres;


insert into _reaction select profile_id, sum(count * 3) as value from wormswar.shop_statistic where item_type = 10
group by 1;

update wormswar.user_profile set reaction_rate = reaction_rate + (select value from _reaction where profile_id = id)
where id in (select profile_id from _reaction);

with reaction as (
select profile_id,
 case
   when burned_enemies >=50 and burned_enemies < 200 then 3
   when burned_enemies >=200 and burned_enemies < 500 then 5
   when burned_enemies >=500 then 10
 else 0 end +
 case
   when destroyed_square >=1000 and destroyed_square < 10000 then 10
   when destroyed_square >=10000 and destroyed_square < 30000 then 15
 else 0 end +
 case
   when drowned_opponents >=50 and drowned_opponents < 250 then 3
   when drowned_opponents >=250 and drowned_opponents < 600 then 5
   when drowned_opponents >=600 then 10
 else 0 end +
 case
   when wager_winner >=50 and wager_winner < 400 then 5
   when wager_winner >=400 and wager_winner < 1000 then 10
   when wager_winner >=1000 then 20
 else 0 end +
 case
   when wind_kills >=75 and wind_kills < 200 then 5
   when wind_kills >=200 then 10
 else 0 end +
 case
   when massive_damage >=20 and massive_damage < 100 then 3
   when massive_damage >=250 then 10
 else 0 end +
 case
   when graves_sank >=200 then 10
 else 0 end +
 case
   when immobile_kills >=50 and immobile_kills < 150 then 7
   when immobile_kills >=150 then 15
 else 0 end +
 case
   when zero_looses_victory >=250 and zero_looses_victory < 1000 then 10
   when zero_looses_victory >=1000 then 20
 else 0 end +
 case
   when double_killls >=20 and double_killls < 100 then 3
   when double_killls >=250 then 10
 else 0 end +
 case
   when gathered_supplies >=100 and gathered_supplies < 350 then 10
   when gathered_supplies >=800 then 20
 else 0 end +
 case
   when kamikaze >=25 and kamikaze < 100 then 3
   when kamikaze >=250 then 10
 else 0 end +
 case
   when fuzzes_spent >=10000 and fuzzes_spent < 25000 then 10
   when fuzzes_spent >=25000 and fuzzes_spent < 60000 then 25
   when fuzzes_spent >=60000 then 50
 else 0 end +
 case
   when rubies_spent >=300 and rubies_spent < 800 then 40
   when rubies_spent >=800 then 80
 else 0 end +
 case
   when rubies_found >=10 and rubies_found < 25 then 3
   when rubies_found >=25 and rubies_found < 50 then 6
   when rubies_found >=50 then 12
 else 0 end +
 case
   when game_visits >=10 and game_visits < 20 then 20
 else 0 end +
 case
   when idol >=200 then 10
 else 0 end +
 case
   when inquisitor >=15 and inquisitor < 50 then 5
 else 0 end +
 case
   when partisan >=20 and partisan < 50 then 10
 else 0 end +
 case
   when drop_water_first_turn >=100 then 30
 else 0 end +
 case
   when keymaster >=20 and keymaster < 80 then 10
   when keymaster >=80 and keymaster < 300 then 15
 else 0 end
 as value
 from achieve.worms_achievements
), reaction_value as (
select social.profile_id, value from reaction
inner join wormswar.social_id as social on reaction.profile_id = string_id
where value > 0
)
insert into _reaction2 select * from reaction_value;

update wormswar.user_profile set reaction_rate = reaction_rate + (select value from _reaction2 where profile_id = id)
where id in (select profile_id from _reaction2);