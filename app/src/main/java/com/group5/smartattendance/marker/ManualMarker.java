package com.group5.smartattendance.marker;

import com.group5.smartattendance.session.Session;
import com.group5.smartattendance.student.Student;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

public class ManualMarker implements AttendanceMarker {
    public ManualMarker() {
    }

    @Override
    public AttendanceRecord markAttendance(MarkingRequest request) throws SQLException {
        Objects.requireNonNull(request, "request must not be null");
        AttendanceRecord.Status status = request.desiredStatus()
                .orElseThrow(() -> new IllegalArgumentException("Manual marking requires a desired status"));

        Session session = request.getSession();
        Student student = request.getStudent();

        if (!session.getRoster().contains(student.getId())) {
            throw new IllegalArgumentException("Student is not part of the session roster");
        }

        Instant markedAt = request.markedAt();
        String notes = request.notes().orElse(null);

        Optional<AttendanceRecord> existingRecord = AttendanceManager.findBySessionAndStudentId(session.getId(),
                student.getId());
        // check if there is an existing record (should always be present)
        if (existingRecord.isPresent()) {
            AttendanceRecord current = existingRecord.get();
            // get session date and start time (in local time), convert to instant with zone
            // default
            Instant sessionStart = session.getSessionDate().atTime(session.getStartTime())
                    .atZone(ZoneId.systemDefault()).toInstant();
            // TODO: change to use config value for late threshold
            if (markedAt.isAfter(sessionStart.plus(Duration.ofMinutes(15)))) {
                current = AttendanceManager.update(current.setStatus(AttendanceRecord.Status.LATE));
            } else {
                current = AttendanceManager.update(current.setStatus(AttendanceRecord.Status.PRESENT));
            }

            // if not marked, set manual
            if (current.getMethod().isEmpty()) {
                return AttendanceManager.update(current.setManual(status, markedAt, notes));
            }

            // if auto, override
            if (current.getMethod().get() == AttendanceRecord.Method.AUTO) {
                return AttendanceManager.update(current.setManual(status, markedAt, notes));
            }

            // if manual, return
            if (current.getMethod().isPresent() && current.getMethod().get() == AttendanceRecord.Method.MANUAL) {
                return current;
            }
        }

        return AttendanceManager.update(AttendanceRecord.createManual(session, student, status, markedAt, notes));
    }
}
