
ALTER TABLE clan.season ADD COLUMN start date;
ALTER TABLE clan.season ADD COLUMN finish date;
ALTER TABLE clan.season ADD COLUMN closed boolean NOT NULL DEFAULT false;

update clan.season set start='2013-08-01', finish='2013-09-01', closed=true where id = 1;
update clan.season set start='2013-09-01', finish='2013-10-01', closed=true where id = 2;

ALTER TABLE clan.season ALTER COLUMN start SET NOT NULL;
ALTER TABLE clan.season ALTER COLUMN finish SET NOT NULL;
ALTER TABLE clan.season  DROP COLUMN note;

insert into clan.season (id, start, finish) values (3, '2013-10-01', '2013-11-01');

create OR REPLACE view clan.clan_member_award as
select place, A.clan_id, profile_id, rating, rank, medal_count, reaction_count, weapon_count from clan.member_award A
inner join clan.season_total T on T.season_id = A.season_id and T.clan_id = A.clan_id
where T.season_id = (SELECT id from clan.season where closed order by id desc limit 1)
order by place, medal_count desc;