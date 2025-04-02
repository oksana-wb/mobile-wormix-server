ALTER TABLE wormswar.social_id ADD UNIQUE (social_net_id, profile_id);

select count(*) from wormswar.social_id;

with a as (
select max(string_id) as string_id, social_net_id, profile_id, count(*) from wormswar.social_id
group by social_net_id, profile_id
having count(*) > 1
)

delete from wormswar.social_id b where string_id in (select string_id from a );

-- не забыть удалить файл data/SocialUserIdMapService.profilesByStringIdMap