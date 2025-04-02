WITH a AS (
    SELECT
        clan_id,
        sum(param) AS donate FROM clan.audit
    WHERE publisher_id IN (391726860) AND action = 14
    GROUP BY 1
    ORDER BY 2 DESC
), b AS (
    SELECT
        clan_id,
        treas,
        donate,
        CASE WHEN donate > treas THEN treas ELSE donate END AS refund FROM a
        INNER JOIN clan.clan C ON c.id = a.clan_id
)
--select clan_id, refund from b;
SELECT sum(refund) / 390 * 100 FROM b;

select count(distinct publisher_id) from clan.audit where clan_id = 39506;

select action, count(*) from clan.audit where clan_id = 11148 group by 1 order by 2 desc;
