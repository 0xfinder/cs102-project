package com.group5.smartattendance.persistence;

import java.sql.*;

public class DataSource {
    private static final String URL = "jdbc:sqlite:./data/attendance.db";

    private DataSource() {
    }

    public static Connection get() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            s.execute("PRAGMA journal_mode = WAL");
            s.execute("PRAGMA synchronous = NORMAL");
        }
        return c;
    }

}
