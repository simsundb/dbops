SELECT
    n.nspname AS "模式名",          -- 模式名称
    c.relname AS "表名",           -- 表名称
    COUNT(*) AS "索引总数",         -- 索引总数
    SUM(CASE WHEN i.indisprimary THEN 1 ELSE 0 END) AS "主键索引数", -- 主键索引数
    COUNT(*) - SUM(CASE WHEN i.indisprimary THEN 1 ELSE 0 END) AS "其它索引数", -- 其它索引数
    obj_description(c.oid, 'pg_class') AS "表备注"
FROM
    pg_index i
JOIN
    pg_class c ON i.indrelid = c.oid   -- 关联表信息
JOIN
    pg_namespace n ON c.relnamespace = n.oid -- 关联模式信息
WHERE
    n.nspname IN (
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
    AND c.relkind = 'r'                -- 只查询普通表 (r = ordinary table)
GROUP BY
    n.nspname, c.relname, c.oid
HAVING COUNT(*) - SUM(CASE WHEN i.indisprimary THEN 1 ELSE 0 END) < 3
ORDER BY
    n.nspname, c.relname