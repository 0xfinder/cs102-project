package com.group5.smartattendance.session;

import com.group5.smartattendance.marker.AttendanceManager;
import com.group5.smartattendance.marker.AttendanceRecord;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

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

    private static final String SELECT_ROSTER_IDS_SQL = """
            SELECT student_id
            FROM attendance_records
            WHERE session_id = ?
            """;

    private static final String DELETE_ROSTER_ENTRY_SQL = """
            DELETE FROM attendance_records
            WHERE session_id = ? AND student_id = ?
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
                logger.info("Session created: {} on {} with roster size {}", persisted.getCourseName(),
                        persisted.getSessionDate(), persisted.getRoster().size());
                return persisted;
            } catch (SQLException ex) {
                connection.rollback();
                logger.error("Failed to create session: {}", courseName, ex);
                throw new SessionManagerException("Failed to create session", ex);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            logger.error("Database error while creating session: {}", courseName, ex);
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
            logger.error("Failed to list sessions", ex);
            throw new SessionManagerException("Failed to list sessions", ex);
        }
    }

    public Optional<Session> getSession(String sessionId) {
        long id = Long.parseLong(sessionId);
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
            logger.error("Failed to fetch session {}", sessionId, ex);
            throw new SessionManagerException("Failed to fetch session " + sessionId, ex);
        }
    }

    public Session updateSession(Session session) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(session.getId(), "session id must not be null");
        long id = Long.parseLong(session.getId());

        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SESSION_SQL)) {
                SessionMapper.bindUpdate(statement, session);
                int updatedRows = statement.executeUpdate();
                if (updatedRows == 0) {
                    throw new SessionManagerException("Session " + session.getId() + " not found");
                }
                syncRoster(connection, id, session.getRoster());
                Session updated = findSession(connection, id)
                        .orElseThrow(() -> new SessionManagerException(
                                "Session " + session.getId() + " not found after update"));
                connection.commit();
                logger.info("Session updated: {} (ID: {})", updated.getCourseName(), updated.getId());
                return updated;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                logger.error("Failed to update session: {}", session.getId(), ex);
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new SessionManagerException("Failed to update session " + session.getId(), ex);
        }
    }

    public Session updateRoster(String sessionId, Collection<Student> students) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        long id = Long.parseLong(sessionId);
        Roster desiredRoster;
        try {
            desiredRoster = students == null ? new Roster() : new Roster(students);
        } catch (IllegalArgumentException ex) {
            throw new SessionManagerException("Invalid roster students", ex);
        }

        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Session session = findSession(connection, id)
                        .orElseThrow(() -> new SessionManagerException("Session " + sessionId + " not found"));
                syncRoster(connection, id, desiredRoster);
                Roster updatedRoster = fetchRoster(connection, id);
                Session updatedSession = session.copyWithRoster(updatedRoster);
                connection.commit();
                logger.info("Roster updated for session {}: {} students", sessionId, updatedRoster.size());
                return updatedSession;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                logger.error("Failed to update roster for session {}", sessionId, ex);
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            logger.error("Database error while updating roster for session {}", sessionId, ex);
            throw new SessionManagerException("Failed to update roster for session " + sessionId, ex);
        }
    }

    public Session openSession(String sessionId) {
        return setStatus(sessionId, Session.Status.OPEN);
    }

    public Session closeSession(String sessionId) {
        // for everyone in the roster that has PENDING status in attendance record,
        // set status to absent
        long id = Long.parseLong(sessionId);
        try (Connection connection = connectionProvider.getConnection()) {
            Session session = findSession(connection, id)
                    .orElseThrow(() -> new SessionManagerException("Session " + sessionId + " not found"));
            Roster roster = session.getRoster();
            for (Student student : roster.getStudents()) {
                Optional<AttendanceRecord> attendanceRecord = AttendanceManager.findBySessionAndStudentId(
                        session.getId(),
                        student.getId());
                if (attendanceRecord.isPresent()
                        && attendanceRecord.get().getStatus() == AttendanceRecord.Status.PENDING) {
                    AttendanceRecord existing = attendanceRecord.get();
                    AttendanceRecord updated = existing.setManual(AttendanceRecord.Status.ABSENT, Instant.now(), null);
                    AttendanceManager.update(updated);
                }
            }
            logger.info("Closed session {}, marked remaining students as absent", sessionId);
        } catch (SQLException ex) {
            logger.error("Failed to close session {}", sessionId, ex);
            throw new SessionManagerException("Failed to close session " + sessionId, ex);
        }
        return setStatus(sessionId, Session.Status.CLOSED);
    }

    public void deleteSession(String sessionId) {
        long id = Long.parseLong(sessionId);
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
                logger.info("Session {} deleted", sessionId);
            } catch (SessionManagerException ex) {
                connection.rollback();
                logger.error("Cannot delete session {}: {}", sessionId, ex.getMessage());
                throw ex;
            } catch (SQLException ex) {
                connection.rollback();
                logger.error("Database error deleting session {}", sessionId, ex);
                throw new SessionManagerException("Failed to delete session " + sessionId, ex);
            } catch (RuntimeException ex) {
                connection.rollback();
                logger.error("Unexpected error deleting session {}", sessionId, ex);
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            logger.error("Database connection error deleting session {}", sessionId, ex);
            throw new SessionManagerException("Failed to delete session " + sessionId, ex);
        }
    }

    public Roster loadRoster(String sessionId) {
        long id = Long.parseLong(sessionId);
        try (Connection connection = connectionProvider.getConnection()) {
            return fetchRoster(connection, id);
        } catch (SQLException ex) {
            logger.error("Failed to load roster for session {}", sessionId, ex);
            throw new SessionManagerException("Failed to load roster for session " + sessionId, ex);
        }
    }

    private Session setStatus(String sessionId, Session.Status status) {
        long id = Long.parseLong(sessionId);
        try (Connection connection = connectionProvider.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS_SQL)) {
            statement.setString(1, status.name());
            statement.setLong(2, id);
            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new SessionManagerException("Session " + sessionId + " not found");
            }
            logger.info("Session {} status set to {}", sessionId, status);
            return getSession(sessionId)
                    .orElseThrow(() -> new SessionManagerException(
                            "Session " + sessionId + " not found after status update"));
        } catch (SQLException ex) {
            logger.error("Failed to set status for session {}", sessionId, ex);
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
                statement.setLong(2, Long.parseLong(student.getId()));
                statement.setString(3, AttendanceRecord.Status.PENDING.name());
                statement.setNull(4, Types.VARCHAR);
                statement.setNull(5, Types.VARCHAR);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void syncRoster(Connection connection, long sessionId, Roster roster) throws SQLException {
        Roster effectiveRoster = roster == null ? new Roster() : roster;

        List<Student> desiredStudents = effectiveRoster.getStudents();
        Set<String> desiredIds = new LinkedHashSet<>();
        for (Student student : desiredStudents) {
            String studentId = Objects.requireNonNull(student.getId(), "Roster student id must not be null");
            desiredIds.add(studentId);
        }

        Set<String> existingIds = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ROSTER_IDS_SQL)) {
            statement.setLong(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    existingIds.add(resultSet.getString(1));
                }
            }
        }

        Set<String> toRemove = new LinkedHashSet<>(existingIds);
        toRemove.removeAll(desiredIds);

        Set<String> toAdd = new LinkedHashSet<>(desiredIds);
        toAdd.removeAll(existingIds);

        if (!toRemove.isEmpty()) {
            try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE_ROSTER_ENTRY_SQL)) {
                for (String studentId : toRemove) {
                    deleteStatement.setLong(1, sessionId);
                    deleteStatement.setString(2, studentId);
                    deleteStatement.addBatch();
                }
                deleteStatement.executeBatch();
            }
        }

        if (!toAdd.isEmpty()) {
            try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_ATTENDANCE_SEED_SQL)) {
                for (String studentId : toAdd) {
                    insertStatement.setLong(1, sessionId);
                    insertStatement.setString(2, studentId);
                    insertStatement.setString(3, AttendanceRecord.Status.PENDING.name());
                    insertStatement.setNull(4, Types.VARCHAR);
                    insertStatement.setNull(5, Types.VARCHAR);
                    insertStatement.addBatch();
                }
                insertStatement.executeBatch();
            }
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
                    Long.toString(resultSet.getLong("id")),
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
