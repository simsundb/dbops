-- =============================================================
-- 检查指定多个模式（可指定单表）下所有列名与关键字冲突
-- 设置参数（替换为你需要的值）：
--   target_schemas : 要检查的模式列表，用逗号分隔（必填，如 'public, sales'）
--   target_table   : 要检查的表名（选填，若不填或设为 NULL 则检查所有表）
-- =============================================================
--
-- 关键字类型说明（catcode）：
--   R（Reserved）：完全保留关键字。如 SELECT、INSERT 等，在任何上下文里都不能直接用作对象名，
--                  除非用双引号包裹。共 97 个。
--   T（Type / Function name）：类型或函数名关键字。属于保留类，但在特定语法中可以作为类型名或
--                              函数名使用，通常不能随便当表名或列名用。共 27 个。
--   U（Unreserved）：非保留关键字。限制最少，几乎可以在任何地方用作标识符（表名、列名、别名等），
--                    冲突风险最低。共 569 个。
--   C（Column name）：列名类非保留关键字。属于非保留，但语义上偏向可作列名；不能用作函数名或
--                     类型名，比 U 类受限一些。共 70 个。
-- =============================================================

WITH
-- 1. 参数定义（请在这里修改参数值）
params AS (
    SELECT
         'yth_kf_jfzw_h,yth_kf_schjy_h,yth_kf_goud_h,yth_kf_share_h,yth_kf_yk_h,yth_kf_kefu_h,yth_kf_yj_h,yth_kf_jl_h,yth_kf_dlgyxq_h,yth_kf_xs_h,yth_kf_jc_h,yth_kf_xtjc_h,yth_kf_xtzc_h,yth_kf_glback_h,yth_kf_glbb_h,yth_kf_glcx_h' AS target_schemas,   -- 【替换】多个模式，逗号分隔
        NULL            AS target_table      -- 【替换】目标表名，NULL表示所有表
),

-- 2. 将模式列表拆分为数组
schema_array AS (
    SELECT string_to_array((SELECT target_schemas FROM params), ',') AS schemas
),

-- 3. 获取指定模式下所有普通表（排除系统表、视图等）
tables_to_check AS (
    SELECT
        c.oid AS table_oid,
        n.nspname AS schema_name,
        c.relname AS table_name
    FROM
        pg_class c
        JOIN pg_namespace n ON c.relnamespace = n.oid
        CROSS JOIN params p
        CROSS JOIN schema_array sa
    WHERE
        n.nspname = ANY(sa.schemas)          -- 模式在列表中
        AND c.relkind = 'r'                  -- 只检查普通表
        AND (p.target_table IS NULL OR c.relname = p.target_table)
),

-- 4. 获取这些表的所有有效列
columns_to_check AS (
    SELECT
        t.schema_name,
        t.table_name,
        a.attname AS column_name
    FROM
        tables_to_check t
        JOIN pg_attribute a ON a.attrelid = t.table_oid
    WHERE
        a.attnum > 0           -- 排除系统列
        AND NOT a.attisdropped -- 排除已删除列
),

-- 5. 获取所有关键字及其类型
all_keywords AS (
    SELECT 
        word,
        catcode,
        CASE catcode
            WHEN 'R' THEN '完全保留关键字。在任何上下文里都不能直接用作对象名（表名、列名、函数名等），除非用双引号包裹。'
            WHEN 'T' THEN '类型或函数名关键字。属于保留类，但在特定语法中可以作为类型名或函数名使用，通常不能随便当表名或列名用。'
            WHEN 'U' THEN '非保留关键字。限制最少，几乎可以在任何地方用作标识符（表名、列名、别名等），冲突风险最低。'
            WHEN 'C' THEN '列名类非保留关键字。属于非保留，但语义上偏向可作列名；不能用作函数名或类型名，比U类受限一些。'
            ELSE '未知类型 (' || catcode || ')'
        END AS keyword_type_desc
    FROM pg_get_keywords()
)

-- 6. 找出冲突的列，输出表信息和列信息，以及关键字类型
SELECT
    c.schema_name AS 模式,
    c.table_name  AS 表名,
    c.column_name AS 冲突列名,
    k.keyword_type_desc AS 关键字类型说明,
    '与关键字 "' || c.column_name || '" 冲突' AS 冲突说明
FROM
    columns_to_check c
    JOIN all_keywords k ON c.column_name = k.word
ORDER BY
    c.schema_name, c.table_name, c.column_name