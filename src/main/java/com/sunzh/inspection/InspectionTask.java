package com.sunzh.inspection;

public class InspectionTask {
    private String description;
    private String sql;
    private String sqlFile;
    private boolean enabled;

    // 运行时状态
    private Status status = Status.PENDING;
    private String outputFileName;
    private int rowCount;
    private String errorMessage;

    public enum Status {
        PENDING, SUCCESS, NO_DATA, FAILED, SKIPPED
    }

    // ------ Getter / Setter ------
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }

    public String getSqlFile() { return sqlFile; }
    public void setSqlFile(String sqlFile) { this.sqlFile = sqlFile; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}