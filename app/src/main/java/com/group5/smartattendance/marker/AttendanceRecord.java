package com.group5.smartattendance.marker;

import com.group5.smartattendance.core.Entity;
import com.group5.smartattendance.session.Session;
import com.group5.smartattendance.session.SessionManager;
import com.group5.smartattendance.student.Student;
import com.group5.smartattendance.persistence.StudentManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class AttendanceRecord extends Entity {

    public enum Status {
        PENDING,
        PRESENT,
        ABSENT,
        LATE
    }

    public enum Method {
        AUTO,
        MANUAL
    }

    private final Session session;
    private final Student student;
    private final Optional<Method> method;
    private final Optional<Instant> markedAt;
    private final double confidence;
    private final String notes;
    private final Status status;

    // general constructor
    private AttendanceRecord(
            String id,
            Session session,
            Student student,
            Status status,
            Optional<Method> method,
            Optional<Instant> markedAt,
            double confidence,
            String notes) {
        super(id);
        this.session = Objects.requireNonNull(session, "session must not be null");
        this.student = Objects.requireNonNull(student, "student must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.method = method == null ? Optional.empty() : method;
        this.markedAt = markedAt == null ? Optional.empty() : markedAt;
        this.confidence = confidence;
        this.notes = notes;
    }

    // attendancerecord for auto marking by live recognition
    public static AttendanceRecord createAuto(
            Session session,
            Student student,
            Status status,
            Instant markedAt,
            double confidence,
            String notes) {
        return new AttendanceRecord(
                null,
                session,
                student,
                status,
                Optional.of(Method.AUTO),
                Optional.ofNullable(markedAt),
                confidence,
                notes);
    }

    // attendancerecord for manual marking in edit session
    public static AttendanceRecord createManual(
            Session session,
            Student student,
            Status status,
            Instant markedAt,
            String notes) {
        return new AttendanceRecord(
                null,
                session,
                student,
                status,
                Optional.of(Method.MANUAL),
                Optional.ofNullable(markedAt),
                Double.NaN,
                notes);
    }

    // map attendancerecord from database row
    public static AttendanceRecord map(ResultSet rs) throws SQLException {
        String id = Long.toString(rs.getLong("id"));
        Status status = Status.valueOf(rs.getString("status"));
        String methodStr = rs.getString("method");
        Optional<Method> method = methodStr == null ? Optional.empty() : Optional.of(Method.valueOf(methodStr));
        String markedAtRaw = rs.getString("marked_at");
        Optional<Instant> markedAt = (markedAtRaw == null || markedAtRaw.isBlank())
                ? Optional.empty()
                : Optional.of(Instant.parse(markedAtRaw));
        double confidence = rs.getObject("confidence") == null ? Double.NaN : rs.getDouble("confidence");
        String notes = rs.getString("notes");

        // Get session and student IDs from the attendance record
        long sessionId = rs.getLong("session_id");
        long studentId = rs.getLong("student_id");

        // Fetch Session and Student using their respective managers
        SessionManager sessionManager = new SessionManager();
        Session session = sessionManager.getSession(Long.toString(sessionId))
                .orElseThrow(() -> new SQLException("Session not found with ID: " + sessionId));
        Student student = StudentManager.findById(Long.toString(studentId))
                .orElseThrow(() -> new SQLException("Student not found with ID: " + studentId));

        return new AttendanceRecord(
                id,
                session,
                student,
                status,
                method,
                markedAt,
                confidence,
                notes);
    }

    public Session getSession() {
        return session;
    }

    public Student getStudent() {
        return student;
    }

    public Status getStatus() {
        return status;
    }

    public Optional<Method> getMethod() {
        return method;
    }

    public Optional<Instant> getMarkedAt() {
        return markedAt;
    }

    public Optional<Instant> getLastSeen() {
        return markedAt;
    }

    public double getConfidence() {
        return confidence;
    }

    public Optional<String> getNotes() {
        return Optional.ofNullable(notes);
    }

    // bind to insert statement (probably not needed since seedRoster does this)
    public void bindInsert(PreparedStatement statement) throws SQLException {
        statement.setLong(1, Long.parseLong(session.getId()));
        statement.setLong(2, Long.parseLong(student.getId()));
        statement.setString(3, status.name());
        if (markedAt.isPresent()) {
            statement.setString(4, markedAt.get().toString());
        } else {
            statement.setNull(4, java.sql.Types.VARCHAR);
        }
        if (method.isPresent()) {
            statement.setString(5, method.get().name());
        } else {
            statement.setNull(5, java.sql.Types.VARCHAR);
        }
        if (Double.isNaN(confidence)) {
            statement.setNull(6, java.sql.Types.REAL);
        } else {
            statement.setDouble(6, confidence);
        }
        if (notes == null) {
            statement.setNull(7, java.sql.Types.VARCHAR);
        } else {
            statement.setString(7, notes);
        }
    }

    // bind to update statement
    public void bindUpdate(PreparedStatement statement) throws SQLException {
        statement.setString(1, status.name());
        if (markedAt.isPresent()) {
            statement.setString(2, markedAt.get().toString());
        } else {
            statement.setNull(2, java.sql.Types.VARCHAR);
        }
        if (method.isPresent()) {
            statement.setString(3, method.get().name());
        } else {
            statement.setNull(3, java.sql.Types.VARCHAR);
        }
        if (Double.isNaN(confidence)) {
            statement.setNull(4, java.sql.Types.REAL);
        } else {
            statement.setDouble(4, confidence);
        }
        if (notes == null) {
            statement.setNull(5, java.sql.Types.VARCHAR);
        } else {
            statement.setString(5, notes);
        }
        statement.setLong(6, Long.parseLong(getId()));
    }

    // set id
    public AttendanceRecord withId(String id) {
        return new AttendanceRecord(
                Objects.requireNonNull(id, "id must not be null"),
                session,
                student,
                status,
                method,
                markedAt,
                confidence,
                notes);
    }

    public AttendanceRecord setManual(
            Status status,
            Instant markedAt,
            String notes) {
        return new AttendanceRecord(
                getId(),
                session,
                student,
                status,
                Optional.of(Method.MANUAL),
                Optional.ofNullable(markedAt),
                Double.NaN,
                notes != null ? notes : this.notes);
    }

    public AttendanceRecord setAuto(
            Instant markedAt,
            double confidence,
            String notes) {
        return new AttendanceRecord(
                getId(),
                session,
                student,
                status,
                Optional.of(Method.AUTO),
                Optional.ofNullable(markedAt),
                confidence,
                notes != null ? notes : this.notes);
    }

    public AttendanceRecord setStatus(Status status) {
        return new AttendanceRecord(
                getId(),
                session,
                student,
                status,
                method,
                markedAt,
                confidence,
                notes);
    }
}
