/*
select weapon_id, count(*) from  wormswar.backpack_item_0 where weapon_id in (84, 85, 86)
group by 1
order by 1
*/

delete from  wormswar.backpack_item_0 where weapon_id = 84;
delete from  wormswar.backpack_item_0 where weapon_id = 85;
delete from  wormswar.backpack_item_0 where weapon_id = 86;

delete from  wormswar.backpack_item_1 where weapon_id = 84;
delete from  wormswar.backpack_item_1 where weapon_id = 85;
delete from  wormswar.backpack_item_1 where weapon_id = 86;

delete from  wormswar.backpack_item_2 where weapon_id = 84;
delete from  wormswar.backpack_item_2 where weapon_id = 85;
delete from  wormswar.backpack_item_2 where weapon_id = 86;

delete from  wormswar.backpack_item_3 where weapon_id = 84;
delete from  wormswar.backpack_item_3 where weapon_id = 85;
delete from  wormswar.backpack_item_3 where weapon_id = 86;