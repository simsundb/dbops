SELECT * FROM (
--查看管控表的主键是不是只有ID,而没有唯一索引
-- 合并查询：主键 + 唯一约束 + 唯一索引（去重）
WITH dist_cols AS (
    SELECT
        xc.pcrelid AS relid,
        string_agg(a.attname, ', ' ORDER BY a.attnum) AS dist_columns
    FROM pgxc_class xc
    JOIN pg_attribute a ON a.attrelid = xc.pcrelid
                       AND a.attnum = ANY(xc.pcattnum)
    WHERE a.attnum > 0
      AND NOT a.attisdropped
    GROUP BY xc.pcrelid
),
pk AS (
    SELECT
        n.nspname AS schema_name,
        c.relname AS table_name,
        con.conname AS name,
        'PRIMARY KEY' AS type,
        (
            SELECT string_agg(a.attname, ', ' ORDER BY a.attnum)
            FROM pg_attribute a
            WHERE a.attrelid = c.oid
              AND a.attnum = ANY(con.conkey)
        ) AS columns,
        (
            SELECT COUNT(*)
            FROM pg_attribute a
            WHERE a.attrelid = c.oid
              AND a.attnum = ANY(con.conkey)
        ) AS column_count,
        dc.dist_columns AS dist_columns,
        CASE
            WHEN dc.dist_columns IS NULL OR dc.dist_columns = ''
            THEN '否'
            ELSE
                CASE
                    WHEN (
                        SELECT bool_and(
                            INSTR(
                                ' | ' || REPLACE(
                                    (SELECT string_agg(a2.attname, ', ' ORDER BY a2.attnum)
                                     FROM pg_attribute a2
                                     WHERE a2.attrelid = c.oid
                                       AND a2.attnum = ANY(con.conkey)),
                                ', ', ' | ') || ' | ',
                                ' | ' || trim(dc_col.attname) || ' | '
                            ) > 0
                        )
                        FROM (
                            SELECT unnest(string_to_array(dc.dist_columns, ',')) AS attname
                        ) dc_col
                    ) THEN '是'
                    ELSE '否'
                END
        END AS dist_included,
        CASE
            WHEN EXISTS (
                SELECT 1 FROM pgxc_class xc
                WHERE xc.pcrelid = c.oid
                  AND xc.pclocatortype = 'R'
            ) THEN 'REPLICATION'
            WHEN EXISTS (
                SELECT 1 FROM pgxc_class xc
                WHERE xc.pcrelid = c.oid
                  AND xc.pclocatortype = 'H'
            ) THEN 'HASH'
            WHEN EXISTS (
                SELECT 1 FROM pgxc_class xc
                WHERE xc.pcrelid = c.oid
                  AND xc.pclocatortype = 'N'
            ) THEN 'ROUNDROBIN'
            ELSE 'OTHER'
        END AS dist_type
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_constraint con ON con.conrelid = c.oid AND con.contype = 'p'
    LEFT JOIN dist_cols dc ON dc.relid = c.oid
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
      AND c.relname !~ '[0-9]$'
),
uq AS (
    SELECT
        n.nspname AS schema_name,
        c.relname AS table_name,
        con.conname AS name,
        'UNIQUE CONSTRAINT' AS type,
        (
            SELECT string_agg(a.attname, ', ' ORDER BY a.attnum)
            FROM pg_attribute a
            WHERE a.attrelid = c.oid
              AND a.attnum = ANY(con.conkey)
        ) AS columns,
        (
            SELECT COUNT(*)
            FROM pg_attribute a
            WHERE a.attrelid = c.oid
              AND a.attnum = ANY(con.conkey)
        ) AS column_count,
        dc.dist_columns AS dist_columns,
        CASE
            WHEN dc.dist_columns IS NULL OR dc.dist_columns = ''
            THEN '否'
            ELSE
                CASE
                    WHEN (
                        SELECT bool_and(
                            INSTR(
                                ' | ' || REPLACE(
                                    (SELECT string_agg(a2.attname, ', ' ORDER BY a2.attnum)
                                     FROM pg_attribute a2
                                     WHERE a2.attrelid = c.oid
                                       AND a2.attnum = ANY(con.conkey)),
                                ', ', ' | ') || ' | ',
                                ' | ' || trim(dc_col.attname) || ' | '
                            ) > 0
                        )
                        FROM (
                            SELECT unnest(string_to_array(dc.dist_columns, ',')) AS attname
                        ) dc_col
                    ) THEN '是'
                    ELSE '否'
                END
        END AS dist_included,
        CASE
            WHEN EXISTS (
                SELECT 1 FROM pgxc_class xc
                WHERE xc.pcrelid = c.oid
                  AND xc.pclocatortype = 'R'
            ) THEN 'REPLICATION'
            WHEN EXISTS (
                SELECT 1 FROM pgxc_class xc
                WHERE xc.pcrelid = c.oid
                  AND xc.pclocatortype = 'H'
            ) THEN 'HASH'
            WHEN EXISTS (
                SELECT 1 FROM pgxc_class xc
                WHERE xc.pcrelid = c.oid
                  AND xc.pclocatortype = 'N'
            ) THEN 'ROUNDROBIN'
            ELSE 'OTHER'
        END AS dist_type
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_constraint con ON con.conrelid = c.oid AND con.contype = 'u'
    LEFT JOIN dist_cols dc ON dc.relid = c.oid
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
      AND c.relname !~ '[0-9]$'
),
ui AS (
    SELECT
        n.nspname AS schema_name,
        c.relname AS table_name,
        i.relname AS name,
        'UNIQUE INDEX' AS type,
        (
            SELECT string_agg(a.attname, ', ' ORDER BY a.attnum)
            FROM pg_attribute a
            WHERE a.attrelid = c.oid
              AND a.attnum = ANY(idx.indkey)
        ) AS columns,
        (
            SELECT COUNT(*)
            FROM pg_attribute a
            WHERE a.attrelid = c.oid
              AND a.attnum = ANY(idx.indkey)
        ) AS column_count,
        dc.dist_columns AS dist_columns,
        CASE
            WHEN dc.dist_columns IS NULL OR dc.dist_columns = ''
            THEN '否'
            ELSE
                CASE
                    WHEN (
                        SELECT bool_and(
                            INSTR(
                                ' | ' || REPLACE(
                                    (SELECT string_agg(a2.attname, ', ' ORDER BY a2.attnum)
                                     FROM pg_attribute a2
                                     WHERE a2.attrelid = c.oid
                                       AND a2.attnum = ANY(idx.indkey)),
                                ', ', ' | ') || ' | ',
                                ' | ' || trim(dc_col.attname) || ' | '
                            ) > 0
                        )
                        FROM (
                            SELECT unnest(string_to_array(dc.dist_columns, ',')) AS attname
                        ) dc_col
                    ) THEN '是'
                    ELSE '否'
                END
        END AS dist_included,
        CASE
            WHEN EXISTS (
                SELECT 1 FROM pgxc_class xc
                WHERE xc.pcrelid = c.oid
                  AND xc.pclocatortype = 'R'
            ) THEN 'REPLICATION'
            WHEN EXISTS (
                SELECT 1 FROM pgxc_class xc
                WHERE xc.pcrelid = c.oid
                  AND xc.pclocatortype = 'H'
            ) THEN 'HASH'
            WHEN EXISTS (
                SELECT 1 FROM pgxc_class xc
                WHERE xc.pcrelid = c.oid
                  AND xc.pclocatortype = 'N'
            ) THEN 'ROUNDROBIN'
            ELSE 'OTHER'
        END AS dist_type
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_index idx ON idx.indrelid = c.oid
    JOIN pg_class i ON i.oid = idx.indexrelid
    LEFT JOIN dist_cols dc ON dc.relid = c.oid
    WHERE c.relkind = 'r'
      AND idx.indisunique = true
      AND idx.indisprimary = false
      AND NOT EXISTS (SELECT 1 FROM pg_constraint con WHERE con.conindid = i.oid)
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
      AND c.relname !~ '[0-9]$'
)
SELECT
    schema_name,
    table_name,
    name,
    type,
    columns,
    column_count,
    dist_type,
    dist_columns,
    dist_included
FROM pk
UNION ALL
SELECT
    schema_name,
    table_name,
    name,
    type,
    columns,
    column_count,
    dist_type,
    dist_columns,
    dist_included
FROM uq
UNION ALL
SELECT
    schema_name,
    table_name,
    name,
    type,
    columns,
    column_count,
    dist_type,
    dist_columns,
    dist_included
FROM ui
ORDER BY schema_name, table_name, type
) z_q WHERE (dist_type = 'HASH') AND (dist_included = '否') ORDER BY z_q.dist_included DESC