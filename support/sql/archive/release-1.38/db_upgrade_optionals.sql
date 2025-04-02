-- пробуем тип jsonb

ALTER TABLE wormswar.quest_progress RENAME TO quest_progress_bak;

CREATE TABLE wormswar.quest_progress
(
  profile_id bigint NOT NULL,
  q1 jsonb,
  q2 jsonb,
  q3 jsonb
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.quest_progress
  OWNER TO smos;

insert into wormswar.quest_progress select profile_id, q1::jsonb, q2::jsonb, q3::jsonb from wormswar.quest_progress_bak;

--DROP TABLE wormswar.quest_progress_bak;
ALTER TABLE wormswar.quest_progress_bak DROP CONSTRAINT quest_progress_pkey;
ALTER TABLE wormswar.quest_progress_bak DROP CONSTRAINT quest_progress_profile_id_fkey;

ALTER TABLE wormswar.quest_progress ADD CONSTRAINT quest_progress_pkey PRIMARY KEY(profile_id);

ALTER TABLE wormswar.quest_progress
  ADD CONSTRAINT quest_progress_profile_id_fkey FOREIGN KEY (profile_id)
      REFERENCES wormswar.user_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE;
