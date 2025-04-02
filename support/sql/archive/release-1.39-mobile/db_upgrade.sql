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

ALTER TABLE wormswar.user_profile ADD COLUMN rename_vip_act smallint;

with a as (
    SELECT id FROM wormswar.user_profile WHERE vip_expiry_time > now()
), b as(
    select profile_id, item from payment_statistic_parent where (item like '%vip_30%' or item like 'vip30%') and profile_id in (select id from a)
)
update wormswar.user_profile set rename_vip_act = 2 WHERE id in (SELECT profile_id from b);

