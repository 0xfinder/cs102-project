package com.group5.smartattendance.persistence;

import com.group5.smartattendance.core.Configuration;

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
    private static boolean INITIALIZED = false;
    private static String INITIALIZED_DB_PATH = null;

    private DatabaseManager() {
        // prevent instantiation since class only has static methods
    }

    public static void initialize() {
        String currentDbPath = Configuration.getInstance().getDbPath();
        if (!INITIALIZED || !currentDbPath.equals(INITIALIZED_DB_PATH)) {
            try {
                Path dbPath = Paths.get(currentDbPath);
                Files.createDirectories(dbPath.getParent());
                applySchema(dbPath);
                INITIALIZED = true;
                INITIALIZED_DB_PATH = currentDbPath;
            } catch (IOException | SQLException ex) {
                INITIALIZED = false;
                INITIALIZED_DB_PATH = null;
                throw new IllegalStateException("Failed to initialize database", ex);
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        Path dbPath = Paths.get(Configuration.getInstance().getDbPath());
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toString();

        initialize();
        Connection connection = DriverManager.getConnection(jdbcUrl);
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
        Path dbPath = Paths.get(Configuration.getInstance().getDbPath());
        Files.deleteIfExists(dbPath);
    }

    private static void applySchema(Path dbPath) throws IOException, SQLException {
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toString();
        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
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
