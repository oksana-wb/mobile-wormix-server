with profiles as (
  select creation_date, last_battle_time from wormswar.creation_date CD
  inner join wormswar.user_profile UP on UP.id = CD.id where
      creation_date >= now() - interval '3 MONTH' - interval '7 DAY' and
      creation_date < now() - interval '2 MONTH' - interval '7 DAY'
), retention as (
select '1 day' as period from profiles where last_battle_time - creation_date >= interval '1 DAY' union all
select '2 day' as period from profiles  where last_battle_time - creation_date >= interval '2 DAY' union all
select '3 day' as period from profiles  where last_battle_time - creation_date >= interval '3 DAY' union all
select '4 day' as period from profiles  where last_battle_time - creation_date >= interval '4 DAY' union all
select '5 day' as period from profiles  where last_battle_time - creation_date >= interval '5 DAY' union all
select '6 day' as period from profiles  where last_battle_time - creation_date >= interval '6 DAY' union all
select '7 day' as period from profiles  where last_battle_time - creation_date >= interval '7 DAY' union all
select '9 1 MONTH' as period from profiles where last_battle_time - creation_date >= interval '1 MONTH' union all
select '9 2 MONTH' as period from profiles where last_battle_time - creation_date >= interval '2 MONTH' union all
select '9 3 MONTH' as period from profiles where last_battle_time - creation_date >= interval '3 MONTH' union all
select '< day' as period from profiles where last_battle_time - creation_date < interval '1 DAY'
)
select period, to_char(count(*) * 100::float / (select count(*) from profiles), '99D9') from retention
group by 1
order by 1