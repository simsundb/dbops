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
)),
index_stat AS
 (SELECT a.attname AS column_name,
         COUNT(DISTINCT ri.relname) AS index_count
    FROM pg_index i
    JOIN pg_class c       ON c.oid = i.indrelid
    JOIN pg_class ri      ON ri.oid = i.indexrelid
    JOIN pg_namespace n   ON n.oid = c.relnamespace
    JOIN pg_attribute a   ON a.attrelid = c.oid 
                          AND a.attnum = ANY(i.indkey)
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
    'yth_kf_glcx_h')
     AND a.attnum > 0
     AND NOT a.attisdropped
   GROUP BY a.attname
)
SELECT ci.column_name                                          AS "字段名称",
       CASE
         WHEN COUNT(DISTINCT ci.base_type) > 1 THEN
          '类型不一致'
         WHEN COUNT(DISTINCT ci.full_type) > 1 THEN
          '长度/精度不一致'
         ELSE
          '一致'
       END                                                      AS "差异类型",
       COUNT(DISTINCT ci.base_type)                             AS "基础类型数",
       string_agg(DISTINCT ci.base_type, ',')                   AS "基础类型列表",
       COUNT(DISTINCT ci.full_type)                             AS "完整类型数",
       string_agg(DISTINCT ci.full_type, ',')                   AS "完整类型列表",
       COALESCE(MAX(idx.index_count), 0)                        AS "索引出现次数",
       CASE
         WHEN MAX(idx.index_count) IS NOT NULL AND MAX(idx.index_count) > 0 THEN
          '是'
         ELSE
          '否'
       END                                                      AS "是否为索引列",
       LEFT(string_agg(ci.table_name || '(' || ci.full_type || ')',
                 ';' ORDER BY ci.table_name), 32767)            AS "表定义详情(截断)"
  FROM col_info ci
  LEFT JOIN index_stat idx ON idx.column_name = ci.column_name
 GROUP BY ci.column_name
HAVING COUNT(DISTINCT ci.base_type) > 1 OR COUNT(DISTINCT ci.full_type) > 1
 ORDER BY MAX(idx.index_count) DESC,
          CASE
            WHEN COUNT(DISTINCT ci.base_type) > 1 THEN
             1
            ELSE
             2
          END,
          ci.column_name;