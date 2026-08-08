SELECT n.nspname AS "模式名",
       c.relname AS "表名",
       pg_size_pretty(pg_total_relation_size(c.oid)) AS "总大小",
       c.reltuples::bigint AS "行数(估算)",   -- 表记录数（估算值，来自ANALYZE）
       obj_description(c.oid, 'pg_class') AS "表备注"
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
JOIN pgxc_class x ON x.pcrelid = c.oid
WHERE x.pclocatortype = 'R'  -- R表示复制表
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
  AND (pg_total_relation_size(c.oid) > 1024 * 1024 * 10 
  OR c.reltuples::bigint > 10000)  -- 大于10MB或者大于1万行:标准可修改
ORDER BY n.nspname, pg_total_relation_size(c.oid) DESC