package com.example.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据库连接管理
 * 支持 Oracle 和 GaussDB 连接测试
 */
public class ConnectionManager {

    /**
     * 测试数据库连接
     * @param ds 数据源对象
     * @return true 连接成功，false 连接失败
     */
    public static boolean testConnection(DataSource ds) {
        try {
            if ("ORACLE".equalsIgnoreCase(ds.getType())) {
                Class.forName("oracle.jdbc.driver.OracleDriver");
            } else if ("GAUSSDB".equalsIgnoreCase(ds.getType())) {
                Class.forName("com.huawei.gaussdb.jdbc.Driver");
            } else {
                return false;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        String url = ds.buildUrl();
        try (Connection conn = DriverManager.getConnection(url, ds.getUser(), ds.getPassword())) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}