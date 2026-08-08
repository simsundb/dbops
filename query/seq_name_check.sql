WITH seq_parse AS (
    SELECT
        sequence_owner,
        sequence_name,
        CASE
            WHEN sequence_name NOT LIKE 'seq\_%' ESCAPE '\' THEN '不以seq_开头'
            WHEN LENGTH(SUBSTRING(sequence_name FROM 5)) = 0 THEN 'seq_后无内容'
            ELSE '可解析'
        END AS parse_status,
        CASE
            WHEN sequence_name LIKE 'seq\_%' ESCAPE '\' 
            THEN SUBSTRING(sequence_name FROM 5)
            ELSE NULL
        END AS seq_body
    FROM db_sequences
    WHERE sequence_owner IN (
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
    sp.sequence_owner,
    sp.sequence_name,
    sp.parse_status,
    sp.seq_body,
    CASE
        WHEN sp.parse_status != '可解析' THEN sp.parse_status
        ELSE '表名不存在于db_tables'
    END AS error_reason
FROM seq_parse sp
WHERE NOT EXISTS (
    SELECT 1 
    FROM db_tables t 
    WHERE t.owner = sp.sequence_owner
      AND INSTR(sp.seq_body, t.table_name) > 0
)
ORDER BY sp.sequence_owner, sp.sequence_name;