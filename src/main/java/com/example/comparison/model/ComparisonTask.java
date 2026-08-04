package com.example.comparison.model;

/**
 * 任务表实体类，对应数据库表 gk_sjdb_task
 */
public class ComparisonTask {
    private String jobId;
    private String jobName;
    private String jobDesc;
    private String sourceSchema;
    private String targetSchema;
    private String compareTypes;
    private String enableFlag;
    private String execStatus;
    private String startTime;
    private String endTime;
    private Integer durationSeconds;
    private String errorMsg;
    private String tableStatus;
    private String columnStatus;
    private String indexStatus;
    private String sequenceStatus;
    private String synonymStatus;
    private String tableErrorMsg;
    private String columnErrorMsg;
    private String indexErrorMsg;
    private String sequenceErrorMsg;
    private String synonymErrorMsg;

    // ============================================================
    // Getter 和 Setter
    // ============================================================

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobDesc() {
        return jobDesc;
    }

    public void setJobDesc(String jobDesc) {
        this.jobDesc = jobDesc;
    }

    public String getSourceSchema() {
        return sourceSchema;
    }

    public void setSourceSchema(String sourceSchema) {
        this.sourceSchema = sourceSchema;
    }

    public String getTargetSchema() {
        return targetSchema;
    }

    public void setTargetSchema(String targetSchema) {
        this.targetSchema = targetSchema;
    }

    public String getCompareTypes() {
        return compareTypes;
    }

    public void setCompareTypes(String compareTypes) {
        this.compareTypes = compareTypes;
    }

    public String getEnableFlag() {
        return enableFlag;
    }

    public void setEnableFlag(String enableFlag) {
        this.enableFlag = enableFlag;
    }

    public String getExecStatus() {
        return execStatus;
    }

    public void setExecStatus(String execStatus) {
        this.execStatus = execStatus;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getTableStatus() {
        return tableStatus;
    }

    public void setTableStatus(String tableStatus) {
        this.tableStatus = tableStatus;
    }

    public String getColumnStatus() {
        return columnStatus;
    }

    public void setColumnStatus(String columnStatus) {
        this.columnStatus = columnStatus;
    }

    public String getIndexStatus() {
        return indexStatus;
    }

    public void setIndexStatus(String indexStatus) {
        this.indexStatus = indexStatus;
    }

    public String getSequenceStatus() {
        return sequenceStatus;
    }

    public void setSequenceStatus(String sequenceStatus) {
        this.sequenceStatus = sequenceStatus;
    }

    public String getSynonymStatus() {
        return synonymStatus;
    }

    public void setSynonymStatus(String synonymStatus) {
        this.synonymStatus = synonymStatus;
    }

    public String getTableErrorMsg() {
        return tableErrorMsg;
    }

    public void setTableErrorMsg(String tableErrorMsg) {
        this.tableErrorMsg = tableErrorMsg;
    }

    public String getColumnErrorMsg() {
        return columnErrorMsg;
    }

    public void setColumnErrorMsg(String columnErrorMsg) {
        this.columnErrorMsg = columnErrorMsg;
    }

    public String getIndexErrorMsg() {
        return indexErrorMsg;
    }

    public void setIndexErrorMsg(String indexErrorMsg) {
        this.indexErrorMsg = indexErrorMsg;
    }

    public String getSequenceErrorMsg() {
        return sequenceErrorMsg;
    }

    public void setSequenceErrorMsg(String sequenceErrorMsg) {
        this.sequenceErrorMsg = sequenceErrorMsg;
    }

    public String getSynonymErrorMsg() {
        return synonymErrorMsg;
    }

    public void setSynonymErrorMsg(String synonymErrorMsg) {
        this.synonymErrorMsg = synonymErrorMsg;
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    @Override
    public String toString() {
        return "ComparisonTask{" +
                "jobId='" + jobId + '\'' +
                ", jobName='" + jobName + '\'' +
                ", sourceSchema='" + sourceSchema + '\'' +
                ", targetSchema='" + targetSchema + '\'' +
                ", execStatus='" + execStatus + '\'' +
                '}';
    }
}