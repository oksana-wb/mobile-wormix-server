CREATE TABLE wormswar.cookies
(
   profile_id integer NOT NULL,
   values_as_json character varying NOT NULL,
   PRIMARY KEY (profile_id),
   FOREIGN KEY (profile_id) REFERENCES wormswar.user_profile (id) ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS = FALSE
)
;
ALTER TABLE wormswar.cookies
  OWNER TO smos;