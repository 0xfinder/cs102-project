package com.group5.smartattendance.core;

public class Configuration {
    private static Configuration instance = new Configuration();

    private String dbPath = "data/attendance.db"; // default path

    private Configuration() {} // private constructor

    public static Configuration getInstance() {
        return instance;
    }

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }
}