with r as (
select profile_id,
case
when amount = 10 then 1
when amount = 27 then 3
when amount = 55 then 10
when amount = 112 then 28
when amount = 285 then 90
when amount = 600 then 200
end as real_money
from payment_statistic_parent
where completed and payment_status = 0 and date > '2016-02-17' and amount != 5 and money_type = 0
), rr as (
select profile_id, sum(real_money) as real_money, 0 as money from r group by 1 order by 1
), m as (
select profile_id,
case
when amount = 1000 then 100
when amount = 2700 then 300
when amount = 5500 then 1000
when amount = 11200 then 2800
when amount = 28500 then 9000
when amount = 60000 then 20000
end as money
from payment_statistic_parent
where completed and payment_status = 0 and date > '2016-02-17' and amount != 500 and money_type = 1
), mm as (
select profile_id, 0 as real_money, sum(money) as money from m group by 1 order by 1
), t as (
select * from rr
union
select * from mm
), tt as (
select profile_id, sum(real_money) as real_money, sum(money) as money from t group by 1 order by 1
)

update wormswar.user_profile set
realmoney = realmoney + (select real_money from tt where id = profile_id),
money = money + (select money from tt where id = profile_id)
where id in (select profile_id from tt)

