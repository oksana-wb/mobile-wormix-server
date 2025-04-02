ALTER TABLE payment_statistic_parent ADD COLUMN level smallint;

CREATE OR REPLACE FUNCTION wormswar.payment_statistic_before_insert_trigger()
  RETURNS trigger AS
$BODY$
BEGIN
    select level into NEW.level from wormswar.user_profile where id = NEW.profile_id;
    RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION wormswar.payment_statistic_before_insert_trigger()
  OWNER TO postgres;

CREATE TRIGGER payment_statistic_before_insert BEFORE INSERT
   ON wormswar.payment_statistic FOR EACH ROW
   EXECUTE PROCEDURE wormswar.payment_statistic_before_insert_trigger();


ALTER TABLE wormswar.bundles  ADD COLUMN races character varying(64) NOT NULL DEFAULT '';
ALTER TABLE wormswar.bundles  ADD COLUMN skins character varying(64) NOT NULL DEFAULT '';
