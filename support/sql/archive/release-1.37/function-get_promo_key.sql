CREATE OR REPLACE FUNCTION wormix.get_promo_key(
    _action smallint,
    _uid bigint)
  RETURNS character varying AS
$BODY$
DECLARE
    promo_key VARCHAR;
    i SMALLINT := 0;
BEGIN
    LOOP
        if i >= 3 THEN
            RAISE EXCEPTION 'Не удалось сгенерировать промо код для пользователя % по акции % после % попыток', _uid, _action, i;
        END IF;

        SELECT key FROM wormix.registered_keys WHERE action = _action AND uid = _uid
        INTO promo_key;

        IF length(promo_key) > 0 THEN
            RETURN promo_key;
        END IF;

        SELECT upper(substr(md5(random() :: TEXT), 1, 4) || '-' || substr(md5(random() :: TEXT), 1, 4) || '-' || substr(md5(random() :: TEXT), 1, 4))
        INTO promo_key;

        BEGIN
            INSERT INTO wormix.registered_keys (key, action, uid) VALUES (promo_key, _action, _uid);

            RETURN promo_key;
        EXCEPTION WHEN unique_violation THEN
            -- Do nothing, and loop to try the generate and INSERT again.
        END;
        i:= i + 1;
    END LOOP;
END
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION wormix.get_promo_key(smallint, bigint)
  OWNER TO promo_wormix;