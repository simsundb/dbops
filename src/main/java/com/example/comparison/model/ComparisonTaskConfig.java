package com.example.comparison.model;

public class ComparisonTaskConfig {
    private String sourceSchema;
    private String targetSchema;
    private String enableFlag;

    public String getSourceSchema() { return sourceSchema; }
    public void setSourceSchema(String sourceSchema) { this.sourceSchema = sourceSchema; }
    public String getTargetSchema() { return targetSchema; }
    public void setTargetSchema(String targetSchema) { this.targetSchema = targetSchema; }
    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String enableFlag) { this.enableFlag = enableFlag; }
}