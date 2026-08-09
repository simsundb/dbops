
package com.sunzh.sync;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * GaussDB 表 -> Oracle 表 同步工具（反向版本）。
 * 修正了 VARCHAR -> VARCHAR2 的类型映射问题。
 *
 * @author Reverse version of OracleToGaussDB3
 */
public class GaussDBToOracle {

    private static final int FETCH_SIZE  = 5000;
    private static final int BATCH_SIZE  = 5000;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private boolean appendMode = false;
    private boolean silentMode = false;

    public static void main(String[] args) throws Exception {
        ConsoleEncoding.configureUtf8();
        new GaussDBToOracle().run(args);
    }

    // ═══════════════════════════════════════════════════════════
    //  主流程
    // ═══════════════════════════════════════════════════════════

    private void run(String[] args) throws Exception {
        ParsedArgs pa = parseArgs(args);
        if (pa == null) {
            printUsageAndExit();
        }

        log("========================================");
        log("  GaussDB -> Oracle 表同步工具 (反向)");
        log("  模式: " + (appendMode ? "追加 (APPEND)" : "覆盖 (RECREATE)"));
        log("  静默模式: " + (silentMode ? "开启" : "关闭"));
        log("========================================");
        log("源端 (GaussDB): " + pa.srcUrl);
        log("目标 (Oracle):  " + pa.tgtUrl);
        log("待同步表 (" + pa.mappings.size() + " 张):");
        for (TableMapping m : pa.mappings) {
            log(String.format("  %s  ->  %s", m.sourceName, m.targetName));
        }
        log("FetchSize: " + FETCH_SIZE + " / BatchSize: " + BATCH_SIZE);

        // 加载 GaussDB 驱动
        loadGaussDBDriver();

        // 加载 Oracle 驱动
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            log("✅ 成功加载 Oracle 驱动");
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("未找到 Oracle JDBC 驱动 (oracle.jdbc.driver.OracleDriver)，请检查 lib 目录。");
        }

        int succCount = 0;
        int failCount = 0;
        long totalRows = 0;
        LocalDateTime overallStart = LocalDateTime.now();

