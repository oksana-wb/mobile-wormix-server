ALTER TABLE public.payment_statistic_parent ALTER COLUMN item TYPE character varying;

CREATE TABLE wormswar.deposits
(
  id integer NOT NULL,
  profile_id integer NOT NULL,
  dividends_by_days character varying NOT NULL,
  start_date timestamp without time zone NOT NULL,
  progress smallint NOT NULL DEFAULT 0,
  last_pay_date timestamp without time zone,
  paid_off boolean NOT NULL DEFAULT false,
  CONSTRAINT deposits_pkey PRIMARY KEY (id),
  CONSTRAINT deposits_profile_id_fkey FOREIGN KEY (profile_id)
      REFERENCES wormswar.user_profile (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.deposits
    OWNER TO smos;
CREATE INDEX deposits_profile_id_paid_off ON wormswar.deposits USING BTREE (profile_id, paid_off);
