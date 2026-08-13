package com.sunzh.sync;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Excel -> 数据库 通用导入引擎（Oracle / GaussDB 共用）。
 *
 * 特性：
 *  1. 多 Sheet：每个 Sheet 单独创建一张表
 *  2. 表名 = 文件名解析_<Sheet名解析>；Sheet名无法解析时退化为 _<序号>（_1 表示第一个 Sheet）
 *  3. 行级异常处理：某一行插入失败时记录该行并继续，不影响其余行
 *  4. 有异常行时，生成 <文件名>_异常记录.xlsx（含 汇总 + 各 Sheet 的错误明细）
 *
 * 用法: java -cp ".:lib/*" ExcelImportEngine <jdbc-url> <user> <password> <excel-file>
 */
public class ExcelImportEngine {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int BATCH_SIZE = 500;

    // ===================== 数据库方言差异 =====================
    public interface Dialect {
        String name();
        String driverClass();
        /** 删除旧表的 SQL（不存在时不能报错） */
        String dropTableDdl(String tableName);
        /** 列类型，如 VARCHAR2(255) / VARCHAR(255) */
        String columnType(int maxLen);
        /** 表名规范化（去特殊字符/中文转拼音，Oracle 大写 / GaussDB 小写） */
        String toTableName(String base);
        /** 列名规范化 */
        String toColumnName(String name);
        /** 标识符最大长度 */
        int maxTableNameLen();
    }

    public static final Dialect ORACLE = new Dialect() {
        @Override public String name() { return "Oracle"; }
        @Override public String driverClass() { return "oracle.jdbc.driver.OracleDriver"; }
        @Override public String dropTableDdl(String t) {
            return "BEGIN EXECUTE IMMEDIATE 'DROP TABLE \"" + t + "\"'; EXCEPTION WHEN OTHERS THEN NULL; END;";
        }
        @Override public String columnType(int len) { return "VARCHAR2(" + len + ")"; }
        @Override public String toTableName(String base) { return normalizeName(base, true, "T", maxTableNameLen()); }
        @Override public String toColumnName(String name) { return normalizeName(name, true, "C", 30); }
        @Override public int maxTableNameLen() { return 30; }
    };

    public static final Dialect GAUSSDB = new Dialect() {
        @Override public String name() { return "GaussDB"; }
        @Override public String driverClass() { return "com.huawei.gaussdb.jdbc.Driver"; }
        @Override public String dropTableDdl(String t) { return "DROP TABLE IF EXISTS \"" + t + "\""; }
        @Override public String columnType(int len) { return "VARCHAR(" + len + ")"; }
        @Override public String toTableName(String base) { return normalizeName(base, false, "T", maxTableNameLen()); }
        @Override public String toColumnName(String name) { return normalizeName(name, false, "C", 30); }
        @Override public int maxTableNameLen() { return 63; }
    };

    // ===================== 数据结构 =====================
    /** 一条错误行记录 */
    public static class ErrorRecord {
        final String sheetName;
        final int excelRow;        // Excel 行号（1 起，含表头）
        final List<String> values; // 该行各列原文
        final String message;      // 数据库错误信息

        ErrorRecord(String sheetName, int excelRow, List<String> values, String message) {
            this.sheetName = sheetName;
            this.excelRow = excelRow;
            this.values = values;
            this.message = message;
        }
    }

    /** 单个 Sheet 的导入结果 */
    public static class SheetResult {
        String sheetName;
        String tableName;
        List<String> rawHeaders = new ArrayList<>();
        List<String> colNames = new ArrayList<>();
        int success;
        int fail;
        boolean tableCreated;
        String createError;
        final List<ErrorRecord> errors = new ArrayList<>();

        int dataRows() { return success + fail; }
    }

