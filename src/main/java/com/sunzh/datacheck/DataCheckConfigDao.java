package com.sunzh.datacheck;

import com.sunzh.core.DataSource;
import com.sunzh.core.DataSourceStore;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataCheckConfigDao {

    // ---------- 获取默认数据源（第一个） ----------
    private DataSource getDefaultDataSource() throws SQLException {
        List<DataSource> list = DataSourceStore.load();
        if (list == null || list.isEmpty()) {
            throw new SQLException("未配置数据源，请先在【数据源配置】中设置");
        }
        return list.get(0);
    }

    // ---------- 根据指定数据源获取连接 ----------
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

    // ---------- 原有无参方法（使用默认数据源） ----------
    public List<DataCheckConfig> findAll() throws SQLException {
        return findAll(null);
    }

    public DataCheckConfig findById(Long id) throws SQLException {
        return findById(id, null);
    }

    public int insert(DataCheckConfig config) throws SQLException {
        return insert(config, null);
    }

    public int update(DataCheckConfig config) throws SQLException {
        return update(config, null);
    }

    public int delete(Long id) throws SQLException {
        return delete(id, null);
    }

    // ---------- 新增带数据源参数的方法 ----------
    public List<DataCheckConfig> findAll(DataSource ds) throws SQLException {
        List<DataCheckConfig> list = new ArrayList<>();
        String sql = "SELECT rule_id, db_type, rule_type, rule_name, exec_flag, apply_data_type, " +
                "check_condition, clean_expression, priority, rule_desc, create_time, update_time " +
                "FROM GK_DATA_CHECK_CLEAN_CONFIG ORDER BY priority";
        try (Connection conn = getConnection(ds);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                DataCheckConfig c = new DataCheckConfig();
                c.setRuleId(rs.getLong("rule_id"));
                c.setDbType(rs.getString("db_type"));
                c.setRuleType(rs.getString("rule_type"));
                c.setRuleName(rs.getString("rule_name"));
                c.setExecFlag(rs.getString("exec_flag"));
                c.setApplyDataType(rs.getString("apply_data_type"));
                c.setCheckCondition(rs.getString("check_condition"));
                c.setCleanExpression(rs.getString("clean_expression"));
                c.setPriority(rs.getInt("priority"));
                c.setRuleDesc(rs.getString("rule_desc"));
                c.setCreateTime(rs.getTimestamp("create_time"));
                c.setUpdateTime(rs.getTimestamp("update_time"));
                list.add(c);
            }
        }
        return list;
    }

    public DataCheckConfig findById(Long id, DataSource ds) throws SQLException {
        String sql = "SELECT rule_id, db_type, rule_type, rule_name, exec_flag, apply_data_type, " +
                "check_condition, clean_expression, priority, rule_desc, create_time, update_time " +
                "FROM GK_DATA_CHECK_CLEAN_CONFIG WHERE rule_id = ?";
        try (Connection conn = getConnection(ds);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DataCheckConfig c = new DataCheckConfig();
                    c.setRuleId(rs.getLong("rule_id"));
                    c.setDbType(rs.getString("db_type"));
                    c.setRuleType(rs.getString("rule_type"));
                    c.setRuleName(rs.getString("rule_name"));
                    c.setExecFlag(rs.getString("exec_flag"));
                    c.setApplyDataType(rs.getString("apply_data_type"));
                    c.setCheckCondition(rs.getString("check_condition"));
                    c.setCleanExpression(rs.getString("clean_expression"));
                    c.setPriority(rs.getInt("priority"));
                    c.setRuleDesc(rs.getString("rule_desc"));
                    c.setCreateTime(rs.getTimestamp("create_time"));
                    c.setUpdateTime(rs.getTimestamp("update_time"));
                    return c;
                }
            }
        }
        return null;
    }

    private Long getMaxRuleId(Connection conn) throws SQLException {
        String sql = "SELECT NVL(MAX(rule_id), 0) FROM GK_DATA_CHECK_CLEAN_CONFIG";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0L;
    }

    public int insert(DataCheckConfig config, DataSource ds) throws SQLException {
        String sql = "INSERT INTO GK_DATA_CHECK_CLEAN_CONFIG " +
                "(rule_id, db_type, rule_type, rule_name, exec_flag, apply_data_type, " +
                "check_condition, clean_expression, priority, rule_desc, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE, SYSDATE)";
        try (Connection conn = getConnection(ds);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (config.getRuleId() == null) {
                Long maxId = getMaxRuleId(conn);
                config.setRuleId(maxId + 1);
            }
            ps.setLong(1, config.getRuleId());
            ps.setString(2, config.getDbType());
            ps.setString(3, config.getRuleType());
            ps.setString(4, config.getRuleName());
            ps.setString(5, config.getExecFlag());
            ps.setString(6, config.getApplyDataType());
            ps.setString(7, config.getCheckCondition());
            ps.setString(8, config.getCleanExpression());
            ps.setInt(9, config.getPriority() != null ? config.getPriority() : 99);
            ps.setString(10, config.getRuleDesc());
            return ps.executeUpdate();
        }
    }

    public int update(DataCheckConfig config, DataSource ds) throws SQLException {
        String sql = "UPDATE GK_DATA_CHECK_CLEAN_CONFIG SET " +
                "db_type = ?, rule_type = ?, rule_name = ?, exec_flag = ?, apply_data_type = ?, " +
                "check_condition = ?, clean_expression = ?, priority = ?, rule_desc = ?, update_time = SYSDATE " +
                "WHERE rule_id = ?";
        try (Connection conn = getConnection(ds);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, config.getDbType());
            ps.setString(2, config.getRuleType());
            ps.setString(3, config.getRuleName());
            ps.setString(4, config.getExecFlag());
            ps.setString(5, config.getApplyDataType());
            ps.setString(6, config.getCheckCondition());
            ps.setString(7, config.getCleanExpression());
            ps.setInt(8, config.getPriority() != null ? config.getPriority() : 99);
            ps.setString(9, config.getRuleDesc());
            ps.setLong(10, config.getRuleId());
            return ps.executeUpdate();
        }
    }

    public int delete(Long id, DataSource ds) throws SQLException {
        String sql = "DELETE FROM GK_DATA_CHECK_CLEAN_CONFIG WHERE rule_id = ?";
        try (Connection conn = getConnection(ds);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }
}