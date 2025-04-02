ALTER TABLE public.payment_statistic_parent ALTER COLUMN transaction_id TYPE character varying(256);

ALTER TABLE wormswar.social_id RENAME id TO profile_id;
ALTER TABLE wormswar.social_id ADD COLUMN social_net_id smallint;

CREATE TABLE wormswar.notify_registration
(
  profile_id bigint NOT NULL,
  registration_date timestamp without time zone NOT NULL,
  registration_id character varying(255) NOT NULL,
  social_net_id smallint NOT NULL,
  unregistration_date timestamp without time zone,
  CONSTRAINT notify_registration_pkey PRIMARY KEY (profile_id),
  CONSTRAINT notify_registration_registration_id_key UNIQUE (registration_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.notify_registration
  OWNER TO smos;

