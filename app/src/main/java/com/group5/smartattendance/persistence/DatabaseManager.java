package com.group5.smartattendance.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// db manager that connects to sqlite db and initializes schemas
public final class DatabaseManager {
    private static final Path DATABASE_PATH = Paths.get("data", "attendance.db");
    private static final String JDBC_URL = "jdbc:sqlite:" + DATABASE_PATH.toString();
    private static boolean INITIALIZED = false;

    private DatabaseManager() {
        // prevent instantiation since class only has static methods
    }

    public static void initialize() {
        if (!INITIALIZED) {
            try {
                Files.createDirectories(DATABASE_PATH.getParent());
                applySchema();
                INITIALIZED = true;
            } catch (IOException | SQLException ex) {
                INITIALIZED = false;
                throw new IllegalStateException("Failed to initialize database", ex);
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        initialize();
        Connection connection = DriverManager.getConnection(JDBC_URL);
        try (Statement s = connection.createStatement()) {
            // set sqlite config
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA journal_mode=WAL");
            s.execute("PRAGMA synchronous=NORMAL");
        }
        return connection;
    }

    // delete database for testing
    public static void deleteDatabase() throws IOException {
        INITIALIZED = false;
        Files.deleteIfExists(DATABASE_PATH);
    }

    private static void applySchema() throws IOException, SQLException {
        try (Connection connection = DriverManager.getConnection(JDBC_URL)) {
            // rollback all changes if any single table creation fails
            connection.setAutoCommit(false);
            try {
                // read and execute schema file
                try (InputStream schemaStream = DatabaseManager.class.getResourceAsStream("/schema.sql")) {
                    if (schemaStream == null) {
                        throw new IOException("Missing schema.sql resource");
                    }

                    // read whole file as a single line and split statements by ';'
                    String schema = new String(schemaStream.readAllBytes(), StandardCharsets.UTF_8);
                    for (String rawStatement : schema.split(";")) {
                        String statementText = rawStatement.trim();
                        if (statementText.isEmpty()) {
                            continue;
                        }
                        try (Statement statement = connection.createStatement()) {
                            statement.execute(statementText);
                        }
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}
