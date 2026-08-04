
WITH index_base AS (
    SELECT
        lower(i.index_name) AS index_name,
        lower(i.table_owner) AS table_owner,
        lower(i.table_name) AS table_name,
        lower(i.owner) AS index_owner,
        CASE
            WHEN lower(i.index_name) LIKE 'idx\_%' ESCAPE '\' THEN 'idx'
            WHEN lower(i.index_name) LIKE 'pk\_%' ESCAPE '\' THEN 'pk'
            WHEN lower(i.index_name) LIKE 'uidx\_%' ESCAPE '\' THEN 'uidx'
            ELSE '未知前缀'
        END AS prefix_type,
        CASE
            WHEN lower(i.index_name) LIKE 'idx\_%' ESCAPE '\' THEN SUBSTRING(lower(i.index_name) FROM 5)
            WHEN lower(i.index_name) LIKE 'pk\_%' ESCAPE '\' THEN SUBSTRING(lower(i.index_name) FROM 4)
            WHEN lower(i.index_name) LIKE 'uidx\_%' ESCAPE '\' THEN SUBSTRING(lower(i.index_name) FROM 6)
            ELSE lower(i.index_name)
        END AS suffix
    FROM db_indexes i
    WHERE i.owner IN (
        'yth_kf_jfzw_h', 'yth_kf_schjy_h', 'yth_kf_goud_h',
        'yth_kf_share_h', 'yth_kf_yk_h', 'yth_kf_kefu_h',
        'yth_kf_yj_h', 'yth_kf_jl_h', 'yth_kf_dlgyxq_h',
        'yth_kf_xs_h', 'yth_kf_jc_h', 'yth_kf_xtjc_h',
        'yth_kf_xtzc_h', 'yth_kf_glback_h', 'yth_kf_glbb_h',
        'yth_kf_glcx_h'
    )
)
SELECT
    ib.index_owner AS "索引所属用户",
    ib.index_name AS "索引名称",
    ib.prefix_type AS "前缀类型",
    ib.suffix AS "去掉前缀后的内容",
    ib.table_name AS "实际表名",
    CASE
        WHEN ib.prefix_type = '未知前缀' THEN '前缀不符合规范'
        ELSE '索引中表名不匹配'
    END AS "错误原因"
FROM index_base ib
WHERE NOT (
    ib.prefix_type != '未知前缀'
    AND (INSTR(ib.suffix, ib.table_name) > 0 OR INSTR(ib.table_name, ib.suffix) > 0)
)
ORDER BY ib.index_owner, ib.index_name