SELECT
    sequence_owner AS "模式名",
    sequence_name AS "序列名",
    TRIM(BOTH '; ' FROM
        CASE WHEN cache_size < 1000 THEN '⚠️ 缓存值过小;' ELSE '' END ||
        CASE WHEN cycle_flag = 'y' THEN '⚠️ 循环序列; ' ELSE '' END ||
        CASE WHEN increment_by <> 1 THEN '⚠️ 步长异常（建议1）; ' ELSE '' END ||
        CASE WHEN order_flag IS NOT NULL THEN '⚠️ 有序序列（异常）; ' ELSE '' END
    ) AS "检查状态",
    cache_size AS "缓存大小",
    cycle_flag AS "循环标志",
    max_value AS "最大值",
    last_number AS "当前值",
    order_flag AS "有序标志",
    increment_by AS "步长",
    round(100.0 * last_number / nullif(max_value, 0), 2) AS "使用百分比"
FROM db_sequences
WHERE
    (cache_size < 1000
    OR cycle_flag = 'y'
    OR order_flag IS NOT NULL
    OR increment_by <> 1)
    AND sequence_owner IN (
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
ORDER BY sequence_owner, sequence_name