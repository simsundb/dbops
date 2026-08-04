package com.example.stats;

/**
 * 统计数据查询配置模型
 * 对应 stats_config.yaml 中的每个条目
 */
public class StatsConfig {
    private String description;
    private boolean enabled;
    private String sqlFile;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSqlFile() { return sqlFile; }
    public void setSqlFile(String sqlFile) { this.sqlFile = sqlFile; }

    @Override
    public String toString() {
        return description + (enabled ? " ✅" : " ⛔");
    }
}