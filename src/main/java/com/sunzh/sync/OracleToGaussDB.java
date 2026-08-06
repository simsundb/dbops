package com.sunzh.sync;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Oracle 表 -> GaussDB 表 同步工具（v3：支持表重命名 + 追加模式）。
 *
 * <h3>核心功能</h3>
 * <ul>
 *   <li><b>表重命名</b> —— Oracle 表名可以映射为不同的 GaussDB 表名（格式：ORA_NAME:GAUSS_NAME）</li>
 *   <li><b>追加模式</b> —— {@code --append} 标志：跳过建表，直接将数据追加到已有的 GaussDB 表中</li>
 *   <li><b>覆盖模式（默认）</b> —— 在 GaussDB 中创建与 Oracle 结构一致的表（若已存在则自动覆盖），全量导入数据</li>
 * </ul>
 *
 * <h3>用法</h3>
 * <pre>{@code
 * java -cp ".:lib/*" OracleToGaussDB3 \
 *   <源URL> <源用户> <源密码> \
 *   <目标URL> <目标用户> <目标密码> \
 *   <表映射...> [--append] [--yes]
 * }</pre>
 *
 * <h3>表映射格式</h3>
 * <ul>
 *   <li>{@code TABLE1} — Oracle 和 GaussDB 同名（GaussDB 自动转小写）</li>
 *   <li>{@code ORA_NAME:GAUSS_NAME} — Oracle 表 ORA_NAME 映射到 GaussDB 表 GAUSS_NAME</li>
 *   <li>多张表用逗号分隔：{@code A,B:NEW_B,C:ANOTHER_C}</li>
 * </ul>
 *
 * <h3>示例</h3>
 * <pre>{@code
 * # 覆盖模式（默认）：三张表同名同步
 * java -cp ".:lib/*" OracleToGaussDB3 \
 *   "jdbc:oracle:thin:@//172.20.36.45:1521/hydb" ZDHSQD_GD ZDHSQD_GD \
 *   "jdbc:gaussdb://172.19.136.3:8000/muts" gktest gktest_2026 \
 *   EMPLOYEES,DEPARTMENTS,JOBS
 *
 * # 表重命名：Oracle EMP -> GaussDB employees_new
 * java -cp ".:lib/*" OracleToGaussDB3 \
 *   "jdbc:oracle:thin:@//172.20.36.45:1521/hydb" ZDHSQD_GD ZDHSQD_GD \
 *   "jdbc:gaussdb://172.19.136.3:8000/muts" gktest gktest_2026 \
 *   EMP:EMPLOYEES_NEW,DEPT:DEPARTMENTS_NEW
 *
 * # 追加模式：将 Oracle 数据追加到 GaussDB 已有表中（不建表不删表）
 * java -cp ".:lib/*" OracleToGaussDB3 \
 *   "jdbc:oracle:thin:@//172.20.36.45:1521/hydb" ZDHSQD_GD ZDHSQD_GD \
 *   "jdbc:gaussdb://172.19.136.3:8000/muts" gktest gktest_2026 \
 *   EMPLOYEES,DEPARTMENTS --append
 * }</pre>
 *
 * @author Modified version - automatic overwrite without interaction
 */
public class OracleToGaussDB {

    private static final int FETCH_SIZE  = 5000;
    private static final int BATCH_SIZE  = 5000;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── 运行模式 ──────────────────────────────────────────────
    private boolean appendMode  = false;   // true = 追加模式，不建表

    public static void main(String[] args) throws Exception {
        new OracleToGaussDB().run(args);
    }

    // ═══════════════════════════════════════════════════════════
    //  主流程
    // ═══════════════════════════════════════════════════════════

