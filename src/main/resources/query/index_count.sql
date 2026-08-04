SELECT
    i.schemaname          AS "模式名",
    i.tablename           AS "表名",
    COUNT(*)              AS "索引数量",
    LISTAGG(i.indexname, ', ') WITHIN GROUP (ORDER BY i.indexname) AS "索引列表",
    obj_description(c.oid, 'pg_class') AS "表备注"
FROM pg_indexes i
JOIN pg_class c ON c.relname = i.tablename
JOIN pg_namespace n ON n.nspname = i.schemaname AND n.oid = c.relnamespace
WHERE i.schemaname IN (
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
GROUP BY i.schemaname, i.tablename, c.oid
HAVING COUNT(*) > 7
ORDER BY COUNT(*) DESC, i.tablename