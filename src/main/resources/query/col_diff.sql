WITH col_info AS
 (SELECT n.nspname schema_name,
         c.relname table_name,
         a.attname column_name,
         t.typname base_type,
         format_type(a.atttypid, a.atttypmod) full_type
    FROM pg_attribute a
    JOIN pg_class c
      ON c.oid = a.attrelid
    JOIN pg_namespace n
      ON n.oid = c.relnamespace
    JOIN pg_type t
      ON t.oid = a.atttypid
   WHERE a.attnum > 0
     AND NOT a.attisdropped
     AND c.relkind IN ('r', 'p')
     AND n.nspname  IN (
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
))
SELECT column_name                                          AS "字段名称",
       CASE
         WHEN COUNT(DISTINCT base_type) > 1 THEN
          '类型不一致'
         WHEN COUNT(DISTINCT full_type) > 1 THEN
          '长度/精度不一致'
         ELSE
          '一致'
       END                                                   AS "差异类型",
       COUNT(DISTINCT base_type)                              AS "基础类型数",
       string_agg(DISTINCT base_type, ',')                    AS "基础类型列表",
       COUNT(DISTINCT full_type)                              AS "完整类型数",
       string_agg(DISTINCT full_type, ',')                    AS "完整类型列表",
       LEFT(string_agg(table_name || '(' || full_type || ')',
                 ';' ORDER BY table_name), 32767)            AS "表定义详情(截断)"
  FROM col_info
 GROUP BY column_name
HAVING COUNT (DISTINCT base_type) > 1 OR COUNT (DISTINCT full_type) > 1
 ORDER BY CASE
            WHEN COUNT(DISTINCT base_type) > 1 THEN
             1
            ELSE
             2
          END,
          column_name;