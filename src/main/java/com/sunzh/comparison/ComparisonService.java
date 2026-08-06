package com.sunzh.comparison;

import com.sunzh.comparison.model.ComparisonTask;
import com.sunzh.comparison.model.ComparisonTaskConfig;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ComparisonService {
    private final Connection conn;

    public ComparisonService(Connection conn) {
        this.conn = conn;
    }

    // ----- 抽取数据 -----
    public void extractData(String schema, String type, boolean isSource) throws SQLException {
        String procName = isSource ? "sp_extract_source_data" : "sp_extract_target_data";
        try (CallableStatement cstmt = conn.prepareCall("{call " + procName + "(?,?)}")) {
            cstmt.setString(1, schema);
            cstmt.setString(2, type);
            cstmt.execute();
        }
    }

    // ----- 执行对比 -----
    public void generateCompare(String jobId) throws SQLException {
        try (CallableStatement cstmt = conn.prepareCall("{call sp_generate_all_compare(?)}")) {
            cstmt.setString(1, jobId);
            cstmt.execute();
        }
    }

    // ----- 查询配置表 -----
    public List<ComparisonTaskConfig> loadTaskConfigs() throws SQLException {
        List<ComparisonTaskConfig> list = new ArrayList<>();
        String sql = "SELECT compare_schemas_source, compare_schemas_target, enable_flag FROM gk_sjdb_task_config";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ComparisonTaskConfig tc = new ComparisonTaskConfig();
                tc.setSourceSchema(rs.getString(1));
                tc.setTargetSchema(rs.getString(2));
                tc.setEnableFlag(rs.getString(3));
                list.add(tc);
            }
        }
        return list;
    }

    // ----- 查询任务表 -----
    public List<ComparisonTask> loadTasks() throws SQLException {
        List<ComparisonTask> list = new ArrayList<>();
        String sql = "SELECT job_id, job_name, job_desc, compare_schemas_source, compare_schemas_target, " +
                "compare_types, enable_flag, exec_status, start_time, end_time, duration_seconds, error_msg, " +
                "table_status, column_status, index_status, sequence_status, synonym_status, " +
                "table_error_msg, column_error_msg, index_error_msg, sequence_error_msg, synonym_error_msg " +
                "FROM gk_sjdb_task";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            while (rs.next()) {
                ComparisonTask task = new ComparisonTask();
                task.setJobId(rs.getString(1));
                task.setJobName(rs.getString(2));
                task.setJobDesc(rs.getString(3));
                task.setSourceSchema(rs.getString(4));
                task.setTargetSchema(rs.getString(5));
                task.setCompareTypes(rs.getString(6));
                task.setEnableFlag(rs.getString(7));
                task.setExecStatus(rs.getString(8));
                Timestamp start = rs.getTimestamp(9);
                Timestamp end = rs.getTimestamp(10);
                task.setStartTime(start != null ? sdf.format(start) : "");
                task.setEndTime(end != null ? sdf.format(end) : "");
                task.setDurationSeconds(rs.getInt(11));
                if (rs.wasNull()) task.setDurationSeconds(null);
                task.setErrorMsg(rs.getString(12));
                task.setTableStatus(rs.getString(13));
                task.setColumnStatus(rs.getString(14));
                task.setIndexStatus(rs.getString(15));
                task.setSequenceStatus(rs.getString(16));
                task.setSynonymStatus(rs.getString(17));
                task.setTableErrorMsg(rs.getString(18));
                task.setColumnErrorMsg(rs.getString(19));
                task.setIndexErrorMsg(rs.getString(20));
                task.setSequenceErrorMsg(rs.getString(21));
                task.setSynonymErrorMsg(rs.getString(22));
                list.add(task);
            }
        }
        return list;
    }

    // ----- 保存配置表（全量替换） -----
    public void saveTaskConfigs(List<ComparisonTaskConfig> configs) throws SQLException {
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM gk_sjdb_task_config");
            String insertSql = "INSERT INTO gk_sjdb_task_config (compare_schemas_source, compare_schemas_target, enable_flag) VALUES (?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (ComparisonTaskConfig tc : configs) {
                    ps.setString(1, tc.getSourceSchema());
                    ps.setString(2, tc.getTargetSchema());
                    ps.setString(3, tc.getEnableFlag());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ----- 更新单个任务 -----
    public void updateTask(ComparisonTask task) throws SQLException {
        String sql = "UPDATE gk_sjdb_task SET " +
                "job_name=?, job_desc=?, compare_schemas_source=?, compare_schemas_target=?, " +
                "compare_types=?, enable_flag=?, exec_status=?, start_time=?, end_time=?, " +
                "duration_seconds=?, error_msg=?, table_status=?, column_status=?, " +
                "index_status=?, sequence_status=?, synonym_status=?, " +
                "table_error_msg=?, column_error_msg=?, index_error_msg=?, sequence_error_msg=?, synonym_error_msg=? " +
                "WHERE job_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getJobName());
            ps.setString(2, task.getJobDesc());
            ps.setString(3, task.getSourceSchema());
            ps.setString(4, task.getTargetSchema());
            ps.setString(5, task.getCompareTypes());
            ps.setString(6, task.getEnableFlag());
            ps.setString(7, task.getExecStatus());
            ps.setString(8, task.getStartTime());
            ps.setString(9, task.getEndTime());
            ps.setObject(10, task.getDurationSeconds());
            ps.setString(11, task.getErrorMsg());
            ps.setString(12, task.getTableStatus());
            ps.setString(13, task.getColumnStatus());
            ps.setString(14, task.getIndexStatus());
            ps.setString(15, task.getSequenceStatus());
            ps.setString(16, task.getSynonymStatus());
            ps.setString(17, task.getTableErrorMsg());
            ps.setString(18, task.getColumnErrorMsg());
            ps.setString(19, task.getIndexErrorMsg());
            ps.setString(20, task.getSequenceErrorMsg());
            ps.setString(21, task.getSynonymErrorMsg());
            ps.setString(22, task.getJobId());
            ps.executeUpdate();
        }
    }

    // ----- 删除任务 -----
    public void deleteTask(String jobId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM gk_sjdb_task WHERE job_id=?")) {
            ps.setString(1, jobId);
            ps.executeUpdate();
        }
    }

    // ----- 新增任务 -----
    public void insertTask(ComparisonTask task) throws SQLException {
        String sql = "INSERT INTO gk_sjdb_task (" +
                "job_id, job_name, job_desc, compare_schemas_source, compare_schemas_target, " +
                "compare_types, enable_flag, exec_status, start_time) " +
                "VALUES (?,?,?,?,?,?,?,?, SYSTIMESTAMP)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getJobId());
            ps.setString(2, task.getJobName());
            ps.setString(3, task.getJobDesc());
            ps.setString(4, task.getSourceSchema());
            ps.setString(5, task.getTargetSchema());
            ps.setString(6, task.getCompareTypes());
            ps.setString(7, task.getEnableFlag());
            ps.setString(8, "IDLE");
            ps.executeUpdate();
        }
    }

    // ----- 重置任务状态（清空状态字段） -----
    public void resetTaskStatuses(List<String> jobIds) throws SQLException {
        String sql = "UPDATE gk_sjdb_task SET " +
                "exec_status=NULL, table_status=NULL, column_status=NULL, " +
                "index_status=NULL, sequence_status=NULL, synonym_status=NULL, " +
                "end_time=NULL, duration_seconds=NULL " +
                "WHERE job_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String jobId : jobIds) {
                ps.setString(1, jobId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ----- 获取对比结果汇总（用于 ComparePanel） -----
    public List<Object[]> getSummaryForJob(String jobId) throws SQLException {
        List<Object[]> result = new ArrayList<>();
        String sql = "SELECT '表' AS OBJECT_TYPE, COUNT(*) AS TOTAL, " +
                "SUM(CASE WHEN DIFF_STATUS='ONLY_SOURCE' THEN 1 ELSE 0 END) AS ONLY_SOURCE, " +
                "SUM(CASE WHEN DIFF_STATUS='ONLY_TARGET' THEN 1 ELSE 0 END) AS ONLY_TARGET, " +
                "SUM(CASE WHEN DIFF_STATUS='MISMATCH' THEN 1 ELSE 0 END) AS MISMATCH " +
                "FROM gk_sjdb_table_jg WHERE JOB_ID=? " +
                "UNION ALL SELECT '列', COUNT(*), " +
                "SUM(CASE WHEN DIFF_STATUS='ONLY_SOURCE' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN DIFF_STATUS='ONLY_TARGET' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN DIFF_STATUS='MISMATCH' THEN 1 ELSE 0 END) " +
                "FROM gk_sjdb_column_jg WHERE JOB_ID=? " +
                "UNION ALL SELECT '索引', COUNT(*), " +
                "SUM(CASE WHEN DIFF_STATUS='ONLY_SOURCE' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN DIFF_STATUS='ONLY_TARGET' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN DIFF_STATUS='MISMATCH' THEN 1 ELSE 0 END) " +
                "FROM gk_sjdb_index_jg WHERE JOB_ID=? " +
                "UNION ALL SELECT '序列', COUNT(*), " +
                "SUM(CASE WHEN DIFF_STATUS='ONLY_SOURCE' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN DIFF_STATUS='ONLY_TARGET' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN DIFF_STATUS='MISMATCH' THEN 1 ELSE 0 END) " +
                "FROM gk_sjdb_sequence_jg WHERE JOB_ID=? " +
                "UNION ALL SELECT '同义词', COUNT(*), " +
                "SUM(CASE WHEN DIFF_STATUS='ONLY_SOURCE' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN DIFF_STATUS='ONLY_TARGET' THEN 1 ELSE 0 END), 0 AS MISMATCH " +
                "FROM gk_sjdb_synonym_jg WHERE JOB_ID=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) ps.setString(i, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Object[]{
                            rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5)
                    });
                }
            }
        }
        return result;
    }

    // ----- 查询明细（用于 DetailPanel） -----
    public ResultSet queryDetail(String type, String jobId) throws SQLException {
        String tableName = getTableName(type);
        if (tableName == null) throw new SQLException("未知类型: " + type);
        String sql = "SELECT * FROM " + tableName;
        if (jobId != null && !jobId.trim().isEmpty()) {
            sql += " WHERE job_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, jobId);
            return ps.executeQuery();
        } else {
            Statement st = conn.createStatement();
            return st.executeQuery(sql);
        }
    }

    private String getTableName(String type) {
        switch (type) {
            case "表": return "gk_sjdb_table_jg";
            case "列": return "gk_sjdb_column_jg";
            case "索引": return "gk_sjdb_index_jg";
            case "序列": return "gk_sjdb_sequence_jg";
            case "同义词": return "gk_sjdb_synonym_jg";
            default: return null;
        }
    }
}