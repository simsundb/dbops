WITH dist_info AS
(
    SELECT
        n.nspname AS schema_name,
        c.oid     AS table_oid,
        c.relname AS table_name,
        CASE TRIM(CAST(xc.pclocatortype AS VARCHAR(10)))
            WHEN 'H' THEN 'HASH'
            WHEN 'R' THEN 'REPLICATION'
            WHEN 'N' THEN 'ROUNDROBIN'
            WHEN 'M' THEN 'MODULO'
            WHEN 'G' THEN 'GSHARDING'
            ELSE TRIM(CAST(xc.pclocatortype AS VARCHAR(10)))
        END AS distribute_type,
        string_agg(a.attname, ',' ORDER BY a.attnum) AS distribute_columns
    FROM pgxc_class xc
    JOIN pg_class c
      ON xc.pcrelid = c.oid
    JOIN pg_namespace n
      ON n.oid = c.relnamespace
    LEFT JOIN pg_attribute a
      ON a.attrelid = c.oid
     AND a.attnum = ANY(xc.pcattnum)
    WHERE n.nspname in (
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
    GROUP BY
        n.nspname,
        c.oid,
        c.relname,
        xc.pclocatortype
),
part_info AS
(
    SELECT
        c.oid AS table_oid,
        CASE p.partstrategy
            WHEN 'r' THEN 'RANGE'
            WHEN 'l' THEN 'LIST'
            WHEN 'h' THEN 'HASH'
            ELSE 'UNKNOWN'
        END AS partition_type,
        string_agg(a.attname, ',' ORDER BY a.attnum) AS partition_columns
    FROM pg_partition p
    JOIN pg_class c
      ON p.parentid = c.oid
    LEFT JOIN pg_attribute a
      ON a.attrelid = c.oid
     AND a.attnum = ANY(p.partkey)
    WHERE p.parttype = 'r'
    GROUP BY
        c.oid,
        p.partstrategy
)
SELECT
    d.schema_name AS "模式名",
    d.table_name AS "表名",
    d.distribute_type AS "分布方式",
    CASE d.distribute_type
        WHEN 'HASH' THEN '哈希分布'
        WHEN 'REPLICATION' THEN '复制分布'
        WHEN 'ROUNDROBIN' THEN '轮询分布'
        WHEN 'MODULO' THEN '取模分布'
        WHEN 'GSHARDING' THEN 'G级分片'
        ELSE d.distribute_type
    END AS "分布方式(中文)",
    d.distribute_columns AS "分布键",
    NVL(p.partition_type,'NONPARTITION') AS "分区方式",
    CASE NVL(p.partition_type,'NONPARTITION')
        WHEN 'RANGE' THEN '范围分区'
        WHEN 'LIST' THEN '列表分区'
        WHEN 'HASH' THEN '哈希分区'
        WHEN 'NONPARTITION' THEN '非分区表'
        ELSE NVL(p.partition_type,'NONPARTITION')
    END AS "分区方式(中文)",
    p.partition_columns AS "分区列",
    obj_description(d.table_oid, 'pg_class') AS "表备注"
FROM dist_info d
LEFT JOIN part_info p
       ON d.table_oid = p.table_oid
/*where strpos(d.distribute_columns,',')>0 
    or strpos(p.partition_columns,',')>0
    or d.distribute_type not in ('REPLICATION','HASH')*/ --解封就是多列的检查了
    where d.distribute_type='REPLICATION' and NVL(p.partition_type,'NONPARTITION')<>'NONPARTITION'
ORDER BY  d.schema_name, d.table_name, d.distribute_type, d.distribute_columns, NVL(p.partition_type,'NONPARTITION'), p.partition_columns