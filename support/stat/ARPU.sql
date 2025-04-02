-- vrontakte

CREATE TABLE _payments_cache
(
  date timestamp without time zone NOT NULL,
  profile_id bigint NOT NULL,
  votes integer NOT NULL,
  CONSTRAINT _payments_cache_pkey PRIMARY KEY (profile_id, date)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE _payments_cache
  OWNER TO postgres;

insert into _payments_cache (select date::date, profile_id, sum(votes) as votes from payment_statistic_parent where date >= now() - interval '3 MONTH' and payment_status = 0 group by 1, 2);

with profiles as (
  select id, creation_date from wormswar.creation_date where
  creation_date >= now() - interval '1 MONTH' and
  creation_date < now() - interval '0 MONTH'
), donaters as (
select profile_id, sum(votes) as votes from _payments_cache where profile_id in (select id from profiles) and
 date >= (select min(creation_date) from profiles) and date < (select max(creation_date) from profiles) + interval '1 MINUTE'
 group by 1
)

select to_char((select avg(votes) from donaters) / 30::float, '99D99');

with profiles as (
  select id, creation_date from wormswar.creation_date where
  creation_date >= now() - interval '1 MONTH' and
  creation_date < now() - interval '0 MONTH'
), donaters as (
select profile_id, sum(votes) as votes from _payments_cache where profile_id in (select id from profiles) and
 date >= (select min(creation_date) from profiles) and date < (select max(creation_date) from profiles) + interval '1 MINUTE'
 group by 1
)

select to_char((select avg(votes) from donaters), '99D99');

-- остальные --
with profiles as (
  select id, creation_date from wormswar.creation_date where
  creation_date >= now() - interval '1 MONTH' and
  creation_date < now() - interval '0 MONTH'
)

select to_char((select sum(votes) from payment_statistic_parent where profile_id in (select id from profiles) and payment_status = 0) / (select count(*) from profiles)::float / 30::float, '99D999')
union
select to_char((select sum(votes) from payment_statistic_parent where profile_id in (select id from profiles) and payment_status = 0) / (select count(*) from profiles)::float, '99D999');

-------- LTV --------------------------------------------

with profiles as (
  select creation_date, last_battle_time from wormswar.creation_date CD
  inner join wormswar.user_profile UP on UP.id = CD.id where
      creation_date >= now() - interval '1 MONTH' and
      creation_date < now() - interval '0 MONTH'
)

select to_char((extract(day from avg(last_battle_time - creation_date)) * 24 + extract(hour from avg(last_battle_time - creation_date))) / 30 / 24, '99D99') from profiles