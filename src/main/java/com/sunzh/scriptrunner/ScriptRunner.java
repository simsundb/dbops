package com.sunzh.scriptrunner;

import com.sunzh.utils.EncodingUtils;

import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * SQL文件迁移核心逻辑
 * 功能：
 * 1. 读取SQL文件内容
 * 2. 将文件内容插入 general_app_form
 * 3. 解析SQL语句，插入 general_app_form_parsed
 * 4. 顺序执行未执行的DDL，并更新执行状态
 * 5. 支持断点续传（已执行过的SQL跳过）
 * 
 * 依赖：JDBC驱动（Oracle/GaussDB）
 */
public class ScriptRunner {
    
    /**
     * 日志回调接口，用于向GUI输出日志
     */
    public interface LogCallback {
        void log(String msg);
    }
    
    private LogCallback logCallback;
    
    public ScriptRunner() {
    }
    
    public ScriptRunner(LogCallback callback) {
        this.logCallback = callback;
    }
    
    public void setLogCallback(LogCallback callback) {
        this.logCallback = callback;
    }
    
    /**
     * 处理单个SQL文件（完整流程：入库→解析→执行→更新状态）
     * 
     * @param conn 目标数据库连接（已开启事务）
     * @param file SQL文件
     * @param fileName 文件名（用于数据库记录）
     * @throws Exception 处理异常
     */
    public void processFile(Connection conn, File file, String fileName) throws Exception {
        if (conn == null) {
            throw new IllegalArgumentException("数据库连接不能为空");
        }
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("文件不存在: " + file);
        }
        
        // 读取文件内容
        String fileContent = readFileContent(file);
        log("读取文件: " + fileName + "，大小: " + fileContent.length() + " 字符");
        
        // 1. 检查文件是否已存在
        if (fileExists(conn, fileName)) {
            log("文件已存在，检查是否有未执行的SQL...");
            if (hasUnexecutedSQL(conn, fileName)) {
                log("发现未执行的SQL，开始断点续传...");
                executePendingSQL(conn, fileName);
                log("断点续传完成");
            } else {
                log("所有SQL已执行完毕，跳过");
            }
            return;
        }
        
        // 2. 插入文件内容
        log("插入文件内容到 general_app_form...");
        insertFileRecord(conn, fileName, fileContent);
        
        // 3. 解析SQL
        log("解析SQL语句...");
        List<String> sqlList = splitSQLFixed(fileContent);
        log("解析完成，共 " + sqlList.size() + " 条SQL");
        if (sqlList.isEmpty()) {
            log("警告: 未解析到有效SQL，跳过");
            return;
        }
        
        // 4. 插入解析记录
        log("插入解析记录到 general_app_form_parsed...");
        insertParsedRecords(conn, fileName, sqlList);
        
        // 5. 标记文件已解析
        updateFileParseStatus(conn, fileName);
        log("文件已解析");
        
