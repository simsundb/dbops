SELECT d."owner",
       d.table_name,
       d.data_type,
       d.column_name,
       d."comments",
       d."nullable",
       d.data_default
  FROM db_Tab_columns d
 WHERE d.data_type IN ('datea',
                       'float8',
                       'bpchar',
                       'time',
                       'jsonb',
                       'blob',
                       'bool',
                       'bytea')
   AND "owner" IN ('yth_kf_jfzw_h',
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
                   'yth_kf_glcx_h')
   AND d.table_name NOT LIKE 'pg_toast_%'
 ORDER BY d."owner", d.table_name, d.data_type