        try (Connection srcConn = DriverManager.getConnection(pa.srcUrl, pa.srcUser, pa.srcPassword);
             Connection tgtConn = DriverManager.getConnection(pa.tgtUrl, pa.tgtUser, pa.tgtPassword)) {

            srcConn.setReadOnly(true);
            tgtConn.setAutoCommit(false);

            for (int ti = 0; ti < pa.mappings.size(); ti++) {
                TableMapping m = pa.mappings.get(ti);
                String sourceTable = m.sourceName;
                String targetTable = m.targetName;
                LocalDateTime tableStart = LocalDateTime.now();

                log("");
                log("========================================");
                log("  [" + (ti + 1) + "/" + pa.mappings.size() + "] 开始同步: " + sourceTable + " -> " + targetTable);
                log("========================================");

                try {
                    // 读取 GaussDB 表结构
                    log("--- 读取 GaussDB 表结构 (" + sourceTable + ") ---");
                    List<ColInfo> columns = readTableColumns(srcConn, sourceTable);
                    if (columns.isEmpty()) {
                        log("ERROR: 表 " + sourceTable + " 不存在或无列信息, 跳过");
                        failCount++;
                        continue;
                    }
                    for (ColInfo col : columns) {
                        log(String.format("  %-30s %-20s -> %s", col.name, col.sourceType, col.targetType));
                    }
                    log("共 " + columns.size() + " 列");

                    boolean targetExists = tableExistsInOracle(tgtConn, targetTable);

                    if (!appendMode) {
                        // 覆盖模式：删除表并重建
                        if (targetExists) {
                            if (!silentMode) {
                                log("⚠️  WARNING: 目标表 " + targetTable + " 已存在，将删除并重建（覆盖）");
                            }
                            try (Statement stmt = tgtConn.createStatement()) {
                                stmt.execute("DROP TABLE " + targetTable);
                                log(">>> 已删除旧表 " + targetTable + " (自动覆盖) <<<");
                            }
                        }
                        String createDdl = buildCreateOracleDdl(targetTable, columns);
                        log("");
                        log("--- Oracle 建表 DDL ---");
                        log(createDdl);
                        try (Statement stmt = tgtConn.createStatement()) {
                            stmt.execute(createDdl);
                            log("");
                            log(">>> 表 " + targetTable + " 在 Oracle 中创建成功 <<<");
                        }
                    } else {
                        // 追加模式
                        if (!targetExists) {
                            log("WARNING: 追加模式下目标表 " + targetTable + " 不存在，将自动创建表");
                            String createDdl = buildCreateOracleDdl(targetTable, columns);
                            log("");
                            log("--- Oracle 建表 DDL ---");
                            log(createDdl);
                            try (Statement stmt = tgtConn.createStatement()) {
                                stmt.execute(createDdl);
                                log(">>> 表 " + targetTable + " 创建成功 <<<");
                            }
                            targetExists = true;
                        } else {
                            log(">>> 目标表 " + targetTable + " 已存在，将追加数据 <<<");
                            List<ColInfo> oracleCols = readOracleTableColumns(tgtConn, targetTable);
                            if (!oracleCols.isEmpty()) {
                                columns = alignColumnsForAppend(columns, oracleCols);
                                if (columns == null) {
                                    log("ERROR: GaussDB 与 Oracle 列不兼容, 跳过");
                                    failCount++;
                                    continue;
                                }
                            }
                        }
                    }

                    // 导入数据
                    log("");
                    log("--- 开始导入 (流式读取, 每 " + BATCH_SIZE + " 行提交) ---");

                    String selectSql = buildSelectSql(sourceTable, columns);
                    String insertSql = buildInsertSql(targetTable, columns);
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
                    log("  [" + (ti + 1) + "/" + pa.mappings.size() + "] " + sourceTable + " 完成: " + count + " 行, 耗时 " +
                            Duration.between(tableStart, LocalDateTime.now()).toString().substring(2));
                    succCount++;

                } catch (Exception e) {
                    failCount++;
                    log("ERROR: 表 " + sourceTable + " 同步失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            log("");
            log("========================================");
            log("  全部同步完成");
            log("========================================");
            log("  模式:      " + (appendMode ? "追加 (APPEND)" : "覆盖 (RECREATE)"));
            log("  静默模式:  " + (silentMode ? "开启" : "关闭"));
            log("  总表数:    " + pa.mappings.size());
            log("  成功:      " + succCount);
            log("  失败:      " + failCount);
            log("  总行数:    " + totalRows);
            log("  总耗时:    " + Duration.between(overallStart, LocalDateTime.now()).toString().substring(2));
            log("========================================");
        }
    }

    // ── 驱动加载 ──
    private void loadGaussDBDriver() throws ClassNotFoundException {
        String[] gaussDrivers = {
            "com.huawei.gaussdb.jdbc.Driver",
            "org.postgresql.Driver",
            "com.huawei.gauss.jdbc.Driver",
            "com.huawei.gauss200.jdbc.Driver",
            "com.huawei.opengauss.jdbc.Driver"
        };
        for (String driverClass : gaussDrivers) {
            try {
                Class.forName(driverClass);
                log("✅ 成功加载 GaussDB 驱动: " + driverClass);
                return;
            } catch (ClassNotFoundException ignored) {}
        }
        throw new ClassNotFoundException(
            "无法找到 GaussDB JDBC 驱动，请确认 lib 目录下存在正确的驱动 JAR 包。\n" +
            "常见驱动类名包括: " + String.join(", ", gaussDrivers)
        );
    }

    // ═══════════════════════════════════════════════════════════
    //  命令行解析
    // ═══════════════════════════════════════════════════════════

    static class TableMapping {
        final String sourceName;
        final String targetName;
        TableMapping(String sourceName, String targetName) {
            this.sourceName = sourceName;
            this.targetName = targetName;
        }
    }

    static class ParsedArgs {
        final String srcUrl, srcUser, srcPassword, tgtUrl, tgtUser, tgtPassword;
        final List<TableMapping> mappings;
        ParsedArgs(String srcUrl, String srcUser, String srcPassword,
                   String tgtUrl, String tgtUser, String tgtPassword,
                   List<TableMapping> mappings) {
            this.srcUrl = srcUrl; this.srcUser = srcUser; this.srcPassword = srcPassword;
            this.tgtUrl = tgtUrl; this.tgtUser = tgtUser; this.tgtPassword = tgtPassword;
            this.mappings = mappings;
        }
    }

    private ParsedArgs parseArgs(String[] args) {
        List<String> positional = new ArrayList<>();
        List<String> flags = new ArrayList<>();

        for (String arg : args) {
            if (arg.startsWith("--")) {
                flags.add(arg.toLowerCase());
            } else {
                positional.add(arg);
            }
        }

        appendMode = flags.contains("--append");
        silentMode = flags.contains("--yes");

        if (positional.size() < 7) return null;

        String srcUrl = positional.get(0);
        String srcUser = positional.get(1);
        String srcPassword = positional.get(2);
        String tgtUrl = positional.get(3);
        String tgtUser = positional.get(4);
        String tgtPassword = positional.get(5);
        String tableSpec = positional.get(6).trim();

        List<TableMapping> mappings = new ArrayList<>();
        for (String part : tableSpec.split("\\s*,\\s*")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            String[] kv = part.split("\\s*:\\s*", 2);
            String sourceName = kv[0].trim().toLowerCase();
            String targetName = (kv.length == 2 && !kv[1].trim().isEmpty())
                    ? kv[1].trim().toUpperCase()
                    : sourceName.toUpperCase();
            mappings.add(new TableMapping(sourceName, targetName));
        }

        return mappings.isEmpty() ? null : new ParsedArgs(srcUrl, srcUser, srcPassword, tgtUrl, tgtUser, tgtPassword, mappings);
    }

    private void printUsageAndExit() {
        log("用法: java -cp \".:lib/*\" GaussDBToOracle \\");
        log("        <源URL> <源用户> <源密码> \\");
        log("        <目标URL> <目标用户> <目标密码> \\");
        log("        <表映射...> [--append] [--yes]");
        System.exit(1);
    }

    // ═══════════════════════════════════════════════════════════
    //  表/列 元数据读取
    // ═══════════════════════════════════════════════════════════

    private boolean tableExistsInOracle(Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1 FROM " + tableName + " WHERE 1=0");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private List<ColInfo> readTableColumns(Connection conn, String tableName) throws SQLException {
        List<ColInfo> columns = new ArrayList<>();
        String sql = "SELECT column_name, data_type, character_maximum_length, " +
                     "numeric_precision, numeric_scale FROM information_schema.columns " +
                     "WHERE table_name = ? ORDER BY ordinal_position";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ColInfo col = new ColInfo();
                    col.name = rs.getString("column_name");
                    col.targetName = col.name.toUpperCase();
                    col.sourceType = rs.getString("data_type");
                    int maxLen = rs.getInt("character_maximum_length");
                    int precision = rs.getInt("numeric_precision");
                    int scale = rs.getInt("numeric_scale");
                    col.targetType = mapToOracleType(col.sourceType, maxLen, precision, scale);
                    columns.add(col);
                }
            }
        }
        return columns;
    }

    private List<ColInfo> readOracleTableColumns(Connection conn, String tableName) throws SQLException {
        List<ColInfo> columns = new ArrayList<>();
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, DATA_PRECISION, DATA_SCALE " +
                     "FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? ORDER BY COLUMN_ID";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ColInfo col = new ColInfo();
                    col.targetName = rs.getString("COLUMN_NAME").toUpperCase();
                    col.sourceType = rs.getString("DATA_TYPE");
                    col.precision = rs.getInt("DATA_PRECISION");
                    col.scale = rs.getInt("DATA_SCALE");
                    columns.add(col);
                }
            }
        }
        return columns;
    }

    private List<ColInfo> alignColumnsForAppend(List<ColInfo> sourceCols, List<ColInfo> targetCols) {
        Map<String, ColInfo> sourceMap = new LinkedHashMap<>();
        for (ColInfo c : sourceCols) sourceMap.put(c.targetName, c);

        List<ColInfo> aligned = new ArrayList<>();
        Set<String> matched = new HashSet<>();
        for (ColInfo tc : targetCols) {
            ColInfo sc = sourceMap.get(tc.targetName);
            if (sc != null) {
                aligned.add(sc);
                matched.add(tc.targetName);
            } else {
                log("WARNING: Oracle 列 " + tc.targetName + " 在 GaussDB 中不存在，将设 NULL");
                ColInfo dummy = new ColInfo();
                dummy.name = tc.targetName.toLowerCase();
                dummy.targetName = tc.targetName;
                dummy.sourceType = "DUMMY";
                dummy.targetType = tc.sourceType;
                aligned.add(dummy);
            }
        }
        for (ColInfo sc : sourceCols) {
            if (!matched.contains(sc.targetName)) {
                log("WARNING: GaussDB 列 " + sc.name + " 在 Oracle 中不存在，将跳过该列数据");
            }
        }
        return aligned;
    }

    // ═══════════════════════════════════════════════════════════
    //  ★★★ 修正后的类型映射 ★★★
    // ═══════════════════════════════════════════════════════════

    private String mapToOracleType(String gaussType, int maxLen, int precision, int scale) {
        String t = gaussType.toLowerCase();
        
        // VARCHAR / CHARACTER VARYING -> VARCHAR2
        if (t.startsWith("character varying") || t.startsWith("varchar")) {
            // 如果 maxLen <= 0，使用默认长度 4000
            // 如果 maxLen > 4000，也使用 4000（Oracle VARCHAR2 最大 4000）
            int len = (maxLen > 0 && maxLen <= 4000) ? maxLen : 4000;
            return "VARCHAR2(" + len + ")";
        }
        
        // CHAR / CHARACTER -> CHAR
        else if (t.startsWith("character") || t.startsWith("char")) {
            int len = (maxLen > 0 && maxLen <= 2000) ? maxLen : 255;
            return "CHAR(" + len + ")";
        }
        
        // NUMERIC / DECIMAL -> NUMBER
        else if (t.startsWith("numeric") || t.startsWith("decimal")) {
            if (precision == 0 && scale == 0) return "NUMBER";
            if (scale > 0) return "NUMBER(" + Math.max(precision, 1) + "," + scale + ")";
            if (precision > 0) return "NUMBER(" + precision + ")";
            return "NUMBER";
        }
        
        // 整数类型
        else if (t.startsWith("integer") || t.startsWith("int4")) {
            return "NUMBER(10)";
        } else if (t.startsWith("bigint") || t.startsWith("int8")) {
            return "NUMBER(19)";
        } else if (t.startsWith("smallint") || t.startsWith("int2")) {
            return "NUMBER(5)";
        }
        
        // 浮点类型
        else if (t.startsWith("real") || t.startsWith("float4")) {
            return "FLOAT(24)";
        } else if (t.startsWith("double precision") || t.startsWith("float8")) {
            return "FLOAT(53)";
        }
        
        // 日期时间
        else if (t.startsWith("timestamp")) {
            return "TIMESTAMP";
        } else if (t.startsWith("date")) {
            return "DATE";
        }
        
        // 大文本
        else if (t.startsWith("text") || t.startsWith("clob") || t.startsWith("long")) {
            return "CLOB";
        }
        
        // 二进制
        else if (t.startsWith("bytea") || t.startsWith("blob") || t.startsWith("raw")) {
            return "BLOB";
        }
        
        // 其他类型默认使用 VARCHAR2(4000)
        else {
            return "VARCHAR2(4000)";
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SQL 构建
    // ═══════════════════════════════════════════════════════════

    private String buildSelectSql(String tableName, List<ColInfo> columns) {
        StringBuilder sb = new StringBuilder("SELECT ");
        for (int i = 0; i < columns.size(); i++) {
            ColInfo col = columns.get(i);
            if ("DUMMY".equals(col.sourceType)) {
                sb.append("NULL AS \"").append(col.name).append("\"");
            } else {
                sb.append("\"").append(col.name).append("\"");
            }
            if (i < columns.size() - 1) sb.append(", ");
        }
        sb.append(" FROM \"").append(tableName).append("\"");
        return sb.toString();
    }

    private String buildCreateOracleDdl(String tableName, List<ColInfo> columns) {
        StringBuilder sb = new StringBuilder("CREATE TABLE ").append(tableName).append(" (\n");
        for (int i = 0; i < columns.size(); i++) {
            ColInfo col = columns.get(i);
            sb.append("  ").append(col.targetName).append(" ").append(col.targetType);
            if (i < columns.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildInsertSql(String tableName, List<ColInfo> columns) {
        StringBuilder sb = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(columns.get(i).targetName);
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

    private void setParamByType(PreparedStatement pstmt, int idx, ResultSet rs, ColInfo col) throws Exception {
        if ("DUMMY".equals(col.sourceType)) {
            pstmt.setNull(idx, Types.VARCHAR);
            return;
        }

        String type = col.sourceType.toLowerCase();
        if (type.startsWith("text") || type.startsWith("clob") || type.startsWith("long")) {
            String val = rs.getString(idx);
            if (val != null) {
                pstmt.setString(idx, val);
            } else {
                pstmt.setNull(idx, Types.VARCHAR);
            }
        } else if (type.startsWith("bytea") || type.startsWith("blob") || type.startsWith("raw")) {
            byte[] bytes = rs.getBytes(idx);
            if (bytes != null) {
                pstmt.setBytes(idx, bytes);
            } else {
                pstmt.setNull(idx, Types.BINARY);
            }
        } else {
            pstmt.setString(idx, rs.getString(idx));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  工具方法
    // ═══════════════════════════════════════════════════════════

    private static void log(String msg) {
        System.out.println("[" + LocalDateTime.now().format(TS) + "] " + msg);
    }

    private String now() {
        return LocalDateTime.now().format(TS);
    }

    private String elapsedSince(LocalDateTime start) {
        Duration d = Duration.between(start, LocalDateTime.now());
        long h = d.toHours(), m = d.toMinutes() % 60, s = d.getSeconds() % 60;
        if (h > 0) return String.format("%dh %dm %ds", h, m, s);
        if (m > 0) return String.format("%dm %ds", m, s);
        return s + "s";
    }

    // ═══════════════════════════════════════════════════════════
    //  列信息类
    // ═══════════════════════════════════════════════════════════

    static class ColInfo {
        String name;
        String targetName;
        String sourceType;
        String targetType;
        int precision;
        int scale;
    }
}