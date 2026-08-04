SELECT
    n.nspname AS "模式名",
    c.relname AS "表名",
    CASE TRIM(CAST(xc.pclocatortype AS VARCHAR(10)))
        WHEN 'H' THEN 'HASH'
        WHEN 'R' THEN 'REPLICATION'
        WHEN 'N' THEN 'ROUNDROBIN'
        WHEN 'M' THEN 'MODULO'
        WHEN 'G' THEN 'GSHARDING'
        ELSE TRIM(CAST(xc.pclocatortype AS VARCHAR(10)))
    END AS "分布类型",
    string_agg(a.attname, ',' ORDER BY a.attnum) AS "分布列",
    string_agg(
        CASE WHEN a.attnotnull THEN 'NOT NULL' ELSE 'NULL' END, 
        ',' ORDER BY a.attnum
    ) AS "分布列是否可空",
    obj_description(c.oid, 'pg_class') AS "表备注"
FROM pgxc_class xc
JOIN pg_class c
  ON xc.pcrelid = c.oid
JOIN pg_namespace n
  ON n.oid = c.relnamespace
LEFT JOIN pg_attribute a
  ON a.attrelid = c.oid
 AND a.attnum = ANY(xc.pcattnum)
WHERE n.nspname IN (
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
  AND xc.pclocatortype != 'R'                     -- 排除复制表
  AND xc.pcattnum IS NOT NULL
  AND array_length(xc.pcattnum, 1) > 0           -- 只保留有分布键的表
GROUP BY
    n.nspname,
    c.oid,
    c.relname,
    xc.pclocatortype
HAVING
    COUNT(CASE WHEN NOT a.attnotnull THEN 1 END) > 0   -- 存在至少一个可空分布键列
ORDER BY
    n.nspname, c.relname