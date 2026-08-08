SELECT * FROM (
    -- 1. 表 (TABLE)
    SELECT 
        'TABLE' AS object_type,
        n.nspname AS schema_name,
        c.relname AS object_name,
        '用户表' AS description
    FROM pg_class c
    JOIN pg_namespace n ON c.relnamespace = n.oid
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
      AND c.relname ~ '[0-9]'   -- 名称包含数字
    UNION ALL

    -- 2. 索引 (INDEX)
    SELECT 
        'INDEX' AS object_type,
        n.nspname AS schema_name,
        c.relname AS object_name,
        '索引（含唯一索引）' AS description
    FROM pg_class c
    JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE c.relkind = 'i' 
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
      AND c.relname ~ '[0-9]'
    UNION ALL

    -- 3. 视图 (VIEW)
    SELECT 
        'VIEW' AS object_type,
        n.nspname AS schema_name,
        c.relname AS object_name,
        '视图' AS description
    FROM pg_class c
    JOIN pg_namespace n ON c.relnamespace = n.oid
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
      AND c.relname ~ '[0-9]'
    UNION ALL

    -- 4. 序列 (SEQUENCE)
    SELECT 
        'SEQUENCE' AS object_type,
        n.nspname AS schema_name,
        c.relname AS object_name,
        '序列' AS description
    FROM pg_class c
    JOIN pg_namespace n ON c.relnamespace = n.oid
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
      AND c.relname ~ '[0-9]'
    UNION ALL

    -- 5. 约束 (CONSTRAINT)
    SELECT 
        'CONSTRAINT' AS object_type,
        n.nspname AS schema_name,
        con.conname AS object_name,
        CASE con.contype 
            WHEN 'p' THEN '主键约束'
            WHEN 'u' THEN '唯一约束'
            WHEN 'f' THEN '外键约束'
            WHEN 'c' THEN '检查约束'
            ELSE '其他约束'
        END AS description
    FROM pg_constraint con
    JOIN pg_class c ON con.conrelid = c.oid
    JOIN pg_namespace n ON c.relnamespace = n.oid
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
      AND con.conname ~ '[0-9]'
    UNION ALL

    -- 6. 函数/存储过程 (FUNCTION/PROCEDURE)
    SELECT 
        'FUNCTION' AS object_type,
        n.nspname AS schema_name,
        p.proname AS object_name,
        '函数或存储过程' AS description
    FROM pg_proc p
    JOIN pg_namespace n ON p.pronamespace = n.oid
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
      AND p.proname ~ '[0-9]'
    UNION ALL

    -- 7. 模式 (SCHEMA) 自身
    SELECT 
        'SCHEMA' AS object_type,
        n.nspname AS schema_name,
        n.nspname AS object_name,
        '模式对象' AS description
    FROM pg_namespace n
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
      AND n.nspname ~ '[0-9]'
)
WHERE object_name NOT LIKE '%delete%' 
   OR object_name NOT LIKE '%bak%' 
   OR object_name NOT LIKE '%BAK%'
ORDER BY schema_name, object_type, object_name;