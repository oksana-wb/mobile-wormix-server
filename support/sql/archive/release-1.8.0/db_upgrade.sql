ALTER TABLE wormswar.user_profile ALTER COLUMN armor TYPE SMALLINT;
ALTER TABLE wormswar.user_profile ALTER COLUMN attack TYPE SMALLINT;
ALTER TABLE wormswar.user_profile ALTER COLUMN experience TYPE SMALLINT;
ALTER TABLE wormswar.user_profile ALTER COLUMN level TYPE SMALLINT;
ALTER TABLE wormswar.user_profile ADD COLUMN race SMALLINT;
ALTER TABLE wormswar.user_profile ADD COLUMN kit SMALLINT;
ALTER TABLE wormswar.user_profile ADD COLUMN current_new_mission smallint;

ALTER TABLE wormswar.shop_statistic ADD COLUMN level smallint;
ALTER TABLE shop_statistic_parent ADD COLUMN level smallint;

-- из data удалить файлы
-- RatingService.dailyTop
-- RatingService.yesterdayTop

-- переносится на 1.9.0
with split as (
select id as profile_id,
case when hat < 50 then 0 else hat-(((hat-1000)/500)*500) end as hat,
case when hat < 50 then hat else ((hat-1000)/500) end as race
from wormswar.user_profile
)
update wormswar.user_profile set hat = (select hat from split where id=profile_id), race = (select race from split where id=profile_id);

update wormswar.user_profile set hat = (case when hat = 0 then race else hat + race * 500 end);
update wormswar.user_profile set race = null;

