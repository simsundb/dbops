package com.example.datacheck;

import com.example.core.DataSource;
import com.example.core.DataSourceStore;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class DataCheckProcedureDao {

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
    public void generateScript(String tableOwner, String tableName, String dbType) throws SQLException {
        generateScript(tableOwner, tableName, dbType, null);
    }

    public void executeBatch(String batchId) throws SQLException {
        executeBatch(batchId, null);
    }

    // 带数据源参数的方法
    public void generateScript(String tableOwner, String tableName, String dbType, DataSource ds) throws SQLException {
        String sql = "{call SP_GEN_DCC_SCRIPT(?, ?, ?)}";
        try (Connection conn = getConnection(ds);
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setString(1, tableOwner);
            cs.setString(2, tableName);
            cs.setString(3, dbType);
            cs.execute();
        }
    }

    public void executeBatch(String batchId, DataSource ds) throws SQLException {
        String sql = "{call SP_EXEC_DCC_BATCH(?)}";
        try (Connection conn = getConnection(ds);
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setString(1, batchId);
            cs.execute();
        }
    }
}