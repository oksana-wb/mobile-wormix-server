CREATE EXTENSION dblink;

DO
$$
DECLARE
    v_count         BIGINT;
    v_sql           TEXT;
    v_update_status TEXT;
BEGIN
    FOR year IN 2014..2015 LOOP
        RAISE NOTICE '%', year;
        FOR month IN 1..12 LOOP
            SELECT count(*) FROM donaters WHERE extract('YEAR' FROM max_payment_date) = year AND extract('MONTH' FROM max_payment_date) = month INTO v_count;
            RAISE NOTICE '% %: %', to_char(timeofday()::timestamp, 'HH24:MI:SS'), month, v_count;
            v_sql := 'with a as (select * from donaters where extract(''YEAR'' from max_payment_date) = ' || year || ' and extract(''MONTH'' from max_payment_date) = ' || month || ')' ||
                     'update wormswar.user_profile set last_payment_date = (select max_payment_date from a where profile_id = id) where id in (select profile_id from a)';
            v_update_status := dblink_exec('dbname=wormswar user=smos', v_sql);
            RAISE NOTICE '% %', to_char(timeofday()::timestamp, 'HH24:MI:SS'), v_update_status;
        END LOOP;
    END LOOP;
END
$$;

-- корректировка
select min(last_payment_date) from wormswar.user_profile where last_payment_date > 'TODAY';

with a as (
    select max(date) as max_payment_date, profile_id  from wormswar.payment_statistic where date >= (select min(last_payment_date) from wormswar.user_profile where last_payment_date > 'TODAY')
    group by profile_id
)
update wormswar.user_profile set last_payment_date = (select max_payment_date from a where profile_id = id) where id in (select profile_id from a)
