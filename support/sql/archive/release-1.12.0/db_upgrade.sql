ALTER TABLE clan.clan ADD COLUMN prev_top_place integer NOT NULL DEFAULT 0;

CREATE TABLE clan.season
(
  id integer NOT NULL,
  note character varying NOT NULL,
  CONSTRAINT season_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE clan.season
  OWNER TO smos;

insert into clan.season (id, note) values (1, '01.08.2013 - 01.09.2013');

CREATE TABLE clan.season_total
(
  season_id integer NOT NULL,
  clan_id integer NOT NULL,
  season_rating integer NOT NULL,
  size integer NOT NULL,
  place integer NOT NULL,
  CONSTRAINT season_total_pkey PRIMARY KEY (season_id, clan_id),
  CONSTRAINT season_total_season_id_fkey FOREIGN KEY (season_id)
      REFERENCES clan.season (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE clan.season_total
  OWNER TO smos;

WITH total AS (
    SELECT id as clan_id, season_rating, size,
           ROW_NUMBER() OVER(ORDER BY season_rating desc) AS position
      FROM clan.clan)
 insert into clan.season_total SELECT 1, clan_id, season_rating, size, position FROM total WHERE season_rating > 0;

update clan.clan set prev_top_place = 0;
update clan.clan C set prev_top_place = (select place from clan.season_total where clan_id = C.id and season_id = 1)
    where c.id in (select clan_id from clan.season_total where season_id = 1);

CREATE TABLE clan.member_award
(
   season_id integer NOT NULL,
   social_id smallint NOT NULL,
   profile_id integer NOT NULL,
   clan_id integer NOT NULL,
   granted boolean NOT NULL DEFAULT false,
   rating integer NOT NULL,
   rank smallint NOT NULL,
   medal_count integer NOT NULL,
   reaction_count smallint NOT NULL,
   PRIMARY KEY (season_id, social_id, profile_id)
)
WITH (
  OIDS = FALSE
);
ALTER TABLE clan.member_award
  OWNER TO smos;

insert into clan.member_award
select season_id, social_id, profile_id, clan_id, false, rating, rank, medals, medals * 15 as reaction from (
with total as (
select season_id, clan_id, size, clan_rating, place, round(prize / 50 * size) as total_prize, round(prize / 50 * size * 0.1) as leader_prize, round(prize / 50 * size * 0.9) as team_prize  from (
  select season_id, clan_id, T.size, T.season_rating as clan_rating, place,
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
   where place <= 100 and season_id = 1) as a
)
--select M.clan_id, place, social_id, profile_id, rank, season_rating, season_rating::float * 100 / clan_rating as share , total_prize, leader_prize, team_prize,
select season_id, social_id, profile_id, M.clan_id, false, season_rating as rating, rank,
  case rank
    when 1 then leader_prize + round(team_prize * GREATEST(season_rating, 0) / clan_rating)
    else round(team_prize * GREATEST(season_rating, 0) / clan_rating)
  end as medals
from clan.clan_member M
inner join total T on M.clan_id = T.clan_id) as b;

update clan.clan set season_rating = 0;
update clan.clan_member set season_rating = 0;

create OR REPLACE view clan.clan_member_award as
select place, A.clan_id, profile_id, rating, rank, medal_count, reaction_count from clan.member_award A
inner join clan.season_total T on T.season_id = A.season_id and T.clan_id = A.clan_id
where T.season_id = 1
order by place, medal_count desc;

--select * from clan.clan_member_award
