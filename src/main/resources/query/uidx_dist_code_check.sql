SELECT
    n.nspname AS "模式名",
    c.relname AS "表名",
    i.indexrelid::regclass AS "索引名",
    (SELECT string_agg(a.attname, ', ' ORDER BY a.attnum)
     FROM pg_attribute a
     WHERE a.attrelid = c.oid
       AND a.attnum = ANY(STRING_TO_ARRAY(i.indkey::text, ' ')::int2[])
       AND a.attnum > 0) AS "索引列",
    array_length(STRING_TO_ARRAY(i.indkey::text, ' ')::int2[], 1) AS "列数",
    CASE WHEN EXISTS (
        SELECT 1 FROM pg_attribute a
        WHERE a.attrelid = c.oid
          AND a.attnum = ANY(STRING_TO_ARRAY(i.indkey::text, ' ')::int2[])
          AND a.attname = 'DIST_CODE'
    ) THEN '❌是' ELSE '✅否' END AS "是否包含DIST_CODE",
    obj_description(c.oid, 'pg_class') AS "表备注"
FROM pg_index i
JOIN pg_class c ON i.indrelid = c.oid
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE i.indisunique = true
  AND i.indisprimary = false
  AND i.indisvalid = true
  AND c.relkind = 'r'
  AND n.nspname IN (
    'yth_kf_jfzw_h',
    'yth_kf_schjy_h',
    'yth_kf_goud_h',
    'yth_kf_share_h',
    'yth_kf_yk_h',
    'yth_kf_kefu_h',
    'yth_kf_yj_h',
    'yth_kf_jl_h',
    'yth_kf_dlgyxq_h',
    'yth_kf_xs_h',
    'yth_kf_jc_h',
    'yth_kf_xtjc_h',
    'yth_kf_xtzc_h',
    'yth_kf_glback_h',
    'yth_kf_glbb_h',
    'yth_kf_glcx_h'
  )
  AND "是否包含DIST_CODE" LIKE '%是%'
ORDER BY "模式名", "表名", "索引名"