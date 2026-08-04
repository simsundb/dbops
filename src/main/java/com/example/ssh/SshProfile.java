package com.example.ssh;

public class SshProfile {
    private String name;
    private String sshHost;
    private int sshPort = 22;
    private String sshUser = "root";
    private String sshPassword;
    private String execUser = "Ruby";
    private String dbHost;
    private int dbPort = 8000;
    private String dbName;
    private String dbUser;
    private String dbPassword;
    private String backupDir = "/data/dump";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSshHost() { return sshHost; }
    public void setSshHost(String sshHost) { this.sshHost = sshHost; }
    public int getSshPort() { return sshPort; }
    public void setSshPort(int sshPort) { this.sshPort = sshPort; }
    public String getSshUser() { return sshUser; }
    public void setSshUser(String sshUser) { this.sshUser = sshUser; }
    public String getSshPassword() { return sshPassword; }
    public void setSshPassword(String sshPassword) { this.sshPassword = sshPassword; }
    public String getExecUser() { return execUser; }
    public void setExecUser(String execUser) { this.execUser = execUser; }
    public String getDbHost() { return dbHost; }
    public void setDbHost(String dbHost) { this.dbHost = dbHost; }
    public int getDbPort() { return dbPort; }
    public void setDbPort(int dbPort) { this.dbPort = dbPort; }
    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }
    public String getDbUser() { return dbUser; }
    public void setDbUser(String dbUser) { this.dbUser = dbUser; }
    public String getDbPassword() { return dbPassword; }
    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }
    public String getBackupDir() { return backupDir; }
    public void setBackupDir(String backupDir) { this.backupDir = backupDir; }
}