with profiles as (
  select id, creation_date from wormswar.creation_date where
  creation_date >= now() - interval '2 MONTH' and
  creation_date < now() - interval '1 MONTH'
), donaters as (
select count(distinct profile_id) as cnt from payment_statistic_parent where profile_id in (select id from profiles) and payment_status = 0
)

select to_char((select cnt from donaters) * 100::float / (select count(*)from profiles), '99D99');

-- контакт
with profiles as (
  select id, creation_date from wormswar.creation_date where
  creation_date >= now() - interval '3 MONTH' and
  creation_date < now() - interval '2 MONTH'
), donaters as (
select count(*) as cnt from _donaters where profile_id in (select id from profiles)
)

select to_char((select cnt from donaters) * 100::float / (select count(*)from profiles), '99D99')

--insert into _donaters (select distinct profile_id from payment_statistic_parent where date >= now() - interval '3 MONTH') and payment_status = 0
