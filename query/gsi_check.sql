SELECT
    n.nspname AS "模式名",
    c.relname AS "对象名",
    c.oid AS "对象OID",
    parent.relname AS "父表名",
    c.relkind AS "对象类型代码",
    CASE c.relkind
        WHEN 'r' THEN '普通表'
        WHEN 'i' THEN '普通索引'
        WHEN 'G' THEN '全局二级索引 (GSI)'
        WHEN 'S' THEN '序列'
        WHEN 'v' THEN '视图'
        WHEN 't' THEN 'TOAST表'
        WHEN 'f' THEN '外表'
        WHEN 'm' THEN '物化视图'
        WHEN 'e' THEN 'STREAM对象'
        WHEN 'o' THEN 'CONTVIEW对象'
        WHEN 'I' THEN '分区表全局索引（旧标识）'
        ELSE '未知类型 (' || c.relkind || ')'
    END AS "对象类型描述",
    c.relpersistence AS "持久性代码",
    CASE c.relpersistence
        WHEN 'p' THEN '永久表'
        WHEN 'u' THEN '非日志表'
        WHEN 't' THEN '临时表'
        WHEN 'g' THEN '全局临时表'
        ELSE '未知持久性 (' || c.relpersistence || ')'
    END AS "持久性描述",
    c.parttype AS "分区代码",
    CASE c.parttype
        WHEN 'p' THEN '分区对象'
        WHEN 'n' THEN '非分区对象'
        ELSE '未知分区性质 (' || c.parttype || ')'
    END AS "分区描述",
    c.reltuples::bigint AS "估算行数",
    c.relpages AS "估算页数",
    c.reloptions AS "选项",
    obj_description(c.oid, 'pg_class') AS "备注"   -- 新增：对象的备注
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
LEFT JOIN pg_index i ON i.indexrelid = c.oid
LEFT JOIN pg_class parent ON parent.oid = i.indrelid
WHERE 
  n.nspname in (
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
  AND c.relkind = 'G'
ORDER BY n.nspname, c.relname;