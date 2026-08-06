package com.sunzh.datacheck;

import java.util.Date;

public class DataCheckConfig {
    private Long ruleId;
    private String dbType;
    private String ruleType;
    private String ruleName;
    private String execFlag;          // Y/N
    private String applyDataType;     // STRING/NUMBER/DATE/ALL
    private String checkCondition;
    private String cleanExpression;
    private Integer priority;
    private String ruleDesc;
    private Date createTime;
    private Date updateTime;

    // 构造器、getter/setter
    public DataCheckConfig() {}

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getDbType() { return dbType; }
    public void setDbType(String dbType) { this.dbType = dbType; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getExecFlag() { return execFlag; }
    public void setExecFlag(String execFlag) { this.execFlag = execFlag; }

    public String getApplyDataType() { return applyDataType; }
    public void setApplyDataType(String applyDataType) { this.applyDataType = applyDataType; }

    public String getCheckCondition() { return checkCondition; }
    public void setCheckCondition(String checkCondition) { this.checkCondition = checkCondition; }

    public String getCleanExpression() { return cleanExpression; }
    public void setCleanExpression(String cleanExpression) { this.cleanExpression = cleanExpression; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getRuleDesc() { return ruleDesc; }
    public void setRuleDesc(String ruleDesc) { this.ruleDesc = ruleDesc; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}