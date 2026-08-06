package com.example.datacheck;

import java.util.Date;

public class DataCheckDetail {
    private Long logId;
    private String batchId;
    private String tableOwner;
    private String tableName;
    private String columnName;
    private Long ruleId;
    private String ruleType;
    private String ruleName;
    private String applyDataType;
    private Integer priority;
    private String checkSql;          // VARCHAR2 部分
    private String checkSqlClob;      // CLOB 部分（可能合并读取）
    private Integer checkSqlLen;
    private String cleanSql;
    private String cleanSqlClob;
    private Integer cleanSqlLen;
    private String checkStatus;       // W/R/S/E/N
    private Date checkStartTime;
    private Date checkEndTime;
    private String checkErrorMsg;
    private Long checkRowCount;
    private String cleanStatus;       // N/W/R/S/E
    private Date cleanStartTime;
    private Date cleanEndTime;
    private String cleanErrorMsg;
    private Long cleanRowCount;
    private String execFlag;          // Y/N
    private Date createTime;

    // 构造器、getter/setter
    public DataCheckDetail() {}

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getTableOwner() { return tableOwner; }
    public void setTableOwner(String tableOwner) { this.tableOwner = tableOwner; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getApplyDataType() { return applyDataType; }
    public void setApplyDataType(String applyDataType) { this.applyDataType = applyDataType; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getCheckSql() { return checkSql; }
    public void setCheckSql(String checkSql) { this.checkSql = checkSql; }

    public String getCheckSqlClob() { return checkSqlClob; }
    public void setCheckSqlClob(String checkSqlClob) { this.checkSqlClob = checkSqlClob; }

    public Integer getCheckSqlLen() { return checkSqlLen; }
    public void setCheckSqlLen(Integer checkSqlLen) { this.checkSqlLen = checkSqlLen; }

    public String getCleanSql() { return cleanSql; }
    public void setCleanSql(String cleanSql) { this.cleanSql = cleanSql; }

    public String getCleanSqlClob() { return cleanSqlClob; }
    public void setCleanSqlClob(String cleanSqlClob) { this.cleanSqlClob = cleanSqlClob; }

    public Integer getCleanSqlLen() { return cleanSqlLen; }
    public void setCleanSqlLen(Integer cleanSqlLen) { this.cleanSqlLen = cleanSqlLen; }

    public String getCheckStatus() { return checkStatus; }
    public void setCheckStatus(String checkStatus) { this.checkStatus = checkStatus; }

    public Date getCheckStartTime() { return checkStartTime; }
    public void setCheckStartTime(Date checkStartTime) { this.checkStartTime = checkStartTime; }

    public Date getCheckEndTime() { return checkEndTime; }
    public void setCheckEndTime(Date checkEndTime) { this.checkEndTime = checkEndTime; }

    public String getCheckErrorMsg() { return checkErrorMsg; }
    public void setCheckErrorMsg(String checkErrorMsg) { this.checkErrorMsg = checkErrorMsg; }

    public Long getCheckRowCount() { return checkRowCount; }
    public void setCheckRowCount(Long checkRowCount) { this.checkRowCount = checkRowCount; }

    public String getCleanStatus() { return cleanStatus; }
    public void setCleanStatus(String cleanStatus) { this.cleanStatus = cleanStatus; }

    public Date getCleanStartTime() { return cleanStartTime; }
    public void setCleanStartTime(Date cleanStartTime) { this.cleanStartTime = cleanStartTime; }

    public Date getCleanEndTime() { return cleanEndTime; }
    public void setCleanEndTime(Date cleanEndTime) { this.cleanEndTime = cleanEndTime; }

    public String getCleanErrorMsg() { return cleanErrorMsg; }
    public void setCleanErrorMsg(String cleanErrorMsg) { this.cleanErrorMsg = cleanErrorMsg; }

    public Long getCleanRowCount() { return cleanRowCount; }
    public void setCleanRowCount(Long cleanRowCount) { this.cleanRowCount = cleanRowCount; }

    public String getExecFlag() { return execFlag; }
    public void setExecFlag(String execFlag) { this.execFlag = execFlag; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    // 辅助方法：获取完整的检查SQL（合并VARCHAR2和CLOB）
    public String getFullCheckSql() {
        if (checkSqlClob != null && !checkSqlClob.isEmpty()) return checkSqlClob;
        return checkSql;
    }
    public String getFullCleanSql() {
        if (cleanSqlClob != null && !cleanSqlClob.isEmpty()) return cleanSqlClob;
        return cleanSql;
    }
}