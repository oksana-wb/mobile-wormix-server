-- триггер на успешный платеж
ALTER TABLE wormswar.user_profile
    ADD COLUMN last_payment_date TIMESTAMP WITHOUT TIME ZONE;

CREATE OR REPLACE FUNCTION wormswar.payment_statistic_success_trigger()
    RETURNS TRIGGER AS
$BODY$
BEGIN
    IF (NEW.completed AND NEW.payment_status = 0) THEN
        UPDATE wormswar.user_profile SET last_payment_date = now() WHERE id = NEW.profile_id;
    END IF;
    RETURN NEW;
END;
$BODY$
LANGUAGE plpgsql VOLATILE
COST 100;
ALTER FUNCTION wormswar.payment_statistic_success_trigger()
OWNER TO smos;

CREATE TRIGGER payment_statistic_after_success
AFTER INSERT OR UPDATE
    ON wormswar.payment_statistic
FOR EACH ROW
EXECUTE PROCEDURE wormswar.payment_statistic_success_trigger();

-- донаты

CREATE TABLE public.donaters
(
    profile_id       BIGINT NOT NULL,
    max_payment_date TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT donaters_pkey PRIMARY KEY (profile_id)
)
WITH (
OIDS = FALSE
);
ALTER TABLE public.donaters
    OWNER TO smos;

INSERT INTO donaters SELECT
                         profile_id,
                         max(date) FROM public.payment_statistic_parent
                     WHERE completed AND (payment_status IS NULL OR payment_status = 0) GROUP BY 1;

CREATE INDEX donaters_max_payment_date_idx
    ON public.donaters
    USING BTREE
    (max_payment_date);
