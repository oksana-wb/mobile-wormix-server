select * from (
    select 1, '10-50', count(*)/100 from wormswar.user_profile where rating >= 10 and rating < 50 and last_login_time  > now() - INTERVAL '365 DAYS'
    union
    select 2, '50-200', count(*)/100 from wormswar.user_profile where rating >= 50 and rating < 200 and last_login_time  > now() - INTERVAL '365 DAYS'
    union
    select 3, '200-500', count(*)/100 from wormswar.user_profile where rating >= 200 and rating < 500 and last_login_time  > now() - INTERVAL '365 DAYS'
    union
    select 4, '500-2000', count(*)/100 from wormswar.user_profile where rating >= 500 and rating < 2000 and last_login_time  > now() - INTERVAL '365 DAYS'
    union
    select 5, '2000-10000', count(*)/100 from wormswar.user_profile where rating >= 2000 and rating < 10000 and last_login_time  > now() - INTERVAL '365 DAYS'
    union
    select 6, '>10000', count(*)/100 from wormswar.user_profile where rating >= 10000 and last_login_time  > now() - INTERVAL '365 DAYS'
) as a order by 1
