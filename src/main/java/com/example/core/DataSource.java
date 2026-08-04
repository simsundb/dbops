package com.example.core;

import com.example.utils.CryptoUtils;

/**
 * 数据源实体类
 * 支持 Oracle 和 GaussDB 两种数据库类型
 * 密码使用 Base64 加密存储
 */
public class DataSource {
    private String name;
    private String type;        // "ORACLE" 或 "GAUSSDB"
    private String host;
    private int port;
    private String database;    // GaussDB 专用
    private String schema;      // GaussDB 专用
    private String user;
    private String password;    // Base64 加密存储
    private String serviceName; // Oracle 专用

    public DataSource() {}

    // ---- GaussDB 构造方法 ----
    public DataSource(String name, String type, String host, int port, String database,
                      String schema, String user, String password) {
        this.name = name;
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.schema = schema;
        this.user = user;
        this.password = CryptoUtils.encrypt(password);
    }

    // ---- Oracle 构造方法 ----
    public DataSource(String name, String type, String host, int port, String serviceName,
                      String user, String password) {
        this.name = name;
        this.type = type;
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
        this.user = user;
        this.password = CryptoUtils.encrypt(password);
    }

    // ---- Getter / Setter ----
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    /**
     * 获取明文密码（自动解密）
     */
    public String getPassword() {
        return CryptoUtils.decrypt(password);
    }

    /**
     * 设置密码（自动加密存储）
     */
    public void setPassword(String password) {
        this.password = CryptoUtils.encrypt(password);
    }

    public String getEncryptedPassword() { return password; }
    public void setEncryptedPassword(String encryptedPassword) { this.password = encryptedPassword; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    /**
     * 根据类型动态构建 JDBC URL
     */
    public String buildUrl() {
        if ("ORACLE".equalsIgnoreCase(type)) {
            if (serviceName == null || serviceName.trim().isEmpty()) return "";
            // 包含 . 或 - 视为 Service Name，使用 / 分隔
            if (serviceName.contains(".") || serviceName.contains("-")) {
                return "jdbc:oracle:thin:@" + host + ":" + port + "/" + serviceName;
            } else {
                return "jdbc:oracle:thin:@" + host + ":" + port + ":" + serviceName;
            }
        } else if ("GAUSSDB".equalsIgnoreCase(type)) {
            if (database == null || database.trim().isEmpty()) return "";
            String url = "jdbc:gaussdb://" + host + ":" + port + "/" + database;
            if (schema != null && !schema.trim().isEmpty()) {
                url += "?currentSchema=" + schema;
            }
            return url;
        }
        return "";
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}