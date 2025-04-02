--pgsql -a -f truncate_backpack wormswar

select now();
COPY (SELECT * FROM wormswar.backpack_item_0 WHERE weapon_id > 4) TO '/home/postgres/truncate/live_backpack_item_0.copy';
COPY (SELECT * FROM wormswar.backpack_item_1 WHERE weapon_id > 4) TO '/home/postgres/truncate/live_backpack_item_1.copy';
COPY (SELECT * FROM wormswar.backpack_item_2 WHERE weapon_id > 4) TO '/home/postgres/truncate/live_backpack_item_2.copy';
COPY (SELECT * FROM wormswar.backpack_item_3 WHERE weapon_id > 4) TO '/home/postgres/truncate/live_backpack_item_3.copy';

truncate wormswar.backpack_item_0;
truncate wormswar.backpack_item_1;
truncate wormswar.backpack_item_2;
truncate wormswar.backpack_item_3;

select now();
COPY wormswar.backpack_item_0 FROM '/home/postgres/truncate/live_backpack_item_0.copy';
select now();
COPY wormswar.backpack_item_1 FROM '/home/postgres/truncate/live_backpack_item_1.copy';
select now();
COPY wormswar.backpack_item_2 FROM '/home/postgres/truncate/live_backpack_item_2.copy';
select now();
COPY wormswar.backpack_item_3 FROM '/home/postgres/truncate/live_backpack_item_3.copy';

--select * from pg_stat_activity where datname = 'wormswar'