CREATE TABLE _reaction4
(
  profile_id integer NOT NULL,
  value integer NOT NULL,
  CONSTRAINT _reaction4_pkey PRIMARY KEY (profile_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE _reaction4
  OWNER TO postgres;

with reaction as (
select profile_id,
   case
      when award_type = 1000 then 3
      when award_type = 2000 then 5
      when award_type = 3000 then 10

      when award_type = 1001 then 10
      when award_type = 2001 then 15

      when award_type = 1002 then 3
      when award_type = 2002 then 5
      when award_type = 3002 then 10

      when award_type = 1003 then 5
      when award_type = 2003 then 10
      when award_type = 3003 then 20

      when award_type = 2005 then 5
      when award_type = 3005 then 10

      when award_type = 1006 then 3
      when award_type = 3006 then 10

      when award_type = 3007 then 10

      when award_type = 2009 then 7
      when award_type = 3009 then 15

      when award_type = 2010 then 10
      when award_type = 3010 then 20

      when award_type = 1012 then 3
      when award_type = 3012 then 10

      when award_type = 1014 then 10
      when award_type = 3014 then 20

      when award_type = 1015 then 3
      when award_type = 3015 then 10

      when award_type = 1017 then 10
      when award_type = 2017 then 25
      when award_type = 3017 then 50

      when award_type = 2018 then 40
      when award_type = 3018 then 80

      when award_type = 1020 then 3
      when award_type = 2020 then 6
      when award_type = 3020 then 12

      when award_type = 2023 then 20

      when award_type = 3024 then 10

      when award_type = 1025 then 5

      when award_type = 2027 then 10

      when award_type = 3029 then 30

      when award_type = 1030 then 10
      when award_type = 2030 then 15
    else 0 end
    as value 
 from wormswar.award_statistic where date < '2014-10-23 17:01' and award_type >= 1000 and award_type <= 3031
)

insert into _reaction4 select profile_id, sum(value) from reaction group by profile_id;

with a as (
select profile_id, (r3.value - r2.value) as value from _reaction2 as r2
inner join _reaction3 as r3 using(profile_id)
where r3.value - r2.value > 0)

update wormswar.user_profile set reaction_rate = reaction_rate + (select value from a where profile_id = id) where id in (select profile_id from a)
