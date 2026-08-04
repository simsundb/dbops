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
             report_type,
             dimension_value,
             total_conn,
             active_conn,
             idle_in_trans_conn,
             waiting_conn,
             query_duration,
             query_text,
             sort_priority,
             ROW_NUMBER() OVER (
                 PARTITION BY report_type
                 ORDER BY
                     CASE WHEN report_type = 'OVERALL_SUMMARY' THEN 0 ELSE total_conn END DESC,
                     query_duration DESC NULLS LAST
                 ) AS rn
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
                      1,
                      1,
                      0,
                      CASE WHEN waiting THEN 1 ELSE 0 END,
                      now() - query_start,
                      query,
                      5
                  FROM pgxc_stat_activity
                  WHERE state = 'active'
              ) AS base
     ) AS ranked
WHERE rn <= 5
ORDER BY
    sort_priority,
    rn;