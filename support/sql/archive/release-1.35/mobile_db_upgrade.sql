CREATE TABLE wormswar.restrictions (
  id serial NOT NULL,
  profile_id bigint NOT NULL,
  blocks smallint NOT NULL,
  start_date timestamp without time zone NOT NULL,
  end_date timestamp without time zone,
  reason integer NOT NULL,
  history character varying,
  CONSTRAINT restrictions_pkey PRIMARY KEY (id),
  CONSTRAINT restrictions_profile_id_fkey FOREIGN KEY (profile_id)
    REFERENCES wormswar.user_profile (id) MATCH SIMPLE
    ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.restrictions
    OWNER TO smos;