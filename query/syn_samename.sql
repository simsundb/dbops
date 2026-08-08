WITH target_schemas AS (
    SELECT unnest(ARRAY[
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
    ]) AS schema_name
),
current_synonyms AS (
    SELECT
        s.synname,
        s.synnamespace,
        ns.nspname AS schema_name,
        'SYNONYM' AS synonym_type
    FROM
        pg_synonym s
    JOIN
        pg_namespace ns ON s.synnamespace = ns.oid
    WHERE
        ns.nspname IN (SELECT schema_name FROM target_schemas)
),
other_objects AS (
    -- 表、视图、物化视图、序列 (来自 pg_class)
    SELECT
        c.relname AS object_name,
        ns.nspname AS schema_name,
        CASE c.relkind
            WHEN 'r' THEN 'TABLE'
            WHEN 'v' THEN 'VIEW'
            WHEN 'm' THEN 'MATERIALIZED VIEW'
            WHEN 'S' THEN 'SEQUENCE'
            ELSE 'OTHER'
        END AS object_type
    FROM
        pg_class c
    JOIN
        pg_namespace ns ON c.relnamespace = ns.oid
    WHERE
        ns.nspname IN (SELECT schema_name FROM target_schemas)
        AND c.relkind IN ('r', 'v', 'm', 'S')
    
    UNION ALL
    
    -- 函数、存储过程 (来自 pg_proc)
    SELECT
        p.proname AS object_name,
        ns.nspname AS schema_name,
        CASE
            WHEN p.prokind = 'f' THEN 'FUNCTION'
            WHEN p.prokind = 'p' THEN 'PROCEDURE'
            ELSE 'OTHER'
        END AS object_type
    FROM
        pg_proc p
    JOIN
        pg_namespace ns ON p.pronamespace = ns.oid
    WHERE
        ns.nspname IN (SELECT schema_name FROM target_schemas)
)
SELECT
    s.schema_name,                      -- 模式名
    s.synname AS synonym_name,          -- 同义词名称
    s.synonym_type,                     -- 对象类型（固定为'SYNONYM'）
    o.object_name AS conflicting_object_name,     -- 冲突对象名称
    o.object_type AS conflicting_object_type,     -- 冲突对象类型
    '同义词与 ' || o.object_type || ' 重名' AS conflict_description  -- 冲突描述
FROM
    current_synonyms s
JOIN
    other_objects o 
    ON s.schema_name = o.schema_name    -- 确保在同一个模式下比较
    AND s.synname = o.object_name       -- 名称相同
ORDER BY
    s.schema_name,                      -- 按模式名排序
    s.synname           								-- 再按同义词名排序