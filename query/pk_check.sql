WITH dist_tables AS (
    SELECT
        n.nspname AS schema_name,
        c.oid AS table_oid,
        c.relname AS table_name,
        CASE TRIM(CAST(xc.pclocatortype AS VARCHAR(10)))
            WHEN 'H' THEN 'HASH'
            WHEN 'R' THEN 'REPLICATION'
            WHEN 'N' THEN 'ROUNDROBIN'
            WHEN 'M' THEN 'MODULO'
            WHEN 'G' THEN 'GSHARDING'
            ELSE TRIM(CAST(xc.pclocatortype AS VARCHAR(10)))
        END AS distribute_type,
        xc.pcattnum AS dist_cols,                      -- 分布键列号数组
        string_agg(a.attname, ', ' ORDER BY a.attnum) AS dist_columns  -- 分布键列名
    FROM pgxc_class xc
    JOIN pg_class c ON xc.pcrelid = c.oid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    LEFT JOIN pg_attribute a
        ON a.attrelid = c.oid
        AND a.attnum = ANY(xc.pcattnum)
        AND a.attnum > 0
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
      AND xc.pclocatortype IN ('H', 'R')              -- 🔥 包含 HASH 和复制表
    GROUP BY n.nspname, c.oid, c.relname, xc.pclocatortype, xc.pcattnum
),
-- 2. 获取这些表的主键信息（列号数组 + 列名）
pk_info AS (
    SELECT
        conrelid AS table_oid,
        conkey AS pk_cols,
        string_agg(a.attname, ', ' ORDER BY a.attnum) AS pk_columns
    FROM pg_constraint con
    LEFT JOIN pg_attribute a
        ON a.attrelid = con.conrelid
        AND a.attnum = ANY(con.conkey)
        AND a.attnum > 0
    WHERE contype = 'p'
    GROUP BY conrelid, conkey
)
-- 3. 合并检查（HASH 表严格检查，复制表仅检查有无主键）
SELECT
    d.schema_name AS "模式名",
    d.table_name AS "表名",
    d.distribute_type AS "分布类型",
    COALESCE(d.dist_columns, '(无分布键)') AS "分布列",
    COALESCE(p.pk_columns, '(无主键)') AS "主键列",
    CASE
        -- 🔴 无主键：无论 HASH 还是复制表，都提示缺少主键
        WHEN p.pk_cols IS NULL THEN '❌ 缺少主键(不合规)'
        
        -- 🟢 复制表：有主键即合规（不检查分布键包含关系）
        WHEN d.distribute_type = 'REPLICATION' AND p.pk_cols IS NOT NULL THEN '✅ 复制表有主键(合规)'
        
        -- 🟢 HASH 表：主键必须包含全部分布键
        WHEN d.distribute_type = 'HASH' AND (d.dist_cols <@ p.pk_cols) THEN '✅ HASH表主键包含分布键(合规)'
        
        -- 🔴 HASH 表：有主键但未包含分布键
        WHEN d.distribute_type = 'HASH' AND NOT (d.dist_cols <@ p.pk_cols) THEN '❌ HASH表主键未包含分布键(不合规)'
        
        ELSE '⚠️ 未知状态'
    END AS "检查结果",
    obj_description(d.table_oid, 'pg_class') AS "表备注"
FROM dist_tables d
LEFT JOIN pk_info p ON d.table_oid = p.table_oid
WHERE "检查结果" LIKE '%不合规%'
ORDER BY d.schema_name, d.table_name