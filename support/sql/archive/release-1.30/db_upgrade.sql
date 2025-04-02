CREATE TABLE wormswar.quest_progress
(
  profile_id bigint NOT NULL,
  q1_start_date timestamp without time zone,
  q1_win smallint NOT NULL DEFAULT 0,
  q1_finished_date timestamp without time zone,
  q1_rewarded boolean NOT NULL DEFAULT false,
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