        // 6. 执行DDL
        log("开始执行DDL...");
        executePendingSQL(conn, fileName);
        log("执行完成");
    }
    
    // ================== 私有方法 ==================
    
    private void log(String msg) {
        if (logCallback != null) {
            logCallback.log(msg);
        } else {
            System.out.println(msg);
        }
    }
    
    /**
     * 读取文件内容（自动识别编码 UTF-8/GBK，避免乱码）
     */
    private String readFileContent(File file) throws IOException {
        return EncodingUtils.readText(file);
    }
    
    /**
     * 检查文件是否已在 general_app_form 中存在
     */
    private boolean fileExists(Connection conn, String fileName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM general_app_form WHERE file_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fileName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    /**
     * 检查是否有未执行的SQL
     */
    private boolean hasUnexecutedSQL(Connection conn, String fileName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM general_app_form_parsed " +
                     "WHERE file_name = ? AND exec_flag IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fileName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    /**
     * 插入文件记录
     */
    private void insertFileRecord(Connection conn, String fileName, String content) throws SQLException {
        String sql = "INSERT INTO general_app_form (file_name, file_content, read_time, parsed_flag) " +
                     "VALUES (?, ?, SYSTIMESTAMP, '0')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fileName);
            Clob clob = conn.createClob();
            clob.setString(1, content);
            ps.setClob(2, clob);
            ps.executeUpdate();
        }
    }
    
    /**
     * 插入解析后的SQL记录
     */
    private void insertParsedRecords(Connection conn, String fileName, List<String> sqlList) throws SQLException {
        String sql = "INSERT INTO general_app_form_parsed (file_name, seq_id, ddl_sql) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < sqlList.size(); i++) {
                ps.setString(1, fileName);
                ps.setInt(2, i + 1);
                Clob clob = conn.createClob();
                clob.setString(1, sqlList.get(i));
                ps.setClob(3, clob);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    
    /**
     * 更新文件的解析状态
     */
    private void updateFileParseStatus(Connection conn, String fileName) throws SQLException {
        String sql = "UPDATE general_app_form SET parsed_flag = '1', parse_time = SYSTIMESTAMP " +
                     "WHERE file_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fileName);
            ps.executeUpdate();
        }
    }
    
    /**
     * 执行所有未执行的SQL（断点续传）
     */
    private void executePendingSQL(Connection conn, String fileName) throws SQLException {
        String querySql = "SELECT seq_id, ddl_sql FROM general_app_form_parsed " +
                          "WHERE file_name = ? AND exec_flag IS NULL ORDER BY seq_id";
        try (PreparedStatement ps = conn.prepareStatement(querySql)) {
            ps.setString(1, fileName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int seqId = rs.getInt("seq_id");
                    Clob clob = rs.getClob("ddl_sql");
                    String ddlSQL = clobToString(clob);
                    
                    log("执行 SEQ_ID=" + seqId);
                    updateExecStatus(conn, fileName, seqId, "RUNNING", "开始执行");
                    
                    try {
                        executeDDL(conn, ddlSQL);
                        updateExecStatus(conn, fileName, seqId, "SUCCESS", "执行成功");
                        log("执行 SEQ_ID=" + seqId + " - 成功");
                    } catch (Exception e) {
                        String errMsg = getErrorMessage(e);
                        updateExecStatus(conn, fileName, seqId, "FAILED", errMsg);
                        log("执行 SEQ_ID=" + seqId + " - 失败: " + errMsg);
                    }
                }
            }
        }
    }
    
    /**
     * 执行单条DDL
     */
    private void executeDDL(Connection conn, String ddlSQL) throws SQLException {
        String cleanSQL = ddlSQL.trim().replaceAll("(?m)^/\\s*$", "").trim();
        if (cleanSQL.isEmpty()) return;
        try (Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(600);
            stmt.execute(cleanSQL);
        }
    }
    
    /**
     * 更新执行状态
     */
    private void updateExecStatus(Connection conn, String fileName, int seqId, 
                                  String flag, String msg) throws SQLException {
        String sql = "UPDATE general_app_form_parsed SET exec_flag = ?, exec_time = SYSTIMESTAMP, exec_msg = ? " +
                     "WHERE file_name = ? AND seq_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, flag);
            if (msg != null && msg.length() > 3900) {
                msg = msg.substring(0, 3900) + "... (截断)";
            }
            ps.setString(2, msg);
            ps.setString(3, fileName);
            ps.setInt(4, seqId);
            ps.executeUpdate();
        }
    }
    
    /**
     * CLOB转字符串（异常已包装为SQLException）
     */
    private String clobToString(Clob clob) throws SQLException {
        if (clob == null) return "";
        StringBuilder sb = new StringBuilder();
        try (Reader reader = clob.getCharacterStream();
             BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new SQLException("读取CLOB失败: " + e.getMessage(), e);
        }
        return sb.toString();
    }
    
    private String getErrorMessage(Exception e) {
        String msg = e.getMessage();
        return msg != null ? msg : e.toString();
    }
    
    // ================== SQL解析工具 ==================
    
    private static class StringRegion {
        int start, end;
        StringRegion(int start, int end) {
            this.start = start;
            this.end = end;
        }
        boolean contains(int pos) {
            return pos >= start && pos <= end;
        }
    }
    
    private List<StringRegion> findStringRegions(String content) {
        List<StringRegion> regions = new ArrayList<>();
        boolean inString = false;
        int stringStart = -1;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\'') {
                if (i + 1 < content.length() && content.charAt(i + 1) == '\'') {
                    i++;
                    continue;
                }
                if (!inString) {
                    stringStart = i;
                    inString = true;
                } else {
                    regions.add(new StringRegion(stringStart, i));
                    inString = false;
                }
            }
        }
        return regions;
    }
    
    private String removeCommentsProtected(String content, List<StringRegion> stringRegions) {
        StringBuilder result = new StringBuilder();
        boolean inBlockComment = false;
        boolean inLineComment = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            boolean inString = false;
            for (StringRegion region : stringRegions) {
                if (region.contains(i)) {
                    inString = true;
                    break;
                }
            }
            if (inString) {
                result.append(c);
                continue;
            }
            if (!inLineComment && !inBlockComment && 
                c == '/' && i + 1 < content.length() && content.charAt(i + 1) == '*') {
                inBlockComment = true;
                i++;
                continue;
            }
            if (inBlockComment && 
                c == '*' && i + 1 < content.length() && content.charAt(i + 1) == '/') {
                inBlockComment = false;
                i++;
                continue;
            }
            if (!inBlockComment && !inLineComment && 
                c == '-' && i + 1 < content.length() && content.charAt(i + 1) == '-') {
                inLineComment = true;
                continue;
            }
            if (inLineComment && (c == '\n' || c == '\r')) {
                inLineComment = false;
                result.append(c);
                continue;
            }
            if (!inBlockComment && !inLineComment) {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    private List<String> splitSQLFixed(String content) {
        List<String> result = new ArrayList<>();
        List<StringRegion> stringRegions = findStringRegions(content);
        content = removeCommentsProtected(content, stringRegions);
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\'') {
                if (i + 1 < content.length() && content.charAt(i + 1) == '\'') {
                    current.append("''");
                    i++;
                } else {
                    inString = !inString;
                    current.append(c);
                }
            } else if (c == ';' && !inString) {
                String sql = current.toString().trim();
                if (!sql.isEmpty()) {
                    result.add(sql);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        String lastSQL = current.toString().trim();
        if (!lastSQL.isEmpty()) {
            result.add(lastSQL);
        }
        return result;
    }
}