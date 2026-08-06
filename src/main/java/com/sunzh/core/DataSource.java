package com.sunzh.core;

import com.sunzh.utils.CryptoUtils;

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

    // 新增：Oracle 连接类型，true=Service Name（斜杠），false=SID（冒号），默认 true
    private boolean useServiceName = true;

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

    // ---- Oracle 构造方法（保留兼容，默认 Service Name） ----
    public DataSource(String name, String type, String host, int port, String serviceName,
                      String user, String password) {
        this(name, type, host, port, serviceName, user, password, true);
    }

    // ---- Oracle 构造方法（增加连接类型参数） ----
    public DataSource(String name, String type, String host, int port, String serviceName,
                      String user, String password, boolean useServiceName) {
        this.name = name;
        this.type = type;
        this.host = host;
        this.port = port;
        this.serviceName = serviceName;
        this.user = user;
        this.password = CryptoUtils.encrypt(password);
        this.useServiceName = useServiceName;
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

    public String getPassword() {
        return CryptoUtils.decrypt(password);
    }
    public void setPassword(String password) {
        this.password = CryptoUtils.encrypt(password);
    }

    public String getEncryptedPassword() { return password; }
    public void setEncryptedPassword(String encryptedPassword) { this.password = encryptedPassword; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public boolean isUseServiceName() { return useServiceName; }
    public void setUseServiceName(boolean useServiceName) { this.useServiceName = useServiceName; }

    /**
     * 根据类型动态构建 JDBC URL
     * Oracle：根据 useServiceName 决定使用斜杠（Service Name）还是冒号（SID）
     */
    public String buildUrl() {
        if ("ORACLE".equalsIgnoreCase(type)) {
            if (serviceName == null || serviceName.trim().isEmpty()) return "";
            if (useServiceName) {
                // Service Name 格式：jdbc:oracle:thin:@host:port/serviceName
                return "jdbc:oracle:thin:@" + host + ":" + port + "/" + serviceName;
            } else {
                // SID 格式：jdbc:oracle:thin:@host:port:serviceName（即 SID）
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