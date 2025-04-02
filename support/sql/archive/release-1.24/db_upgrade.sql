-- Гладиаторская арена
CREATE TABLE wormswar.coliseum
(
    profile_id   INTEGER                     NOT NULL,
    open         BOOLEAN,
    num          INTEGER,
    data         BYTEA,
    create_date  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    start_series TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT coliseum_pkey PRIMARY KEY (profile_id)
)
WITH (
OIDS =FALSE
);
ALTER TABLE wormswar.coliseum
OWNER TO smos;

-- Статистика действий в клане
DELETE FROM clan.audit
WHERE action = 16;

CREATE INDEX ON clan.audit (date DESC NULLS FIRST);

--возврат стоимости веревки (id=49, 2000 фузов), если покупали за 2 недели на уровне >= 8
WITH b AS (
    SELECT
        profile_id,
        2000 AS award
    FROM wormswar.shop_statistic SS
        INNER JOIN wormswar.user_profile UP ON UP.id = SS.profile_id
    WHERE date > now() - INTERVAL '14 DAYS' AND item_type = 0 AND item_id = 49 AND UP.level >= 8
)

UPDATE wormswar.user_profile
SET money = money + (SELECT award
                     FROM b
                     WHERE profile_id = id)
WHERE id IN (SELECT profile_id
             FROM b);