    private void run(String[] args) throws Exception {
        // ── 1. 解析命令行参数 ────────────────────────────────
        ParsedArgs pa = parseArgs(args);
        if (pa == null) {
            printUsageAndExit();
        }

        log("========================================");
        log("  Oracle -> GaussDB 表同步工具 v3");
        log("  模式: " + (appendMode ? "追加 (APPEND)" : "覆盖 (RECREATE)"));
        log("========================================");
        log("源端: " + pa.srcUrl);
        log("目标: " + pa.tgtUrl);
        log("待同步表 (" + pa.mappings.size() + " 张):");
        for (TableMapping m : pa.mappings) {
            log(String.format("  %s  ->  %s", m.oracleName, m.gaussName));
        }
        log("FetchSize: " + FETCH_SIZE + " / BatchSize: " + BATCH_SIZE);

        Class.forName("oracle.jdbc.driver.OracleDriver");

        int succCount = 0;
        int failCount = 0;
        long totalRows = 0;
        LocalDateTime overallStart = LocalDateTime.now();

        try (Connection srcConn = DriverManager.getConnection(pa.srcUrl, pa.srcUser, pa.srcPassword);
             Connection tgtConn = DriverManager.getConnection(pa.tgtUrl, pa.tgtUser, pa.tgtPassword)) {

            srcConn.setReadOnly(true);
            tgtConn.setAutoCommit(false);

            for (int ti = 0; ti < pa.mappings.size(); ti++) {
                TableMapping m  = pa.mappings.get(ti);
                String oraTable = m.oracleName;
                String gaussTable = m.gaussName;
                LocalDateTime tableStart = LocalDateTime.now();

                log("");
                log("========================================");
                log("  [" + (ti + 1) + "/" + pa.mappings.size() + "] 开始同步: " + oraTable + " -> " + gaussTable);
                log("========================================");

                try {
                    // ── 2. 读取 Oracle 表结构 ──────────────────
                    log("--- 读取 Oracle 表结构 (" + oraTable + ") ---");
                    List<ColInfo> columns = readTableColumns(srcConn, oraTable);
                    if (columns.isEmpty()) {
                        log("ERROR: 表 " + oraTable + " 不存在或无列信息, 跳过");
                        failCount++;
                        continue;
                    }
                    for (ColInfo col : columns) {
                        log(String.format("  %-30s %-20s -> %s", col.name, col.oracleType, col.gaussName));
                    }
                    log("共 " + columns.size() + " 列");

                    // ── 3. 覆盖模式：建表逻辑（自动覆盖） ──────────
                    if (!appendMode) {
                        String createDdl = buildCreateGaussDdl(gaussTable, columns);
                        log("");
                        log("--- GaussDB 建表 DDL ---");
                        log(createDdl);

                        // 如果表已存在，先删除
                        if (tableExistsInGaussdb(tgtConn, gaussTable)) {
                            try (Statement stmt = tgtConn.createStatement()) {
                                stmt.execute("DROP TABLE IF EXISTS " + gaussTable);
                                log(">>> 已删除旧表 " + gaussTable + " (自动覆盖) <<<");
                            }
                        }

                        // 执行建表
                        try (Statement stmt = tgtConn.createStatement()) {
                            stmt.execute(createDdl);
                            log("");
                            log(">>> 表 " + gaussTable + " 在 GaussDB 中创建成功 <<<");
                        } catch (SQLException e) {
                            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                                try (Statement stmt = tgtConn.createStatement()) {
                                    stmt.execute("DROP TABLE IF EXISTS " + gaussTable);
                                    stmt.execute(createDdl);
                                }
                                log("");
                                log(">>> 表 " + gaussTable + " (覆盖) 创建成功 <<<");
                            } else {
                                throw e;
                            }
                        }
                    } else {
                        // ── 追加模式：仅检查目标表是否存在 ──────
                        log("");
                        log("--- 追加模式：跳过建表，检查目标表是否存在 ---");
                        boolean tableExists = tableExistsInGaussdb(tgtConn, gaussTable);
                        if (!tableExists) {
                            log("ERROR: 追加模式下目标表 " + gaussTable + " 不存在, 跳过");
                            failCount++;
                            continue;
                        }
                        log(">>> 目标表 " + gaussTable + " 已存在，将追加数据 <<<");

                        // 追加模式下也检查 Oracle 和目标表的列兼容性
                        List<ColInfo> gaussCols = readGaussTableColumns(tgtConn, gaussTable);
                        if (gaussCols.isEmpty()) {
                            log("WARNING: 无法读取 GaussDB 表 " + gaussTable + " 的列信息，将按 Oracle 列结构插入");
                        } else {
                            log("GaussDB 目标表共 " + gaussCols.size() + " 列");
                            // 按 GaussDB 实际列顺序调整插入（列名按小写匹配）
                            columns = alignColumnsForAppend(columns, gaussCols);
                            if (columns == null) {
                                log("ERROR: Oracle 与 GaussDB 列不兼容, 跳过");
                                failCount++;
                                continue;
                            }
                        }
                    }

                    // ── 4. 流式读取 Oracle 并批量写入 GaussDB ────
                    log("");
                    log("--- 开始导入 (流式读取, 每 " + BATCH_SIZE + " 行提交) ---");

                    String selectSql = buildSelectSql(oraTable, columns);
                    String insertSql = buildInsertSql(gaussTable, columns);
                    long count = 0;

                    try (Statement srcStmt = srcConn.createStatement();
                         PreparedStatement pstmt = tgtConn.prepareStatement(insertSql)) {

                        srcStmt.setFetchSize(FETCH_SIZE);

                        try (ResultSet rs = srcStmt.executeQuery(selectSql)) {
                            while (rs.next()) {
                                for (int i = 0; i < columns.size(); i++) {
                                    setParamByType(pstmt, i + 1, rs, columns.get(i));
                                }
                                pstmt.addBatch();
                                count++;

                                if (count % BATCH_SIZE == 0) {
                                    pstmt.executeBatch();
                                    pstmt.clearBatch();
                                    tgtConn.commit();
                                    log(String.format("  [%s] 已提交 %d 行, 耗时 %s",
                                            now(), count, elapsedSince(tableStart)));
                                }
                            }
                        }
                        if (count % BATCH_SIZE != 0) {
                            pstmt.executeBatch();
                            pstmt.clearBatch();
                            tgtConn.commit();
                        }
                    }

                    totalRows += count;
                    log("");
                    log("  [" + (ti + 1) + "/" + pa.mappings.size() + "] " + oraTable + " 完成: " + count + " 行, 耗时 " +
                            Duration.between(tableStart, LocalDateTime.now()).toString().substring(2));
                    succCount++;

                } catch (Exception e) {
                    failCount++;
                    log("ERROR: 表 " + oraTable + " 同步失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // ── 汇总 ────────────────────────────────────────
            log("");
            log("========================================");
            log("  全部同步完成");
            log("========================================");
            log("  模式:      " + (appendMode ? "追加 (APPEND)" : "覆盖 (RECREATE)"));
            log("  总表数:    " + pa.mappings.size());
            log("  成功:      " + succCount);
            log("  失败:      " + failCount);
            log("  总行数:    " + totalRows);
            log("  总耗时:    " + Duration.between(overallStart, LocalDateTime.now()).toString().substring(2));
            log("========================================");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  命令行解析
    // ═══════════════════════════════════════════════════════════

    static class TableMapping {
        final String oracleName;
        final String gaussName;
        TableMapping(String oracleName, String gaussName) {
            this.oracleName = oracleName;
            this.gaussName  = gaussName;
        }
    }

    static class ParsedArgs {
        final String srcUrl;
        final String srcUser;
        final String srcPassword;
        final String tgtUrl;
        final String tgtUser;
        final String tgtPassword;
        final List<TableMapping> mappings;
        ParsedArgs(String srcUrl, String srcUser, String srcPassword,
                   String tgtUrl, String tgtUser, String tgtPassword,
                   List<TableMapping> mappings) {
            this.srcUrl      = srcUrl;
            this.srcUser     = srcUser;
            this.srcPassword = srcPassword;
            this.tgtUrl      = tgtUrl;
            this.tgtUser     = tgtUser;
            this.tgtPassword = tgtPassword;
            this.mappings    = mappings;
        }
    }

    /**
     * 解析命令行参数。
     * 必需参数（位置）: srcUrl srcUser srcPwd tgtUrl tgtUser tgtPwd tableMappings
     * 可选标志（可出现在任意位置）: --append
     */
    private ParsedArgs parseArgs(String[] args) {
        List<String> positional = new ArrayList<>();
        List<String> flags     = new ArrayList<>();

        for (String arg : args) {
            if (arg.startsWith("--")) {
                flags.add(arg.toLowerCase());
            } else {
                positional.add(arg);
            }
        }

        // 处理标志
        appendMode = flags.contains("--append");

        if (positional.size() < 7) return null;

        String srcUrl      = positional.get(0);
        String srcUser     = positional.get(1);
        String srcPassword = positional.get(2);
        String tgtUrl      = positional.get(3);
        String tgtUser     = positional.get(4);
        String tgtPassword = positional.get(5);
        String tableSpec   = positional.get(6).trim();

        // 解析表映射
        List<TableMapping> mappings = new ArrayList<>();
        String[] parts = tableSpec.split("\\s*,\\s*");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            String[] kv = part.split("\\s*:\\s*", 2);
            String oraName  = kv[0].toUpperCase().trim();
            String gaussName;
            if (kv.length == 2 && !kv[1].trim().isEmpty()) {
                // 显式指定 GaussDB 表名（保持用户指定的大小写，通常是小写）
                gaussName = kv[1].trim().toLowerCase();
            } else {
                // 默认：Oracle 表名转小写作为 GaussDB 表名
                gaussName = oraName.toLowerCase();
            }
            mappings.add(new TableMapping(oraName, gaussName));
        }

        if (mappings.isEmpty()) return null;

        return new ParsedArgs(srcUrl, srcUser, srcPassword, tgtUrl, tgtUser, tgtPassword, mappings);
    }

    private void printUsageAndExit() {
        log("用法: java -cp \".:lib/*\" OracleToGaussDB \\");
        log("        <源URL> <源用户> <源密码> \\");
        log("        <目标URL> <目标用户> <目标密码> \\");
        log("        <表映射...> [--append]");
        log("");
        log("参数说明:");
        log("  源URL            Oracle JDBC 连接地址");
        log("  源用户            Oracle 登录用户名");
        log("  源密码            Oracle 登录密码");
        log("  目标URL           GaussDB JDBC 连接地址");
        log("  目标用户           GaussDB 登录用户名");
        log("  目标密码           GaussDB 登录密码");
        log("  表映射            表名列表，支持重命名（见下方格式）");
        log("");
        log("表映射格式:");
        log("  TABLE1                     Oracle/GaussDB 同名（GaussDB 自动转小写）");
        log("  ORA_NAME:GAUSS_NAME        Oracle 表映射到不同名的 GaussDB 表");
        log("  多表逗号分隔:  A,B:NEW_B,C:ANOTHER_C");
        log("");
        log("可选标志:");
        log("  --append      追加模式：不建表，直接向已有 GaussDB 表追加数据");
        log("");
        log("完整示例:");
        log("  # 覆盖模式（默认）：同名同步");
        log("  java -cp \".:lib/*\" OracleToGaussDB \\");
        log("    \"jdbc:oracle:thin:@//172.20.36.45:1521/hydb\" ZDHSQD_GD ZDHSQD_GD \\");
        log("    \"jdbc:gaussdb://172.19.136.3:8000/muts\" gktest gktest_2026 \\");
        log("    EMPLOYEES,DEPARTMENTS,JOBS");
        log("");
        log("  # 表重命名");
        log("  java -cp \".:lib/*\" OracleToGaussDB \\");
        log("    ... EMP:EMPLOYEES_NEW,DEPT:DEPARTMENTS_NEW");
        log("");
        log("  # 追加模式");
        log("  java -cp \".:lib/*\" OracleToGaussDB\\");
        log("    ... EMPLOYEES,DEPARTMENTS --append");
        System.exit(1);
    }

    // ═══════════════════════════════════════════════════════════
    //  表/列 元数据读取
    // ═══════════════════════════════════════════════════════════

    private boolean tableExistsInGaussdb(Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT 1 FROM pg_tables WHERE tablename = '" + tableName + "'")) {
            return rs.next();
        } catch (SQLException e) {
            return tableExistsBySelect(conn, tableName);
        }
    }

    private boolean tableExistsBySelect(Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1 FROM " + tableName + " WHERE 1=0");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /** 读取 Oracle 表的列定义。 */
    private List<ColInfo> readTableColumns(Connection conn, String tableName) throws SQLException {
        List<ColInfo> columns = new ArrayList<>();
        String sql =
            "SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, DATA_PRECISION, DATA_SCALE " +
            "FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? ORDER BY COLUMN_ID";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ColInfo col = new ColInfo();
                    col.name        = rs.getString("COLUMN_NAME");
                    col.gaussName   = col.name.toLowerCase();
                    col.oracleType  = rs.getString("DATA_TYPE");
                    col.length      = rs.getInt("DATA_LENGTH");
                    col.precision   = rs.getInt("DATA_PRECISION");
                    col.scale       = rs.getInt("DATA_SCALE");
                    col.gaussdbType = mapType(col.oracleType, col.length, col.precision, col.scale);
                    columns.add(col);
                }
            }
        }
        return columns;
    }

