package com.group5.smartattendance.session;

import com.group5.smartattendance.persistence.DatabaseManager;
import com.group5.smartattendance.student.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SessionManager {

    private static final String INSERT_SESSION_SQL = """
            INSERT INTO sessions (course_name, session_date, start_time, end_time, location, status)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SESSION_SQL = """
            UPDATE sessions
            SET course_name = ?, session_date = ?, start_time = ?, end_time = ?, location = ?, status = ?
            WHERE id = ?
            """;

    private static final String UPDATE_STATUS_SQL = """
            UPDATE sessions SET status = ? WHERE id = ?
            """;

    private static final String DELETE_SESSION_SQL = """
            DELETE FROM sessions WHERE id = ?
            """;

    private static final String SELECT_ALL_SESSIONS_SQL = """
            SELECT * FROM sessions ORDER BY session_date DESC, start_time DESC
            """;

    private static final String SELECT_SESSION_BY_ID_SQL = """
            SELECT * FROM sessions WHERE id = ?
            """;

    private static final String INSERT_ATTENDANCE_SEED_SQL = """
            INSERT OR IGNORE INTO attendance_records (session_id, student_id, status, marked_at, method)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SELECT_ROSTER_SQL = """
            SELECT s.id, s.name, s.class_group, s.email, s.phone, s.enrollment_date
            FROM attendance_records ar
            INNER JOIN students s ON ar.student_id = s.id
            WHERE ar.session_id = ?
            ORDER BY s.name COLLATE NOCASE
            """;

    private final ConnectionProvider connectionProvider;

    public SessionManager() {
        this(DatabaseManager::getConnection);
    }

    public SessionManager(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider must not be null");
    }

    public Session createSession(
            String courseName,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String location,
            Roster roster) {
        Objects.requireNonNull(courseName, "courseName must not be null");
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");

        LocalDate effectiveDate = sessionDate != null ? sessionDate : LocalDate.now();
        Roster effectiveRoster = roster == null ? new Roster() : roster;
        Session newSession = new Session(
                null,
                courseName,
                effectiveDate,
                startTime,
                endTime,
                location,
                Session.Status.OPEN,
                effectiveRoster);

        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Session persisted = insertSession(connection, newSession);
                seedRoster(connection, persisted);
                connection.commit();
                return persisted;
            } catch (SQLException ex) {
                connection.rollback();
                throw new SessionManagerException("Failed to create session", ex);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new SessionManagerException("Failed to create session", ex);
        }
    }

    public List<Session> listSessions() {
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SESSIONS_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            List<Session> sessions = new ArrayList<>();
            while (resultSet.next()) {
                long sessionId = resultSet.getLong("id");
                Roster roster = fetchRoster(connection, sessionId);
                sessions.add(SessionMapper.map(resultSet, roster));
            }
            return sessions;
        } catch (SQLException ex) {
            throw new SessionManagerException("Failed to list sessions", ex);
        }
    }

    public Optional<Session> getSession(String sessionId) {
        long id = parseSessionId(sessionId);
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_SESSION_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Roster roster = fetchRoster(connection, id);
                    return Optional.of(SessionMapper.map(resultSet, roster));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new SessionManagerException("Failed to fetch session " + sessionId, ex);
        }
    }

    public Session updateSession(Session session) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(session.getId(), "session id must not be null");

        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SESSION_SQL)) {
            SessionMapper.bindUpdate(statement, session);
            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new SessionManagerException("Session " + session.getId() + " not found");
            }
            return getSession(session.getId())
                    .orElseThrow(() -> new SessionManagerException(
                            "Session " + session.getId() + " not found after update"));
        } catch (SQLException ex) {
            throw new SessionManagerException("Failed to update session " + session.getId(), ex);
        }
    }

    public Session openSession(String sessionId) {
        return setStatus(sessionId, Session.Status.OPEN);
    }

    public Session closeSession(String sessionId) {
        return setStatus(sessionId, Session.Status.CLOSED);
    }

    public void deleteSession(String sessionId) {
        long id = parseSessionId(sessionId);
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Session session = findSession(connection, id)
                        .orElseThrow(() -> new SessionManagerException("Session " + sessionId + " not found"));
                if (session.getStatus() == Session.Status.OPEN) {
                    throw new SessionManagerException("Cannot delete an open session");
                }
                try (PreparedStatement statement = connection.prepareStatement(DELETE_SESSION_SQL)) {
                    statement.setLong(1, id);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SessionManagerException ex) {
                connection.rollback();
                throw ex;
            } catch (SQLException ex) {
                connection.rollback();
                throw new SessionManagerException("Failed to delete session " + sessionId, ex);
            } catch (RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new SessionManagerException("Failed to delete session " + sessionId, ex);
        }
    }

    public Roster loadRoster(String sessionId) {
        long id = parseSessionId(sessionId);
        try (Connection connection = connectionProvider.getConnection()) {
            return fetchRoster(connection, id);
        } catch (SQLException ex) {
            throw new SessionManagerException("Failed to load roster for session " + sessionId, ex);
        }
    }

    private Session setStatus(String sessionId, Session.Status status) {
        long id = parseSessionId(sessionId);
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS_SQL)) {
            statement.setString(1, status.name());
            statement.setLong(2, id);
            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new SessionManagerException("Session " + sessionId + " not found");
            }
            return getSession(sessionId)
                    .orElseThrow(() -> new SessionManagerException(
                            "Session " + sessionId + " not found after status update"));
        } catch (SQLException ex) {
            throw new SessionManagerException("Failed to update session status", ex);
        }
    }

    private Session insertSession(Connection connection, Session session) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SESSION_SQL,
                Statement.RETURN_GENERATED_KEYS)) {
            SessionMapper.bindInsert(statement, session);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Missing generated id for session");
                }
                long id = generatedKeys.getLong(1);
                return new Session(
                        Long.toString(id),
                        session.getCourseName(),
                        session.getSessionDate(),
                        session.getStartTime(),
                        session.getEndTime(),
                        session.getLocation().orElse(null),
                        session.getStatus(),
                        session.getRoster());
            }
        }
    }

    private Roster fetchRoster(Connection connection, long sessionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ROSTER_SQL)) {
            statement.setLong(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Student> students = new ArrayList<>();
                while (resultSet.next()) {
                    students.add(mapStudent(resultSet));
                }
                return new Roster(students);
            }
        }
    }

    private void seedRoster(Connection connection, Session session) throws SQLException {
        Roster roster = session.getRoster();
        if (roster == null || roster.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(INSERT_ATTENDANCE_SEED_SQL)) {
            long sessionId = Long.parseLong(Objects.requireNonNull(session.getId(), "session id required"));
            for (Student student : roster.getStudents()) {
                statement.setLong(1, sessionId);
                statement.setString(2, student.getId());
                statement.setString(3, "PENDING");
                statement.setNull(4, Types.VARCHAR);
                statement.setNull(5, Types.VARCHAR);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private Optional<Session> findSession(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_SESSION_BY_ID_SQL)) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Roster roster = fetchRoster(connection, id);
                        return Optional.of(SessionMapper.map(resultSet, roster));
                    }
                    return Optional.empty();
                }
            }
        }

    private Student mapStudent(ResultSet resultSet) throws SQLException {
        try {
            String enrollmentDateRaw = resultSet.getString("enrollment_date");
            return new Student(
                    resultSet.getString("id"),
                    resultSet.getString("name"),
                    resultSet.getString("class_group"),
                    resultSet.getString("email"),
                    resultSet.getString("phone"),
                    parseEnrollmentDate(enrollmentDateRaw));
        } catch (DateTimeParseException ex) {
            throw new SessionManagerException("Failed to parse enrollment date for roster entry", ex);
        }
    }

    private Instant parseEnrollmentDate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(rawValue);
        } catch (DateTimeParseException primaryParseFailure) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(
                        rawValue,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return dateTime.toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                throw primaryParseFailure;
            }
        }
    }

    private long parseSessionId(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        try {
            return Long.parseLong(sessionId);
        } catch (NumberFormatException ex) {
            throw new SessionManagerException("Session id must be numeric", ex);
        }
    }

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

    public static class SessionManagerException extends RuntimeException {
        public SessionManagerException(String message) {
            super(message);
        }

        public SessionManagerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
