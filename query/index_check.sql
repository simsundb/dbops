WITH idx_cols AS (
    SELECT
        n.nspname AS schema_name,
        t.relname AS table_name,
        i.relname AS index_name,
        string_agg(a.attname, ',' ORDER BY a.attnum) AS col_list
    FROM pg_index idx
    JOIN pg_class i ON i.oid = idx.indexrelid
    JOIN pg_class t ON t.oid = idx.indrelid
    JOIN pg_namespace n ON n.oid = t.relnamespace
    JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(idx.indkey)
    WHERE t.relkind = 'r'               -- 普通表
      AND i.relkind = 'i'               -- 普通索引
      AND n.nspname NOT IN (
        'pg_toast', 'pg_catalog', 'public', 'information_schema',
        'dbe_perf', 'snapshot', 'sys', 'pkg_service', 'pkg_util',
        'dbe_raw', 'dbe_session', 'dbe_lob', 'dbe_match', 'dbe_task',
        'dbe_sql', 'dbe_file', 'dbe_output', 'dbe_random',
        'dbe_application_info', 'dbe_utility', 'dbe_scheduler',
        'dbe_sql_util', 'dbe_pldebugger', 'dbe_pldeveloper',
        'blockchain', 'sqladvisor', 'db4ai'
      )
      AND a.attnum > 0                  -- 排除系统列
      AND NOT a.attisdropped
    GROUP BY n.nspname, t.relname, i.relname
)
SELECT
    a.schema_name AS "模式名",
    a.table_name AS "表名",
    a.index_name AS "索引1",
    b.index_name AS "索引2",
    a.col_list AS "索引列列表"
FROM idx_cols a
JOIN idx_cols b
  ON a.schema_name = b.schema_name
 AND a.table_name = b.table_name
 AND a.col_list = b.col_list
 AND a.index_name < b.index_name   -- 避免重复配对
ORDER BY a.schema_name, a.table_name