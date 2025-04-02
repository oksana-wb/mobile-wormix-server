ALTER TABLE wormswar.user_profile ADD COLUMN "name" CHARACTER VARYING(20);

ALTER TABLE wormswar.worm_groups
ADD COLUMN team_member_5 INTEGER,
ADD COLUMN team_member_6 INTEGER,
ADD COLUMN team_member_7 INTEGER,
ADD COLUMN team_member_meta_5 BYTEA,
ADD COLUMN team_member_meta_6 BYTEA,
ADD COLUMN team_member_meta_7 BYTEA,
ADD COLUMN extra_group_slots_count SMALLINT,
ADD COLUMN team_member_names CHARACTER VARYING;

ALTER TABLE stat.audit_admin_action ALTER COLUMN note TYPE CHARACTER VARYING;

CREATE TABLE wormswar.mercenaries
(
    profile_id   INTEGER  NOT NULL,
    open         BOOLEAN  NOT NULL DEFAULT FALSE,
    mercenary_01 SMALLINT NOT NULL DEFAULT 0,
    mercenary_02 SMALLINT NOT NULL DEFAULT 0,
    mercenary_03 SMALLINT NOT NULL DEFAULT 0,
    start_series TIMESTAMP WITHOUT TIME ZONE,
    win          SMALLINT NOT NULL DEFAULT 0,
    defeat       SMALLINT NOT NULL DEFAULT 0,
    draw         SMALLINT NOT NULL DEFAULT 0,
    num          INTEGER  NOT NULL DEFAULT 0,
    CONSTRAINT mercenaries_pkey PRIMARY KEY (profile_id)
)
WITH (
OIDS =FALSE
);
ALTER TABLE wormswar.mercenaries
OWNER TO smos;

