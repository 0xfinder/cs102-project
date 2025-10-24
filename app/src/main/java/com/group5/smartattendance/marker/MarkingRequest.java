package com.group5.smartattendance.marker;

import com.group5.smartattendance.session.Session;
import com.group5.smartattendance.session.SessionManager;
import com.group5.smartattendance.student.Student;
import com.group5.smartattendance.persistence.StudentManager;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

// usage:
// MarkingRequest request = MarkingRequest.builder("sessionId", "studentId")
//         .markedAt(Instant.now())
//         .confidence(0.85)
//         .notes("Test recognition")
//         .build();
// AttendanceMarker marker = new AutoMarker();
// AttendanceRecord record = marker.markAttendance(request);

public record MarkingRequest(
        String sessionId,
        String studentId,
        Instant markedAt,
        Optional<AttendanceRecord.Status> desiredStatus,
        Optional<Double> confidence,
        Optional<String> notes) {

    public MarkingRequest {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        markedAt = markedAt == null ? Instant.now() : markedAt;
        desiredStatus = desiredStatus == null ? Optional.empty() : desiredStatus;
        confidence = confidence == null ? Optional.empty() : confidence;
        notes = notes == null ? Optional.empty() : notes;
    }

    // get session by id
    public Session getSession() throws SQLException {
        return new SessionManager().getSession(sessionId)
                .orElseThrow(() -> new SQLException("Session not found with ID: " + sessionId));
    }

    // get student by id
    public Student getStudent() throws SQLException {
        return StudentManager.findById(studentId)
                .orElseThrow(() -> new SQLException("Student not found with ID: " + studentId));
    }

    public static Builder builder(String sessionId, String studentId) {
        return new Builder(sessionId, studentId);
    }

    public static final class Builder {
        private final String sessionId;
        private final String studentId;
        private Instant markedAt;
        private AttendanceRecord.Status desiredStatus;
        private Double confidence;
        private Optional<String> notes;

        private Builder(String sessionId, String studentId) {
            this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
            this.studentId = Objects.requireNonNull(studentId, "studentId must not be null");
        }

        public Builder markedAt(Instant markedAt) {
            this.markedAt = markedAt;
            return this;
        }

        public Builder desiredStatus(AttendanceRecord.Status desiredStatus) {
            this.desiredStatus = desiredStatus;
            return this;
        }

        public Builder confidence(Double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes == null ? Optional.empty() : Optional.ofNullable(notes);
            return this;
        }

        public MarkingRequest build() {
            return new MarkingRequest(
                    sessionId,
                    studentId,
                    markedAt,
                    Optional.ofNullable(desiredStatus),
                    Optional.ofNullable(confidence),
                    notes);
        }
    }
}
