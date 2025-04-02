CREATE TABLE _reaction3
(
  profile_id integer NOT NULL,
  value integer NOT NULL,
  CONSTRAINT _reaction3_pkey PRIMARY KEY (profile_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE _reaction3
  OWNER TO postgres;

with reaction as (
select profile_id,
 case
   when burned_enemies >=50 and burned_enemies < 200 then 3
   when burned_enemies >=200 and burned_enemies < 500 then 8
   when burned_enemies >=500 then 18
 else 0 end +
 case
   when destroyed_square >=1000 and destroyed_square < 10000 then 10
   when destroyed_square >=10000 and destroyed_square < 30000 then 25
 else 0 end +
 case
   when drowned_opponents >=50 and drowned_opponents < 250 then 3
   when drowned_opponents >=250 and drowned_opponents < 600 then 8
   when drowned_opponents >=600 then 18
 else 0 end +
 case
   when wager_winner >=50 and wager_winner < 400 then 5
   when wager_winner >=400 and wager_winner < 1000 then 15
   when wager_winner >=1000 then 35
 else 0 end +
 case
   when wind_kills >=75 and wind_kills < 200 then 5
   when wind_kills >=200 then 15
 else 0 end +
 case
   when massive_damage >=20 and massive_damage < 100 then 3
   when massive_damage >=250 then 13
 else 0 end +
 case
   when graves_sank >=200 then 10
 else 0 end +
 case
   when immobile_kills >=50 and immobile_kills < 150 then 7
   when immobile_kills >=150 then 22
 else 0 end +
 case
   when zero_looses_victory >=250 and zero_looses_victory < 1000 then 10
   when zero_looses_victory >=1000 then 30
 else 0 end +
 case
   when double_killls >=20 and double_killls < 100 then 3
   when double_killls >=250 then 13
 else 0 end +
 case
   when gathered_supplies >=100 and gathered_supplies < 350 then 10
   when gathered_supplies >=800 then 30
 else 0 end +
 case
   when kamikaze >=25 and kamikaze < 100 then 3
   when kamikaze >=250 then 13
 else 0 end +
 case
   when fuzzes_spent >=10000 and fuzzes_spent < 25000 then 10
   when fuzzes_spent >=25000 and fuzzes_spent < 60000 then 35
   when fuzzes_spent >=60000 then 85
 else 0 end +
 case
   when rubies_spent >=300 and rubies_spent < 800 then 40
   when rubies_spent >=800 then 120
 else 0 end +
 case
   when rubies_found >=10 and rubies_found < 25 then 3
   when rubies_found >=25 and rubies_found < 50 then 9
   when rubies_found >=50 then 21
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
   when keymaster >=80 and keymaster < 300 then 25
 else 0 end
 as value
 from achieve.worms_achievements
), reaction_value as (
select social.profile_id, value from reaction
inner join wormswar.social_id as social on reaction.profile_id = string_id
where value > 0
)
insert into _reaction3 select * from reaction_value;

with a as (
select profile_id, (r3.value - r2.value) as value from _reaction2 as r2
inner join _reaction3 as r3 using(profile_id)
where r3.value - r2.value > 0)

update wormswar.user_profile set reaction_rate = reaction_rate + (select value from a where profile_id = id) where id in (select profile_id from a)
