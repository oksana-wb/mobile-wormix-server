-- Выполнять на сервере достижений

update achieve.worms_achievements set invested_award_points = 0 where invested_award_points > 0;

-- Выполнять на main сервере
delete from wormswar.backpack_item_0 where weapon_id in (62,63,64,67,68,70,77,78,115);
delete from wormswar.backpack_item_1 where weapon_id in (62,63,64,67,68,70,77,78,115);
delete from wormswar.backpack_item_2 where weapon_id in (62,63,64,67,68,70,77,78,115);
delete from wormswar.backpack_item_3 where weapon_id in (62,63,64,67,68,70,77,78,115);

