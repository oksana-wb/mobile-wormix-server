ALTER TABLE clan.clan_member ADD COLUMN expel_permit boolean NOT NULL DEFAULT true;
ALTER TABLE clan.clan_member ADD COLUMN mute_mode boolean NOT NULL DEFAULT false;

DROP TABLE wormswar.quest_progress;

CREATE TABLE wormswar.quest_progress
(
  profile_id bigint NOT NULL,
  q1 character varying,
  q2 character varying,
  CONSTRAINT quest_progress_pkey PRIMARY KEY (profile_id),
  CONSTRAINT quest_progress_profile_id_fkey FOREIGN KEY (profile_id)
      REFERENCES wormswar.user_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.quest_progress
  OWNER TO smos;

