SELECT 
    n.nspname AS schema_name,
    c.relname AS view_name,
    pg_get_viewdef(c.oid) AS view_definition
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE c.relkind = 'v'          -- v = view
  AND n.nspname  IN (
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
ORDER BY c.relname;