CREATE TABLE wormswar.bundles
(
  id serial NOT NULL,
  code character varying(255) NOT NULL,
  discount integer NOT NULL,
  votes numeric NOT NULL,
  items character varying(255) DEFAULT ''::character varying,
  race integer NOT NULL DEFAULT (-1),
  disabled boolean NOT NULL DEFAULT false,
  start timestamp without time zone,
  finish timestamp without time zone,
  create_date timestamp without time zone NOT NULL DEFAULT now(),
  update_date timestamp without time zone,
  CONSTRAINT bundles_pkey PRIMARY KEY (id),
  CONSTRAINT bundles_code_key UNIQUE (code)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE wormswar.bundles
  OWNER TO smos;
