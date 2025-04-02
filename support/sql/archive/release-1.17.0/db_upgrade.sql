CREATE TABLE wormswar.referral_link
(
  id serial NOT NULL,
  token character varying(16) NOT NULL,
  start timestamp without time zone NOT NULL,
  finish timestamp without time zone,
  "limit" integer NOT NULL DEFAULT 0,
  ruby integer NOT NULL DEFAULT 0,
  fuzy integer NOT NULL DEFAULT 0,
  battles integer NOT NULL DEFAULT 0,
  reaction integer NOT NULL DEFAULT 0,
  reagents character varying(256) NOT NULL DEFAULT ''::character varying,
  weapons character varying(256) NOT NULL DEFAULT ''::character varying,
  visitors integer NOT NULL DEFAULT 0,
  CONSTRAINT referral_link_pkey PRIMARY KEY (id),
  CONSTRAINT referral_link_code_key UNIQUE (token)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.referral_link
  OWNER TO smos;


CREATE TABLE wormswar.referral_link_visit
(
  referral_link_id integer NOT NULL,
  profile_id integer NOT NULL,
  CONSTRAINT referral_link_visit_pkey PRIMARY KEY (referral_link_id, profile_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.referral_link_visit
  OWNER TO smos;

ALTER TABLE wormswar.worm_groups DROP CONSTRAINT worm_groups_team_member_1_fkey;
ALTER TABLE wormswar.worm_groups DROP CONSTRAINT worm_groups_team_member_2_fkey;
ALTER TABLE wormswar.worm_groups DROP CONSTRAINT worm_groups_team_member_3_fkey;
ALTER TABLE wormswar.worm_groups DROP CONSTRAINT worm_groups_team_member_4_fkey;
