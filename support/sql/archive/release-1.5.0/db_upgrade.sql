-- новая таблица хранения команды
CREATE TABLE wormswar.worm_groups
(
  profile_id integer NOT NULL,
  team_member_1 integer NOT NULL,
  team_member_2 integer NOT NULL,
  team_member_3 integer,
  team_member_4 integer,
  CONSTRAINT worm_groups_pkey PRIMARY KEY (profile_id ),
  CONSTRAINT worm_groups_profile_id_fkey FOREIGN KEY (profile_id)
      REFERENCES wormswar.user_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT worm_groups_team_member_1_fkey FOREIGN KEY (team_member_1)
      REFERENCES wormswar.user_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT worm_groups_team_member_2_fkey FOREIGN KEY (team_member_2)
      REFERENCES wormswar.user_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT worm_groups_team_member_3_fkey FOREIGN KEY (team_member_3)
      REFERENCES wormswar.user_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT worm_groups_team_member_4_fkey FOREIGN KEY (team_member_4)
      REFERENCES wormswar.user_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.worm_groups
  OWNER TO smos;

insert into wormswar.worm_groups
select  profile_id, profile_id, arr[1], arr[2], arr[3] from (
select profile_id, array_agg(owner_profile_id) as arr from (
select profile_id, owner_profile_id from wormswar.worm_group
) as a
group by profile_id
) as b;

--новые достижения
ALTER TABLE achieve.worms_achievements ADD COLUMN partisan smallint NOT NULL DEFAULT 0;
ALTER TABLE achieve.worms_achievements ADD COLUMN collector smallint NOT NULL DEFAULT 0;

--новые боссы
update wormswar.user_profile set current_mission = current_mission + 3 where current_mission > 0;