    /** 读取 GaussDB 表的列定义（用于追加模式下列对齐）。 */
    private List<ColInfo> readGaussTableColumns(Connection conn, String tableName) throws SQLException {
        List<ColInfo> columns = new ArrayList<>();
        String sql =
            "SELECT column_name, data_type, character_maximum_length, " +
            "       numeric_precision, numeric_scale, ordinal_position " +
            "FROM information_schema.columns " +
            "WHERE table_name = ? ORDER BY ordinal_position";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ColInfo col = new ColInfo();
                    col.gaussName  = rs.getString("column_name").toLowerCase();
                    col.oracleType = rs.getString("data_type");   // 复用字段存 GaussDB 类型
                    col.precision  = rs.getInt("numeric_precision");
                    col.scale      = rs.getInt("numeric_scale");
                    columns.add(col);
                }
            }
        }
        return columns;
    }

    /**
     * 追加模式下，按 GaussDB 目标表的实际列顺序重新排列 Oracle 列。
     * 返回重新排列后的列列表；如果存在 GaussDB 中不存在的 Oracle 列则返回 null。
     */
    private List<ColInfo> alignColumnsForAppend(List<ColInfo> oraCols, List<ColInfo> gaussCols) {
        // 构建 Oracle 列名索引
        Map<String, ColInfo> oraMap = new LinkedHashMap<>();
        for (ColInfo c : oraCols) {
            oraMap.put(c.gaussName.toLowerCase(), c);
        }

        List<ColInfo> aligned = new ArrayList<>();
        Set<String> matched = new HashSet<>();

        for (ColInfo gc : gaussCols) {
            ColInfo oc = oraMap.get(gc.gaussName.toLowerCase());
            if (oc != null) {
                aligned.add(oc);
                matched.add(oc.gaussName.toLowerCase());
            } else {
                log("WARNING: GaussDB 列 " + gc.gaussName + " 在 Oracle 中不存在，将设 NULL");
                // 创建一个占位列
                ColInfo dummy = new ColInfo();
                dummy.name       = gc.gaussName.toUpperCase();
                dummy.gaussName  = gc.gaussName.toLowerCase();
                dummy.oracleType = "DUMMY";
                dummy.gaussdbType = gc.oracleType;
                aligned.add(dummy);
            }
        }

        // 检查是否有 Oracle 列在 GaussDB 中不存在
        for (ColInfo oc : oraCols) {
            if (!matched.contains(oc.gaussName.toLowerCase())) {
                log("WARNING: Oracle 列 " + oc.name + " 在 GaussDB 目标表中不存在，将跳过该列数据");
            }
        }

        return aligned;
    }

    // ═══════════════════════════════════════════════════════════
    //  类型映射与 SQL 构建
    // ═══════════════════════════════════════════════════════════

    private String mapType(String oracleType, int length, int precision, int scale) {
        String t = oracleType.toUpperCase();
        switch (t) {
            case "VARCHAR2":
            case "NVARCHAR2":
                if (length > 0 && length <= 2000) return "varchar(" + (length * 2) + ")";
                return "varchar(4000)";
            case "CHAR":
            case "NCHAR":
                if (length > 0) return "char(" + (length * 2) + ")";
                return "char(2)";
            case "NUMBER":
                if (precision == 0 && scale == 0) return "numeric";
                if (scale > 0) return "numeric(" + Math.max(precision, 1) + "," + scale + ")";
                if (precision > 0) return "numeric(" + precision + ")";
                return "numeric";
            case "DATE":
            case "TIMESTAMP":
            case "TIMESTAMP(6)":
            case "TIMESTAMP WITH TIME ZONE":
            case "TIMESTAMP WITH LOCAL TIME ZONE":
                return "timestamp";
            case "CLOB":
            case "NCLOB":
            case "LONG":
                return "text";
            case "BLOB":
            case "RAW":
            case "LONG RAW":
                return "bytea";
            case "FLOAT":
                return "float8";
            case "INTEGER":
            case "INT":
                return "integer";
            default:
                return "varchar(4000)";
        }
    }

    private String buildSelectSql(String tableName, List<ColInfo> columns) {
        StringBuilder sb = new StringBuilder("SELECT ");
        for (int i = 0; i < columns.size(); i++) {
            ColInfo col = columns.get(i);
            // 占位列（GaussDB 有但 Oracle 没有）输出 NULL
            if ("DUMMY".equals(col.oracleType)) {
                sb.append("NULL AS \"").append(col.name).append("\"");
            } else {
                String type = col.oracleType.toUpperCase();
                if (type.contains("DATE") || type.contains("TIMESTAMP")) {
                    sb.append("TO_CHAR(\"").append(col.name).append("\",'YYYY-MM-DD HH24:MI:SS')");
                } else {
                    sb.append("\"").append(col.name).append("\"");
                }
                sb.append(" AS \"").append(col.name).append("\"");
            }
            if (i < columns.size() - 1) sb.append(", ");
        }
        sb.append(" FROM \"").append(tableName).append("\"");
        return sb.toString();
    }

    private String buildCreateGaussDdl(String tableName, List<ColInfo> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName).append(" (\n");
        for (int i = 0; i < columns.size(); i++) {
            ColInfo col = columns.get(i);
            sb.append("  ").append(col.gaussName).append(" ").append(col.gaussdbType);
            if (i < columns.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildInsertSql(String tableName, List<ColInfo> columns) {
        StringBuilder sb = new StringBuilder("INSERT INTO ");
        sb.append(tableName).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(columns.get(i).gaussName);
            if (i < columns.size() - 1) sb.append(", ");
        }
        sb.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            sb.append("?");
            if (i < columns.size() - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════
    //  数据写入
    // ═══════════════════════════════════════════════════════════

    private String now() {
        return LocalDateTime.now().format(TS);
    }

    private String elapsedSince(LocalDateTime start) {
        Duration d = Duration.between(start, LocalDateTime.now());
        long h = d.toHours();
        long m = d.toMinutes() % 60;
        long s = d.getSeconds() % 60;
        if (h > 0) return String.format("%dh %dm %ds", h, m, s);
        if (m > 0) return String.format("%dm %ds", m, s);
        return s + "s";
    }

    private void setParamByType(PreparedStatement pstmt, int idx, ResultSet rs, ColInfo col) throws Exception {
        // 占位列（GaussDB 有但 Oracle 没有的列）设为 NULL
        if ("DUMMY".equals(col.oracleType)) {
            pstmt.setNull(idx, Types.VARCHAR);
            return;
        }

        String type = col.oracleType.toUpperCase();
        if (type.equals("CLOB") || type.equals("NCLOB") || type.equals("LONG")) {
            Clob clob = rs.getClob(idx);
            if (clob != null) {
                pstmt.setString(idx, clob.getSubString(1, (int) clob.length()));
            } else {
                pstmt.setNull(idx, Types.VARCHAR);
            }
        } else if (type.equals("BLOB") || type.equals("RAW") || type.equals("LONG RAW")) {
            Blob blob = rs.getBlob(idx);
            if (blob != null) {
                pstmt.setBytes(idx, blob.getBytes(1, (int) blob.length()));
            } else {
                pstmt.setNull(idx, Types.BINARY);
            }
        } else {
            pstmt.setString(idx, rs.getString(idx));
        }
    }

    private static void log(String msg) {
        System.out.println("[" + LocalDateTime.now().format(TS) + "] " + msg);
    }

    // ═══════════════════════════════════════════════════════════
    //  列信息
    // ═══════════════════════════════════════════════════════════

    static class ColInfo {
        String name;         // Oracle 原始列名（大写）
        String gaussName;    // GaussDB 列名（小写）
        String oracleType;   // Oracle 数据类型
        int    length;
        int    precision;
        int    scale;
        String gaussdbType;  // 映射后的 GaussDB 数据类型
    }
}