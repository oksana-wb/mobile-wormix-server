--скрипт закрытия сезона

-- определяем место клана
WITH total AS (SELECT id as clan_id, season_rating, size, ROW_NUMBER() OVER (ORDER BY season_rating desc) AS position FROM clan.clan)
insert into clan.season_total SELECT #seasonId#, clan_id, season_rating, size, position FROM total WHERE season_rating > 0;

-- заполняем сумму квадратов от рейтингов участников клана и количество участников с долей рейтинга > 0.5
WITH sum_sqrt AS (
select clan_id, sum(sqrt(season_rating)) as sum_sqrt_rating from clan.clan_member
where season_rating > 0
group by clan_id)
update clan.season_total set
    sum_sqrt_rating = coalesce((select sum_sqrt_rating from sum_sqrt where clan_id = season_total.clan_id), 0),
    awarded_size = GREATEST(1, (select count(*) from clan.clan_member where clan_id = season_total.clan_id and season_rating * 100::double precision / season_total.season_rating > 0.5))
where season_id = #seasonId#;

-- выставляем кланам их новое место
update clan.clan set prev_top_place = 0;
update clan.clan C set prev_top_place = (select place from clan.season_total where clan_id = C.id and season_id = #seasonId#)
   where c.id in (select clan_id from clan.season_total where season_id = #seasonId#);

-- считам награду мемберам
insert into clan.member_award
select season_id, social_id, profile_id, clan_id, false, rating, rank, medals, 0 as reaction, 0 as weapon from (
with total as (
select season_id, clan_id, size, awarded_size, clan_rating, sum_sqrt_rating, place, round(prize / 20 * awarded_size * 0.5) as half_prize, round(prize / 20 * awarded_size * 0.1) as leader_prize, round(prize / 20 * awarded_size * 0.4) as team_prize  from (
  select season_id, clan_id, T.size, T.awarded_size ,T.season_rating as clan_rating, sum_sqrt_rating, place,
  case
  when place = 1 then 1000
  when place = 2 then 900
  when place = 3 then 800
  when place = 4 then 700
  when place = 5 then 650
  when place > 5 and place <= 20 then 650 - (place - 5) * 10
  when place > 20 then 500 - (place - 20) * 5
  end as prize
  from clan.season_total T
  inner join clan.clan C on C.id = T.clan_id
   where place <= 100 and season_id = #seasonId#) as a
)
select season_id, social_id, profile_id, M.clan_id, false, season_rating as rating, rank,
  case rank
    when 1 then leader_prize + round(team_prize * sqrt(GREATEST(season_rating, 0)) / GREATEST(1, sum_sqrt_rating)) + (case when season_rating * 100::double precision / clan_rating > 0.5 then round(half_prize / awarded_size) else 0 end)
    else round(team_prize * sqrt(GREATEST(season_rating, 0)) / GREATEST(1, sum_sqrt_rating)) + (case when season_rating * 100::double precision / clan_rating > 0.5 then round(half_prize / awarded_size) else 0 end)
  end as medals
from clan.clan_member M
inner join total T on M.clan_id = T.clan_id
) as b;

-- чистим
update clan.clan set season_rating = 0, medal_price = 0, cashed_medals = 0;

update clan.clan_member set season_rating = 0, cashed_medals = 0, donation_prev_season = donation_curr_season, donation_prev_season_comeback = donation_curr_season_comeback, donation_curr_season = 0, donation_curr_season_comeback = 0;

truncate clan.clan_member_backup_rating;
-- truncate clan.auditclan.audit;

-- закрываем сезон
update clan.season set closed = true where id = #seasonId#;

