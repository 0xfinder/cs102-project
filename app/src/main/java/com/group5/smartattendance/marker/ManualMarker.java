package com.group5.smartattendance.marker;

import com.group5.smartattendance.session.Session;
import com.group5.smartattendance.student.Student;

import java.sql.SQLException;
import java.time.Instant;
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

        if (existingRecord.isPresent()) {
            AttendanceRecord current = existingRecord.get();

            // update to manual (handles empty method or AUTO override)
            return AttendanceManager.update(current.setManual(status, markedAt, notes));
        }

        // record doesn't exist, create new one
        return AttendanceManager.update(AttendanceRecord.createManual(session, student, status, markedAt, notes));
    }
}
