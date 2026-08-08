WITH idx_cols AS (
    SELECT
        i.table_owner,
        i.table_name,
        i.index_name,
        i.uniqueness,
        LISTAGG(c.column_name, ',') WITHIN GROUP (ORDER BY c.column_position) AS idx_columns,
        COUNT(*) AS col_cnt
    FROM DB_INDEXES i           -- 当前用户可见索引（DBA用 ADM_INDEXES）
    JOIN DB_IND_COLUMNS c       -- 索引列明细（DBA用 ADM_IND_COLUMNS）
        ON  i.owner      = c.index_owner
        AND i.index_name = c.index_name
        AND i.table_name = c.table_name
    WHERE  i.index_type  = 'NORMAL'   -- 普通B-tree，排除函数/位图
      AND i.table_owner IN (
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
    GROUP BY i.table_owner, i.table_name, i.index_name, i.uniqueness
)
SELECT
    a.table_owner,
    a.table_name        AS 表名,
    a.index_name        AS 子集索引,
    a.idx_columns       AS 子集列,
    a.col_cnt           AS 子集列数,
    b.index_name        AS 超集索引,
    b.idx_columns       AS 超集列,
    b.col_cnt           AS 超集列数,
    CASE
        WHEN a.uniqueness='UNIQUE' AND b.uniqueness<>'UNIQUE'
            THEN '⚠ 子集是唯一索引，谨慎删除'
        WHEN a.col_cnt = b.col_cnt
            THEN '⚠ 完全相同的索引（重复）'
        ELSE '✅ 可评估删除子集索引'
    END                  AS 建议,
    obj_description(
        (SELECT c.oid FROM pg_class c 
         JOIN pg_namespace n ON n.oid = c.relnamespace 
         WHERE n.nspname = a.table_owner AND c.relname = a.table_name), 
        'pg_class'
    ) AS 表备注
FROM idx_cols a
JOIN idx_cols b
    ON  a.table_owner = b.table_owner
    AND a.table_name  = b.table_name
    AND a.index_name  <> b.index_name       -- 避免自连接和重复配对
    AND a.col_cnt     <= b.col_cnt           -- 子集列数 ≤ 超集列数
    AND INSTR(b.idx_columns || ',', a.idx_columns || ',') = 1   -- 前缀真包含(X vs X,Y)
WHERE a.col_cnt < b.col_cnt                 -- 排除完全相同（可选去掉保留看重复）
ORDER BY a.table_owner, a.table_name, a.index_name