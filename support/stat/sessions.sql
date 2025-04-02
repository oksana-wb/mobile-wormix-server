with profiles as (
  select id, creation_date from wormswar.creation_date where
  creation_date >= now() - interval '3 MONTH' and
  creation_date < now() - interval '2 MONTH'
), session as (
select last_battle_time - last_login_time as period
 from wormswar.user_profile up
 inner join profiles on up.id = profiles.id
 where last_battle_time > last_login_time
)

select avg(period) from session