    // ===================== 入口 =====================
    /**
     * 执行导入。
     * @return 进程退出码：0 成功（含部分行异常，已写入异常记录文件），1 致命错误
     */
    public static int run(Dialect d, String dbUrl, String dbUser, String dbPassword, String excelFilePath) {
        log("========================================");
        log("  Excel -> " + d.name() + " 导入工具（多Sheet版）");
        log("========================================");
        log("数据库: " + dbUrl);
        log("用户:   " + dbUser);
        log("文件:   " + excelFilePath);

        File excelFile = new File(excelFilePath);
        if (!excelFile.exists()) {
            log("ERROR: 文件不存在 - " + excelFilePath);
            return 1;
        }

        String rawName = excelFile.getName();
        String baseName = rawName.substring(0, Math.max(rawName.lastIndexOf('.'), 0));
        String baseAscii = parseToken(baseName);
        if (baseAscii == null) baseAscii = "T_EXCEL";

        log("源文件:   " + rawName);
        log("表名前缀: " + baseAscii);

        try {
            Class.forName(d.driverClass());
        } catch (ClassNotFoundException e) {
            log("ERROR: 驱动加载失败 - " + d.driverClass() + " (" + e.getMessage() + ")");
            return 1;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            int sheetCount = workbook.getNumberOfSheets();
            if (sheetCount == 0) {
                log("ERROR: 工作簿中没有工作表");
                return 1;
            }
            log("共 " + sheetCount + " 个 Sheet\n");

            List<SheetResult> results = new ArrayList<>();
            Set<String> usedSuffixes = new HashSet<>();

            for (int i = 0; i < sheetCount; i++) {
                results.add(importSheet(d, conn, workbook.getSheetAt(i), i, baseAscii, usedSuffixes));
            }

            File errFile = null;
            if (hasAnyError(results)) {
                errFile = writeErrorWorkbook(excelFile, baseName, results);
            }
            printSummary(excelFilePath, results, errFile);
            return 0;
        } catch (Exception e) {
            log("ERROR: " + e.getMessage());
            e.printStackTrace(System.out);
            return 1;
        }
    }

    // ===================== 单个 Sheet =====================
    private static SheetResult importSheet(Dialect d, Connection conn, Sheet sheet, int idx,
                                           String baseAscii, Set<String> usedSuffixes) throws SQLException {
        SheetResult r = new SheetResult();
        r.sheetName = sheet.getSheetName();
        log("=========== Sheet[" + (idx + 1) + "] " + r.sheetName + " ===========");

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            log("WARN: 该 Sheet 第 1 行为空，无表头，跳过建表");
            r.createError = "无表头";
            return r;
        }

        // 列映射
        r.rawHeaders = readHeaders(headerRow);
        r.colNames = normalizeHeaders(d, r.rawHeaders);
        log("--- 列映射 ---");
        for (int i = 0; i < r.rawHeaders.size(); i++) {
            log(String.format("  %-40s -> %s", r.rawHeaders.get(i), r.colNames.get(i)));
        }
        log("共 " + r.colNames.size() + " 列");

        // 探测列长
        log("--- 采样探测列长度 (前50行) ---");
        int[] maxLen = probeMaxLength(sheet, r.colNames.size());
        for (int i = 0; i < r.colNames.size(); i++) {
            log(String.format("  %-30s %s", r.colNames.get(i), d.columnType(maxLen[i])));
        }

        // 表名：<前缀>_<Sheet名解析>，无法解析则 <前缀>_<序号>
        String suffix = sheetSuffix(sheet, idx);
        r.tableName = buildTableName(d, baseAscii, suffix, usedSuffixes);
        log("目标表: " + r.tableName);

        // 建表
        String createDdl = buildCreateDdl(d, r.tableName, r.colNames, maxLen);
        log("--- 建表 DDL ---");
        log(createDdl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(d.dropTableDdl(r.tableName));
            stmt.execute(createDdl);
            r.tableCreated = true;
            log(">>> 表 " + r.tableName + " 创建成功 <<<");
        } catch (SQLException e) {
            r.createError = e.getMessage();
            log("ERROR: 表 " + r.tableName + " 创建失败 - " + e.getMessage() + "（该 Sheet 数据未导入）");
            return r;
        }

