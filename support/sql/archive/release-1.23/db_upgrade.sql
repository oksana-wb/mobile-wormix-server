ALTER TABLE achieve.worms_achievements ADD COLUMN posts_made smallint;

with a as (
select profile_id, case
  when item_id = 39 then 800
  when item_id = 5 then 950
  when item_id = 9 then 1100
end as money
from wormswar.shop_statistic SS
inner join wormswar.user_profile UP on UP.id = SS.profile_id
where date > now() - interval '14 DAYS' and item_type = 0 and item_id in (39, 5, 9) and UP.level >= 6
union
select profile_id, case
  when item_id = 39 then 800
  when item_id = 5 then 950
end as money
from wormswar.shop_statistic SS
inner join wormswar.user_profile UP on UP.id = SS.profile_id
where date > now() - interval '14 DAYS' and item_type = 0 and item_id in (39, 5) and UP.level in (4, 5)
union
select profile_id, 800 as money
from wormswar.shop_statistic SS
inner join wormswar.user_profile UP on UP.id = SS.profile_id
where date > now() - interval '14 DAYS' and item_type = 0 and item_id = 39 and UP.level = 3
),
b as (select profile_id, sum(money) as award from a
group by profile_id)

update wormswar.user_profile set money = money + (select award from b where profile_id = id)
where id in (select profile_id from b);
--Запрос успешно выполнен: 40419 строк изменено за 198236 мс.