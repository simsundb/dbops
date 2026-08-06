package com.example.datacheck;

import com.example.core.DataSource;
import com.example.core.DataSourceStore;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataCheckDetailDao {

    private DataSource getDefaultDataSource() throws SQLException {
        List<DataSource> list = DataSourceStore.load();
        if (list == null || list.isEmpty()) {
            throw new SQLException("未配置数据源，请先在【数据源配置】中设置");
        }
        return list.get(0);
    }

    private Connection getConnection(DataSource ds) throws SQLException {
        if (ds == null) {
            ds = getDefaultDataSource();
        }
        String url = ds.buildUrl();
        if (url == null || url.isEmpty()) {
            throw new SQLException("数据源配置不完整，无法构建 JDBC URL");
        }
        return DriverManager.getConnection(url, ds.getUser(), ds.getPassword());
    }

    // 无参方法（使用默认数据源）
    public List<DataCheckDetail> findByBatchId(String batchId) throws SQLException {
        return findByBatchId(batchId, null);
    }

    public List<String> findAllBatchIds() throws SQLException {
        return findAllBatchIds(null);
    }

    public String getLatestBatchId() throws SQLException {
        return getLatestBatchId(null);
    }

    public int update(DataCheckDetail detail) throws SQLException {
        return update(detail, null);
    }

    public int delete(Long logId) throws SQLException {
        return delete(logId, null);
    }

    public int insert(DataCheckDetail detail) throws SQLException {
        return insert(detail, null);
    }

    // 带数据源参数的方法
    public List<DataCheckDetail> findByBatchId(String batchId, DataSource ds) throws SQLException {
        List<DataCheckDetail> list = new ArrayList<>();
        String sql = "SELECT log_id, batch_id, table_owner, table_name, column_name, " +
                "rule_id, rule_type, rule_name, apply_data_type, priority, " +
                "NVL(check_sql_clob, check_sql) AS full_check_sql, " +
                "NVL(clean_sql_clob, clean_sql) AS full_clean_sql, " +
                "check_sql_len, clean_sql_len, " +
                "check_status, check_start_time, check_end_time, check_error_msg, check_row_count, " +
                "clean_status, clean_start_time, clean_end_time, clean_error_msg, clean_row_count, " +
                "exec_flag, create_time " +
                "FROM GK_DATA_CHECK_CLEAN_DETAIL WHERE batch_id = ? ORDER BY priority, log_id";
        try (Connection conn = getConnection(ds);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DataCheckDetail d = new DataCheckDetail();
                    d.setLogId(rs.getLong("log_id"));
                    d.setBatchId(rs.getString("batch_id"));
                    d.setTableOwner(rs.getString("table_owner"));
                    d.setTableName(rs.getString("table_name"));
                    d.setColumnName(rs.getString("column_name"));
                    d.setRuleId(rs.getLong("rule_id"));
                    d.setRuleType(rs.getString("rule_type"));
                    d.setRuleName(rs.getString("rule_name"));
                    d.setApplyDataType(rs.getString("apply_data_type"));
                    d.setPriority(rs.getInt("priority"));
                    d.setCheckSql(rs.getString("full_check_sql"));
                    d.setCleanSql(rs.getString("full_clean_sql"));
                    d.setCheckSqlLen(rs.getInt("check_sql_len"));
                    d.setCleanSqlLen(rs.getInt("clean_sql_len"));
                    d.setCheckStatus(rs.getString("check_status"));
                    d.setCheckStartTime(rs.getTimestamp("check_start_time"));
                    d.setCheckEndTime(rs.getTimestamp("check_end_time"));
                    d.setCheckErrorMsg(rs.getString("check_error_msg"));
                    d.setCheckRowCount(rs.getLong("check_row_count"));
                    d.setCleanStatus(rs.getString("clean_status"));
                    d.setCleanStartTime(rs.getTimestamp("clean_start_time"));
                    d.setCleanEndTime(rs.getTimestamp("clean_end_time"));
                    d.setCleanErrorMsg(rs.getString("clean_error_msg"));
                    d.setCleanRowCount(rs.getLong("clean_row_count"));
                    d.setExecFlag(rs.getString("exec_flag"));
                    d.setCreateTime(rs.getTimestamp("create_time"));
                    list.add(d);
                }
            }
        }
        return list;
    }

    public List<String> findAllBatchIds(DataSource ds) throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT batch_id FROM GK_DATA_CHECK_CLEAN_DETAIL ORDER BY batch_id DESC";
        try (Connection conn = getConnection(ds);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getString("batch_id"));
            }
        }
        return list;
    }

    public String getLatestBatchId(DataSource ds) throws SQLException {
        String sql = "SELECT MAX(batch_id) FROM GK_DATA_CHECK_CLEAN_DETAIL";
        try (Connection conn = getConnection(ds);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getString(1);
        }
        return null;
    }

    public int update(DataCheckDetail detail, DataSource ds) throws SQLException {
        String sql = "UPDATE GK_DATA_CHECK_CLEAN_DETAIL SET exec_flag = ? WHERE log_id = ?";
        try (Connection conn = getConnection(ds);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, detail.getExecFlag());
            ps.setLong(2, detail.getLogId());
            return ps.executeUpdate();
        }
    }

    public int delete(Long logId, DataSource ds) throws SQLException {
        String sql = "DELETE FROM GK_DATA_CHECK_CLEAN_DETAIL WHERE log_id = ?";
        try (Connection conn = getConnection(ds);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, logId);
            return ps.executeUpdate();
        }
    }

    public int insert(DataCheckDetail detail, DataSource ds) throws SQLException {
        String sql = "INSERT INTO GK_DATA_CHECK_CLEAN_DETAIL " +
                "(log_id, batch_id, table_owner, table_name, column_name, rule_id, " +
                "rule_type, rule_name, apply_data_type, priority, check_sql, clean_sql, " +
                "check_sql_len, clean_sql_len, check_status, clean_status, exec_flag, create_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE)";
        try (Connection conn = getConnection(ds);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, detail.getLogId());
            ps.setString(2, detail.getBatchId());
            ps.setString(3, detail.getTableOwner());
            ps.setString(4, detail.getTableName());
            ps.setString(5, detail.getColumnName());
            ps.setLong(6, detail.getRuleId());
            ps.setString(7, detail.getRuleType());
            ps.setString(8, detail.getRuleName());
            ps.setString(9, detail.getApplyDataType());
            ps.setInt(10, detail.getPriority() != null ? detail.getPriority() : 99);
            ps.setString(11, detail.getCheckSql());
            ps.setString(12, detail.getCleanSql());
            ps.setInt(13, detail.getCheckSqlLen() != null ? detail.getCheckSqlLen() : 0);
            ps.setInt(14, detail.getCleanSqlLen() != null ? detail.getCleanSqlLen() : 0);
            ps.setString(15, detail.getCheckStatus() != null ? detail.getCheckStatus() : "W");
            ps.setString(16, detail.getCleanStatus() != null ? detail.getCleanStatus() : "N");
            ps.setString(17, detail.getExecFlag() != null ? detail.getExecFlag() : "Y");
            return ps.executeUpdate();
        }
    }
}