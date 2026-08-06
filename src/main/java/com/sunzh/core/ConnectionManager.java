package com.sunzh.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据库连接管理
 * 支持 Oracle 11/19/21 和 GaussDB
 */
public class ConnectionManager {

    // 驱动类名常量
    private static final String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";
    private static final String ORACLE_DRIVER_OLD = "oracle.jdbc.driver.OracleDriver";
    private static final String GAUSSDB_DRIVER = "com.huawei.gaussdb.jdbc.Driver";
    private static final String OPENGAUSS_DRIVER = "org.opengauss.Driver";

    /**
     * 加载合适的驱动（自动识别类型）
     */
    private static void loadDriver(DataSource ds) throws ClassNotFoundException {
        if (ds == null) return;
        String type = ds.getType();
        if ("ORACLE".equalsIgnoreCase(type)) {
            try {
                Class.forName(ORACLE_DRIVER);
            } catch (ClassNotFoundException e) {
                // 兼容旧驱动类名
                Class.forName(ORACLE_DRIVER_OLD);
            }
        } else if ("GAUSSDB".equalsIgnoreCase(type)) {
            try {
                Class.forName(GAUSSDB_DRIVER);
            } catch (ClassNotFoundException e) {
                // 尝试 openGauss 驱动
                Class.forName(OPENGAUSS_DRIVER);
            }
        } else {
            throw new ClassNotFoundException("不支持的数据库类型: " + type);
        }
    }

    /**
     * 测试数据库连接
     * @param ds 数据源对象
     * @return true 连接成功，false 连接失败
     */
    public static boolean testConnection(DataSource ds) {
        try {
            loadDriver(ds);
            String url = ds.buildUrl();
            // 二次修正可能存在的错误 scheme
            if (url != null && url.startsWith("jdbc:gausssdb://")) {
                url = url.replace("jdbc:gausssdb://", "jdbc:gaussdb://");
            }
            try (Connection conn = DriverManager.getConnection(url, ds.getUser(), ds.getPassword())) {
                return conn != null && !conn.isClosed();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取数据库连接（供 DAO 使用）
     * @param ds 数据源
     * @return Connection
     * @throws SQLException 连接失败
     */
    public static Connection getConnection(DataSource ds) throws SQLException {
        try {
            loadDriver(ds);
            String url = ds.buildUrl();
            if (url != null && url.startsWith("jdbc:gausssdb://")) {
                url = url.replace("jdbc:gausssdb://", "jdbc:gaussdb://");
            }
            return DriverManager.getConnection(url, ds.getUser(), ds.getPassword());
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC 驱动加载失败: " + e.getMessage(), e);
        }
    }
}