SELECT
    report_type,
    dimension_value,
    total_conn,
    active_conn,
    idle_in_trans_conn,
    waiting_conn,
    query_duration,
    query_text
FROM (
         SELECT
             'OVERALL_SUMMARY' AS report_type,
             'ALL' AS dimension_value,
             COUNT(*) AS total_conn,
             COUNT(CASE WHEN state = 'active' THEN 1 END) AS active_conn,
             COUNT(CASE WHEN state = 'idle in transaction' THEN 1 END) AS idle_in_trans_conn,
             COUNT(CASE WHEN waiting = true THEN 1 END) AS waiting_conn,
             NULL::interval AS query_duration,
                 NULL::text AS query_text,
                 1 AS sort_priority
         FROM pgxc_stat_activity

         UNION ALL

         SELECT
             'CN_SUMMARY',
             coorname,
             COUNT(*),
             COUNT(CASE WHEN state = 'active' THEN 1 END),
             COUNT(CASE WHEN state = 'idle in transaction' THEN 1 END),
             COUNT(CASE WHEN waiting = true THEN 1 END),
             NULL::interval,
                 NULL::text,
                 2
         FROM pgxc_stat_activity
         GROUP BY coorname

         UNION ALL

         SELECT
             'USER_SUMMARY',
             usename,
             COUNT(*),
             COUNT(CASE WHEN state = 'active' THEN 1 END),
             COUNT(CASE WHEN state = 'idle in transaction' THEN 1 END),
             COUNT(CASE WHEN waiting = true THEN 1 END),
             NULL::interval,
                 NULL::text,
                 3
         FROM pgxc_stat_activity
         GROUP BY usename

         UNION ALL

         SELECT
             'IP_SUMMARY',
             client_addr::text,
                 COUNT(*),
             COUNT(CASE WHEN state = 'active' THEN 1 END),
             COUNT(CASE WHEN state = 'idle in transaction' THEN 1 END),
             COUNT(CASE WHEN waiting = true THEN 1 END),
             NULL::interval,
                 NULL::text,
                 4
         FROM pgxc_stat_activity
         WHERE client_addr IS NOT NULL
         GROUP BY client_addr

         UNION ALL

         SELECT
             'ACTIVE_QUERY',
             coorname || ' | ' || usename || ' | ' || client_addr::text,
                 1 AS total_conn,
             1 AS active_conn,
             0 AS idle_in_trans_conn,
             CASE WHEN waiting THEN 1 ELSE 0 END AS waiting_conn,
             now() - query_start AS query_duration,
             query AS query_text,
             5 AS sort_priority
         FROM pgxc_stat_activity
         WHERE state = 'active'
     ) AS t
ORDER BY
    sort_priority,
    query_duration DESC NULLS LAST,
    dimension_value;