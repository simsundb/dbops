WITH all_objects AS (
    -- 从 pg_class 获取常规对象
    SELECT
        n.nspname AS schema_name,
        CASE c.relkind
            WHEN 'r' THEN '普通表'
            WHEN 'i' THEN '普通索引'
            WHEN 'G' THEN '全局二级索引GSI'
            WHEN 'S' THEN '序列'
            WHEN 'v' THEN '视图'
            WHEN 't' THEN 'TOAST表'
            WHEN 'f' THEN '外表'
            WHEN 'm' THEN '物化视图'
            WHEN 'e' THEN 'STREAM对象'
            WHEN 'o' THEN 'CONTVIEW对象'
            WHEN 'I' THEN '分区表全局索引'
            WHEN 'c' THEN '复合类型'        -- ⬅️ 新增
            ELSE '其他对象'
        END AS object_type
    FROM pg_class c
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
    AND n.nspname NOT LIKE 'pg\_%' ESCAPE '\'
    AND n.nspname NOT LIKE 'dbe\_%' ESCAPE '\'
    AND n.nspname NOT LIKE 'pkg\_%' ESCAPE '\'

    UNION ALL

    -- 从 pg_synonym 获取同义词
    SELECT
        n.nspname AS schema_name,
        '同义词' AS object_type
    FROM pg_synonym s
    JOIN pg_namespace n ON n.oid = s.synnamespace
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
)
SELECT
    schema_name AS "模式名",
    SUM(CASE WHEN object_type = '普通表' THEN 1 ELSE 0 END) AS "普通表",
    SUM(CASE WHEN object_type = '普通索引' THEN 1 ELSE 0 END) AS "普通索引",
    SUM(CASE WHEN object_type = '全局二级索引GSI' THEN 1 ELSE 0 END) AS "全局二级索引GSI",
    SUM(CASE WHEN object_type = '序列' THEN 1 ELSE 0 END) AS "序列",
    SUM(CASE WHEN object_type = '视图' THEN 1 ELSE 0 END) AS "视图",
    SUM(CASE WHEN object_type = 'TOAST表' THEN 1 ELSE 0 END) AS "TOAST表",
    SUM(CASE WHEN object_type = '外表' THEN 1 ELSE 0 END) AS "外表",
    SUM(CASE WHEN object_type = '物化视图' THEN 1 ELSE 0 END) AS "物化视图",
    SUM(CASE WHEN object_type = 'STREAM对象' THEN 1 ELSE 0 END) AS "STREAM对象",
    SUM(CASE WHEN object_type = 'CONTVIEW对象' THEN 1 ELSE 0 END) AS "CONTVIEW对象",
    SUM(CASE WHEN object_type = '分区表全局索引' THEN 1 ELSE 0 END) AS "分区表全局索引",
    SUM(CASE WHEN object_type = '复合类型' THEN 1 ELSE 0 END) AS "复合类型",   -- ⬅️ 新增
    SUM(CASE WHEN object_type = '同义词' THEN 1 ELSE 0 END) AS "同义词",
    SUM(CASE WHEN object_type = '其他对象' THEN 1 ELSE 0 END) AS "其他对象",
    COUNT(*) AS "合计"
FROM all_objects
GROUP BY schema_name
ORDER BY schema_name;