        // 导入数据
        importData(d, conn, sheet, r);
        return r;
    }

    private static void importData(Dialect d, Connection conn, Sheet sheet, SheetResult r) throws SQLException {
        int totalRows = sheet.getLastRowNum(); // 不含表头
        String insertSql = buildInsertSql(r.tableName, r.colNames);
        log("");
        log("--- 开始导入数据 (共 " + totalRows + " 行) ---");

        int skipped = 0;
        List<Integer> batchRows = new ArrayList<>(); // 当前批次对应的 Excel 行号
        List<String[]> batchVals = new ArrayList<>(); // 当前批次对应的行值

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (int rr = 1; rr <= sheet.getLastRowNum(); rr++) {
                Row row = sheet.getRow(rr);
                if (row == null) { skipped++; continue; }
                if (isEmptyRow(row, r.colNames.size())) { skipped++; continue; }

                String[] vals = readRowValues(row, r.colNames.size());
                try {
                    setParams(pstmt, vals);
                    pstmt.addBatch();
                    batchRows.add(rr + 1);
                    batchVals.add(vals);
                } catch (SQLException e) {
                    // 极少数：设置参数即失败，视为该行异常
                    recordError(r, rr + 1, vals, e.getMessage());
                }

                if (batchRows.size() >= BATCH_SIZE) {
                    flushBatch(conn, pstmt, r, batchRows, batchVals, rr, totalRows, skipped);
                }
            }
            flushBatch(conn, pstmt, r, batchRows, batchVals, sheet.getLastRowNum(), totalRows, skipped);
        }

        log("");
        log(String.format("  Sheet[%s] 完成: 成功 %d 行, 失败 %d 行, 空行跳过 %d 行, 目标表 %s",
                r.sheetName, r.success, r.fail, skipped, r.tableName));
    }

    /**
     * 批量提交。整批失败时逐行重试，精确隔离出错的那一行。
     * 关键：批处理放在手动事务里——批次失败整体 rollback，避免 Oracle 等驱动
     * 把已执行的部分行先提交后，逐行重插造成重复数据。
     */
    private static void flushBatch(Connection conn, PreparedStatement pstmt, SheetResult r,
                                   List<Integer> batchRows, List<String[]> batchVals,
                                   int curRow, int totalRows, int skipped) throws SQLException {
        if (batchRows.isEmpty()) return;
        boolean oldAuto = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            try {
                pstmt.executeBatch();
                conn.commit();
                r.success += batchRows.size();
            } catch (BatchUpdateException e) {
                conn.rollback(); // 整批回滚：丢弃可能已部分提交的行
                pstmt.clearBatch();
                // 逐行重插：每条好行独立提交，失败行单独回滚，互不影响
                for (int i = 0; i < batchRows.size(); i++) {
                    String[] vals = batchVals.get(i);
                    try {
                        setParams(pstmt, vals);
                        pstmt.executeUpdate();
                        conn.commit(); // 单独提交成功行
                        r.success++;
                    } catch (SQLException se) {
                        conn.rollback(); // 仅回滚当前失败行
                        recordError(r, batchRows.get(i), vals, se.getMessage());
                    }
                }
            }
        } finally {
            conn.setAutoCommit(oldAuto);
            batchRows.clear();
            batchVals.clear();
        }

        if (totalRows > 0) {
            double pct = 100.0 * Math.min(curRow, totalRows) / totalRows;
            log(String.format("  [%s] 已处理 %d/%d 行 (%.1f%%), 成功 %d, 失败 %d, 空行 %d",
                    now(), curRow, totalRows, pct, r.success, r.fail, skipped));
        }
    }

    private static void recordError(SheetResult r, int excelRow, String[] vals, String message) {
        r.fail++;
        r.errors.add(new ErrorRecord(r.sheetName, excelRow, Arrays.asList(vals.clone()), message));
        log("  [第 " + excelRow + " 行] 导入失败: " + message);
    }

    // ===================== 异常记录 Excel =====================
    private static boolean hasAnyError(List<SheetResult> results) {
        for (SheetResult r : results) {
            if (r.fail > 0) return true;
        }
        return false;
    }

    static File writeErrorWorkbook(File src, String baseName, List<SheetResult> results) throws IOException {
        File dir = src.getParentFile();
        if (dir == null) dir = new File(".");
        File out = new File(dir, baseName + "_异常记录.xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // ---- 汇总 Sheet ----
            Sheet sum = wb.createSheet("汇总");
            String[] sumCols = {"源Sheet", "目标表", "数据行数", "成功", "失败", "状态"};
            Row sh = sum.createRow(0);
            for (int c = 0; c < sumCols.length; c++) sh.createCell(c).setCellValue(sumCols[c]);
            int sr = 1;
            for (SheetResult r : results) {
                Row row = sum.createRow(sr++);
                row.createCell(0).setCellValue(r.sheetName);
                row.createCell(1).setCellValue(r.tableName == null ? "-" : r.tableName);
                row.createCell(2).setCellValue(r.dataRows());
                row.createCell(3).setCellValue(r.success);
                row.createCell(4).setCellValue(r.fail);
                row.createCell(5).setCellValue(statusText(r));
            }

            // ---- 每个有错误的源 Sheet 一个明细 Sheet ----
            Set<String> usedNames = new HashSet<>();
            usedNames.add("汇总");
            for (SheetResult r : results) {
                if (r.errors.isEmpty()) continue;
                String name = safeSheetName(r.sheetName, usedNames);
                Sheet es = wb.createSheet(name);
                Row h = es.createRow(0);
                h.createCell(0).setCellValue("Excel行号");
                for (int c = 0; c < r.rawHeaders.size(); c++) {
                    h.createCell(c + 1).setCellValue(r.rawHeaders.get(c));
                }
                h.createCell(r.rawHeaders.size() + 1).setCellValue("错误信息");

                int er = 1;
                for (ErrorRecord rec : r.errors) {
                    Row row = es.createRow(er++);
                    row.createCell(0).setCellValue(rec.excelRow);
                    for (int c = 0; c < rec.values.size(); c++) {
                        row.createCell(c + 1).setCellValue(rec.values.get(c));
                    }
                    row.createCell(rec.values.size() + 1).setCellValue(rec.message);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(out)) {
                wb.write(fos);
            }
        }
        return out;
    }

    private static String statusText(SheetResult r) {
        if (!r.tableCreated) return "建表失败";
        if (r.fail == 0) return "正常";
        return "有异常(" + r.fail + "行)";
    }

    private static String safeSheetName(String name, Set<String> used) {
        String s = (name == null ? "" : name).replaceAll("[\\[\\]:*?/\\\\]", "_").trim();
        if (s.isEmpty()) s = "异常";
        if (s.length() > 31) s = s.substring(0, 31);
        String base = s;
        int k = 2;
        while (!used.add(s)) {
            s = base + "_" + (k++);
        }
        return s;
    }

    // ===================== 汇总输出 =====================
    private static void printSummary(String excelFilePath, List<SheetResult> results, File errFile) {
        log("");
        log("========================================");
        log("  导入完成");
        log("========================================");
        log("  文件:        " + excelFilePath);
        for (SheetResult r : results) {
            if (r.tableName == null) {
                log("  [" + r.sheetName + "] 未建表: " + r.createError);
            } else {
                log(String.format("  [%s] -> %s : 成功 %d 行, 失败 %d 行%s",
                        r.sheetName, r.tableName, r.success, r.fail,
                        r.createError != null ? "（建表失败）" : ""));
            }
        }
        if (errFile != null) {
            log("  异常记录:    " + errFile.getAbsolutePath() + "（请检查并修正后重导）");
        } else {
            log("  异常记录:    无，全部数据导入正常");
        }
        log("========================================");
    }

    // ===================== 表名 / 列名 =====================
    /** Sheet 序号后缀：优先用解析出的 Sheet 名，解析失败用序号 */
    static String sheetSuffix(Sheet sheet, int idx) {
        String token = parseToken(sheet.getSheetName());
        return token != null ? token : String.valueOf(idx + 1);
    }

    /** 表名 = <前缀>_<后缀>，保证唯一且不超长 */
    static String buildTableName(Dialect d, String baseAscii, String suffix, Set<String> usedSuffixes) {
        String candidate = suffix;
        int k = 2;
        while (!usedSuffixes.add(candidate.toLowerCase(Locale.ROOT))) {
            candidate = suffix + "_" + (k++);
        }
        int baseMax = d.maxTableNameLen() - candidate.length() - 1; // 留出 "_"
        if (baseMax < 1) baseMax = 1;
        String basePart = baseAscii.length() <= baseMax ? baseAscii : baseAscii.substring(0, baseMax);
        return d.toTableName(basePart + "_" + candidate);
    }

    /** 抽取为可用标识符的 ASCII token；无法解析返回 null */
    private static String parseToken(String raw) {
        if (raw == null) return null;
        StringBuilder sb = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        for (char ch : raw.toCharArray()) {
            if (isChinese(ch)) {
                try {
                    String[] arr = PinyinHelper.toHanyuPinyinStringArray(ch, format);
                    if (arr != null && arr.length > 0) sb.append(arr[0]);
                } catch (BadHanyuPinyinOutputFormatCombination ignored) {
                }
            } else if (Character.isLetterOrDigit(ch) || ch == '_') {
                sb.append(ch);
            } else {
                sb.append('_');
            }
        }
        String s = sb.toString().replaceAll("_+", "_").replaceAll("^_|_$", "");
        return s.isEmpty() ? null : s;
    }

    private static String normalizeName(String raw, boolean upper, String prefix, int maxLen) {
        String token = parseToken(raw);
        if (token == null) token = "X";
        String name = token;
        if (!Character.isLetter(name.charAt(0))) name = prefix + name;
        if (name.length() > maxLen) name = name.substring(0, maxLen);
        return upper ? name.toUpperCase(Locale.ROOT) : name.toLowerCase(Locale.ROOT);
    }

    private static boolean isChinese(char ch) {
        return Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN;
    }

    /** 列名规范化 + 去重 */
    private static List<String> normalizeHeaders(Dialect d, List<String> raw) {
        Set<String> used = new HashSet<>();
        List<String> result = new ArrayList<>();
        for (String name : raw) {
            String normalized = d.toColumnName(name);
            String unique = normalized;
            int suffix = 1;
            while (used.contains(unique.toUpperCase(Locale.ROOT))) {
                unique = normalized + "_" + (suffix++);
            }
            used.add(unique.toUpperCase(Locale.ROOT));
            result.add(unique);
        }
        return result;
    }

    // ===================== Excel 读取 =====================
    private static List<String> readHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            String val = (cell == null) ? "COL" + (c + 1) : cellToString(cell).trim();
            if (val.isEmpty()) val = "COL" + (c + 1);
            headers.add(val);
        }
        return headers;
    }

    private static String[] readRowValues(Row row, int colCount) {
        String[] vals = new String[colCount];
        for (int c = 0; c < colCount; c++) {
            vals[c] = cellToString(row.getCell(c));
        }
        return vals;
    }

    private static boolean isEmptyRow(Row row, int colCount) {
        for (int c = 0; c < colCount; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !cellToString(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 采样前 50 行探测每列最大长度 */
    static int[] probeMaxLength(Sheet sheet, int colCount) {
        int[] maxLen = new int[colCount];
        for (int i = 0; i < colCount; i++) maxLen[i] = 1;
        boolean[] ambiguous = new boolean[colCount];
        int limit = Math.min(sheet.getLastRowNum() + 1, 51);
        for (int rr = 1; rr < limit; rr++) {
            Row row = sheet.getRow(rr);
            if (row == null) continue;
            for (int c = 0; c < colCount; c++) {
                Cell cell = row.getCell(c);
                // 公式结果为错误的单元格内容不明 -> 该列直接建为 VARCHAR(4000)，保证兼容
                if (cell != null && cell.getCellType() == CellType.FORMULA
                        && cell.getCachedFormulaResultType() == CellType.ERROR) {
                    ambiguous[c] = true;
                    continue;
                }
                String val = cellToString(cell);
                if (val.length() > maxLen[c]) maxLen[c] = val.length();
            }
        }
        for (int i = 0; i < colCount; i++) {
            if (ambiguous[i]) { maxLen[i] = 4000; continue; }
            maxLen[i] = Math.min((int) (maxLen[i] * 2.5), 4000);
            if (maxLen[i] < 255) maxLen[i] = 255;
        }
        return maxLen;
    }

    // ===================== SQL 构建 =====================
    private static String buildCreateDdl(Dialect d, String tableName, List<String> colNames, int[] maxLen) {
        StringBuilder sb = new StringBuilder("CREATE TABLE \"").append(tableName).append("\" (\n");
        for (int i = 0; i < colNames.size(); i++) {
            sb.append("  \"").append(colNames.get(i)).append("\" ").append(d.columnType(maxLen[i]));
            if (i < colNames.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(")");
        return sb.toString();
    }

    private static String buildInsertSql(String tableName, List<String> colNames) {
        StringBuilder sb = new StringBuilder("INSERT INTO \"").append(tableName).append("\" (");
        for (int i = 0; i < colNames.size(); i++) {
            sb.append("\"").append(colNames.get(i)).append("\"");
            if (i < colNames.size() - 1) sb.append(", ");
        }
        sb.append(") VALUES (");
        for (int i = 0; i < colNames.size(); i++) {
            sb.append("?");
            if (i < colNames.size() - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    private static void setParams(PreparedStatement pstmt, String[] vals) throws SQLException {
        for (int i = 0; i < vals.length; i++) {
            pstmt.setString(i + 1, vals[i]);
        }
    }

    static String cellToString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString().replace('T', ' ');
                }
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // 公式单元格按缓存结果类型取值，避免对 ERROR 单元格调 getNumericCellValue() 抛异常
                try {
                    switch (cell.getCachedFormulaResultType()) {
                        case STRING:  return cell.getStringCellValue();
                        case NUMERIC:
                            double fv = cell.getNumericCellValue();
                            if (fv == Math.floor(fv) && !Double.isInfinite(fv)) return String.valueOf((long) fv);
                            return String.valueOf(fv);
                        case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
                        case ERROR:   return FormulaError.forInt(cell.getErrorCellValue()).getString();
                        default:      return "";
                    }
                } catch (Exception e) { return ""; }
            default: return "";
        }
    }

    // ===================== 日志 =====================
    private static String now() {
        return LocalDateTime.now().format(TS);
    }

    private static void log(String msg) {
        System.out.println("[" + now() + "] " + msg);
    }
}
