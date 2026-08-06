package com.sunzh.sync;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Excel -> GaussDB 分布式数据库导入工具。
 * 用法: java -cp ".:lib/*" ExcelToGaussDB <jdbc-url> <user> <password> <excel-file>
 *
 * JDBC URL 示例:
 *   jdbc:gaussdb://host:8000/database
 */
public class ExcelToGaussDB {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            log("用法: java -cp \".:lib/*\" ExcelToGaussDB <jdbc-url> <user> <password> <excel-file>");
            log("示例: java -cp \".:lib/*\" ExcelToGaussDB jdbc:gaussdb://127.0.0.1:8000/mydb root pass123 data.xlsx");
            System.exit(1);
        }

        String dbUrl = args[0];
        String dbUser = args[1];
        String dbPassword = args[2];
        String excelFilePath = args[3];

        log("========================================");
        log("  Excel -> GaussDB 导入工具");
        log("========================================");
        log("数据库: " + dbUrl);
        log("用户:   " + dbUser);
        log("文件:   " + excelFilePath);

        File excelFile = new File(excelFilePath);
        if (!excelFile.exists()) {
            log("ERROR: 文件不存在 - " + excelFilePath);
            System.exit(1);
        }

        String rawName = excelFile.getName();
        String baseName = rawName.substring(0, rawName.lastIndexOf('.'));
        String tableName = toGaussTableName(baseName);

        log("源文件: " + rawName);
        log("目标表: " + tableName);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getLastRowNum(); // 不含表头

            List<String> rawHeaders = readHeaders(sheet);
            List<String> colNames = normalizeHeaders(rawHeaders);

            log("");
            log("--- 列映射 ---");
            for (int i = 0; i < rawHeaders.size(); i++) {
                log(String.format("  %-40s -> %s", rawHeaders.get(i), colNames.get(i)));
            }
            log("共 " + colNames.size() + " 列");

            // 探测列长
            log("");
            log("--- 采样探测列长度 (前50行) ---");
            int[] maxLen = new int[colNames.size()];
            probeMaxLength(sheet, maxLen);
            for (int i = 0; i < colNames.size(); i++) {
                log(String.format("  %-30s VARCHAR(%d)", colNames.get(i), maxLen[i]));
            }

            // 建表
            String createDdl = buildCreateDdl(tableName, colNames, maxLen);
            log("");
            log("--- 建表 DDL ---");
            log(createDdl);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS \"" + tableName + "\"");
                stmt.execute(createDdl);
                log("");
                log(">>> 表 " + tableName + " 创建成功 <<<");
            }

            // 导入数据
            String insertSql = buildInsertSql(tableName, colNames);
            log("");
            log("--- 开始导入数据 (共 " + totalRows + " 行) ---");

            int count = 0;
            int skipped = 0;

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) { skipped++; continue; }
                    if (isEmptyRow(row, colNames.size())) { skipped++; continue; }

                    for (int c = 0; c < colNames.size(); c++) {
                        Cell cell = row.getCell(c);
                        pstmt.setString(c + 1, cellToString(cell));
                    }
                    pstmt.addBatch();
                    count++;

                    if (count % 500 == 0) {
                        pstmt.executeBatch();
                        double pct = totalRows > 0 ? (100.0 * r / totalRows) : 0;
                        log(String.format("  [%s] 已处理 %d/%d 行 (%.1f%%), 已插入 %d 行, 跳过 %d 空行",
                                now(), r, totalRows, pct, count, skipped));
                    }
                }
                pstmt.executeBatch();
            }

            log("");
            log("========================================");
            log("  导入完成");
            log("========================================");
            log("  文件:        " + excelFilePath);
            log("  读取总行数:  " + totalRows);
            log("  实际插入:    " + count + " 行");
            log("  跳过空行:    " + skipped + " 行");
            log("  目标表:      " + tableName);
            log("  列数:        " + colNames.size());
            log("  库表名称:    " + tableName);
            log("========================================");
        }
    }

    private static String now() {
        return LocalDateTime.now().format(TS);
    }

    private static void log(String msg) {
        System.out.println("[" + now() + "] " + msg);
    }

    /** 读取第一行作为列名 */
    private static List<String> readHeaders(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            log("ERROR: Excel 第一行为空，无法读取列名");
            System.exit(1);
        }
        List<String> headers = new ArrayList<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            String val = (cell == null) ? "COL" + (c + 1) : cellToString(cell).trim();
            if (val.isEmpty()) val = "COL" + (c + 1);
            headers.add(val);
        }
        return headers;
    }

    /** 规范化列名: 中文转拼音/英文, 去特殊字符, 限制30字符 */
    private static List<String> normalizeHeaders(List<String> raw) {
        Set<String> used = new HashSet<>();
        List<String> result = new ArrayList<>();
        for (String name : raw) {
            String normalized = normalizeColumnName(name);
            String unique = normalized;
            int suffix = 1;
            while (used.contains(unique.toUpperCase())) {
                unique = normalized + "_" + suffix;
                suffix++;
            }
            used.add(unique.toUpperCase());
            result.add(unique);
        }
        return result;
    }

    private static String normalizeColumnName(String name) {
        StringBuilder sb = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

        for (char ch : name.toCharArray()) {
            if (isChinese(ch)) {
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        sb.append(pinyinArray[0]);
                    }
                } catch (BadHanyuPinyinOutputFormatCombination ignored) {
                }
            } else if (Character.isLetterOrDigit(ch) || ch == '_') {
                sb.append(ch);
            } else {
                sb.append('_');
            }
        }

        String result = sb.toString().replaceAll("_+", "_").replaceAll("^_|_$", "");
        if (result.isEmpty() || !Character.isLetter(result.charAt(0))) {
            result = "C_" + result;
        }
        if (result.length() > 30) {
            result = result.substring(0, 30);
        }
        return result.toLowerCase();
    }

    private static boolean isChinese(char ch) {
        return Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN;
    }

    /** GaussDB 表名: 字母开头, 去掉特殊字符, 限制30字符 */
    private static String toGaussTableName(String baseName) {
        StringBuilder sb = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

        for (char ch : baseName.toCharArray()) {
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

        String name = sb.toString().replaceAll("_+", "_").replaceAll("^_|_$", "");
        if (name.isEmpty()) name = "T_EXCEL";
        if (!Character.isLetter(name.charAt(0))) name = "T" + name;
        if (name.length() > 30) name = name.substring(0, 30);
        return name.toLowerCase();
    }

    /** 采样前 50 行探测每列最大长度 */
    private static void probeMaxLength(Sheet sheet, int[] maxLen) {
        for (int i = 0; i < maxLen.length; i++) maxLen[i] = 1;
        int limit = Math.min(sheet.getLastRowNum() + 1, 51);
        for (int r = 1; r < limit; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < maxLen.length; c++) {
                Cell cell = row.getCell(c);
                String val = cellToString(cell);
                if (val.length() > maxLen[c]) maxLen[c] = val.length();
            }
        }
        for (int i = 0; i < maxLen.length; i++) {
            maxLen[i] = Math.min((int) (maxLen[i] * 2.5), 4000);
            if (maxLen[i] < 255) maxLen[i] = 255;
        }
    }

    private static String buildCreateDdl(String tableName, List<String> colNames, int[] maxLen) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE \"").append(tableName).append("\" (\n");
        for (int i = 0; i < colNames.size(); i++) {
            sb.append("  \"").append(colNames.get(i)).append("\" VARCHAR(").append(maxLen[i]).append(")");
            if (i < colNames.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(")");
        return sb.toString();
    }

    private static String buildInsertSql(String tableName, List<String> colNames) {
        StringBuilder sb = new StringBuilder("INSERT INTO \"");
        sb.append(tableName).append("\" (");
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

    private static String cellToString(Cell cell) {
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
                try { return cell.getStringCellValue(); }
                catch (Exception e) { return String.valueOf(cell.getNumericCellValue()); }
            default: return "";
        }
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
}
