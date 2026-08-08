-- 统一查询：对象名包含大写字母或特殊字符（可能使用了双引号）
SELECT
    schema_name AS "模式名",
    object_type AS "对象类型",
    object_name AS "对象名",
    parent_object AS "父对象",
    issue_description AS "问题描述"
FROM
(
    -- 1. 表
    SELECT
        n.nspname AS schema_name,
        'TABLE' AS object_type,
        c.relname AS object_name,
        NULL::text AS parent_object,
        '表名包含大写字母或特殊字符（可能使用双引号）' AS issue_description
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'r'
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
      AND (c.relname ~ '[A-Z]' OR c.relname ~ '[^a-zA-Z0-9_]')

    UNION ALL

    -- 2. 列
    SELECT
        n.nspname AS schema_name,
        'COLUMN' AS object_type,
        a.attname AS object_name,
        c.relname AS parent_object,
        '列名包含大写字母或特殊字符（可能使用双引号）' AS issue_description
    FROM pg_attribute a
    JOIN pg_class c ON a.attrelid = c.oid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'r'
      AND a.attnum > 0
      AND NOT a.attisdropped
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
      AND (a.attname ~ '[A-Z]' OR a.attname ~ '[^a-zA-Z0-9_]')

    UNION ALL

    -- 3. 索引
    SELECT
        n.nspname AS schema_name,
        'INDEX' AS object_type,
        i.relname AS object_name,
        c.relname AS parent_object,
        '索引名包含大写字母或特殊字符（可能使用双引号）' AS issue_description
    FROM pg_index idx
    JOIN pg_class i ON idx.indexrelid = i.oid
    JOIN pg_class c ON idx.indrelid = c.oid
    JOIN pg_namespace n ON n.oid = c.relnamespace
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
      AND (i.relname ~ '[A-Z]' OR i.relname ~ '[^a-zA-Z0-9_]')

    UNION ALL

    -- 4. 约束
    SELECT
        n.nspname AS schema_name,
        'CONSTRAINT' AS object_type,
        con.conname AS object_name,
        c.relname AS parent_object,
        '约束名包含大写字母或特殊字符（可能使用双引号）' AS issue_description
    FROM pg_constraint con
    JOIN pg_class c ON con.conrelid = c.oid
    JOIN pg_namespace n ON n.oid = c.relnamespace
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
      AND (con.conname ~ '[A-Z]' OR con.conname ~ '[^a-zA-Z0-9_]')

    UNION ALL

    -- 5. 视图
    SELECT
        n.nspname AS schema_name,
        'VIEW' AS object_type,
        c.relname AS object_name,
        NULL::text AS parent_object,
        '视图名包含大写字母或特殊字符（可能使用双引号）' AS issue_description
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'v'
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
      AND (c.relname ~ '[A-Z]' OR c.relname ~ '[^a-zA-Z0-9_]')

    UNION ALL

    -- 6. 序列
    SELECT
        n.nspname AS schema_name,
        'SEQUENCE' AS object_type,
        c.relname AS object_name,
        NULL::text AS parent_object,
        '序列名包含大写字母或特殊字符（可能使用双引号）' AS issue_description
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'S'
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
      AND (c.relname ~ '[A-Z]' OR c.relname ~ '[^a-zA-Z0-9_]')

    UNION ALL

    -- 7. 列名包含双引号字符（单独保留）
    SELECT
        n.nspname AS schema_name,
        'COLUMN' AS object_type,
        a.attname AS object_name,
        c.relname AS parent_object,
        '列名包含双引号字符' AS issue_description
    FROM pg_attribute a
    JOIN pg_class c ON a.attrelid = c.oid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'r'
      AND a.attnum > 0
      AND NOT a.attisdropped
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
      AND a.attname LIKE '%"%'   -- 单独检查双引号
) t 
WHERE object_name NOT LIKE '%delete%' 
  AND object_name NOT LIKE '%bak%' 
  AND object_name NOT LIKE '%BAK%'
ORDER BY schema_name, object_type, object_name