select
    owner AS "模式名",
    table_name AS "表名",
    tablespace_name AS "表空间名",
    temporary AS "临时标志",
    status AS "状态",
    num_rows AS "行数",
    sample_size AS "采样大小",
    avg_row_len AS "平均行长度",
    last_analyzed AS "最后分析时间",
    partitioned AS "分区标志",
    compression AS "压缩标志"
FROM DB_TABLES 
WHERE temporary = 'y'
  AND owner NOT IN (
    'pg_toast', 'pg_catalog', 'public', 'information_schema',
    'dbe_perf', 'snapshot', 'sys', 'pkg_service', 'pkg_util',
    'dbe_raw', 'dbe_session', 'dbe_lob', 'dbe_match', 'dbe_task',
    'dbe_sql', 'dbe_file', 'dbe_output', 'dbe_random',
    'dbe_application_info', 'dbe_utility', 'dbe_scheduler',
    'dbe_sql_util', 'dbe_pldebugger', 'dbe_pldeveloper',
    'blockchain', 'sqladvisor', 'db4ai'
  );