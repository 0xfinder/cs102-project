package com.group5.smartattendance.marker;

import com.group5.smartattendance.session.Session;
import com.group5.smartattendance.student.Student;
import com.group5.smartattendance.core.Configuration;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

public class AutoMarker implements AttendanceMarker {

    // // all these are from config file
    // private final double recognitionThreshold;
    // private final double confirmationLowerBound;
    // // minutes
    // private final Duration lateThreshold;
    // //
    // private final Duration cooldownDuration;

    public AutoMarker() {
        // this.recognitionThreshold = 0.8;
        // this.confirmationLowerBound = 0.6;
        // this.lateThreshold = Duration.ofMinutes(15);
        // this.cooldownDuration = Duration.ofSeconds(30);

        // // validate config
        // if (recognitionThreshold < confirmationLowerBound) {
        // throw new IllegalArgumentException("recognitionThreshold must be >=
        // confirmationLowerBound");
        // }
        // if (recognitionThreshold < 0 || confirmationLowerBound < 0) {
        // throw new IllegalArgumentException("confidence thresholds must be
        // non-negative");
        // }
        // if (lateThreshold.isNegative() || cooldownDuration.isNegative()) {
        // throw new IllegalArgumentException("Durations must be non-negative");
        // }
    }

    @Override
    // mark attendance for a student in a session
    public AttendanceRecord markAttendance(MarkingRequest request) throws SQLException {
        Objects.requireNonNull(request, "request must not be null");

        Session session = request.getSession();
        Student student = request.getStudent();

        if (session.getStatus() != Session.Status.OPEN) {
            throw new IllegalArgumentException("Cannot mark attendance for a closed session");
        }

        if (!session.getRoster().contains(student.getId())) {
            throw new IllegalArgumentException("Student is not part of the session roster");
        }

        Instant markedAt = request.markedAt();
        AttendanceRecord.Status newStatus = computeStatus(session, markedAt, ZoneId.systemDefault());

        Optional<AttendanceRecord> existingRecord = AttendanceManager.findBySessionAndStudentId(session.getId(),
                student.getId());

        if (existingRecord.isPresent()) {
            AttendanceRecord current = existingRecord.get();

            // Update status only if improving to PRESENT or if current is not PRESENT
            if (newStatus == AttendanceRecord.Status.PRESENT
                    || current.getStatus() != AttendanceRecord.Status.PRESENT) {
                current = AttendanceManager.update(current.setStatus(newStatus));
            }

            // Handle method and confidence updates
            if (current.getMethod().isEmpty()) {
                return AttendanceManager.update(current.setAuto(
                        markedAt,
                        request.confidence().orElse(Double.NaN),
                        request.notes().orElse(null)));
            } else if (current.getMethod().get() == AttendanceRecord.Method.MANUAL) {
                return current;
            } else {
                // if confidence is higher than current, update
                double confidence = request.confidence().orElse(Double.NaN);
                if (Double.isNaN(confidence) || confidence > current.getConfidence()) {
                    return AttendanceManager.update(current.setAuto(
                            markedAt,
                            confidence,
                            request.notes().orElse(null)));
                }
                return current;
            }
        } else {
            // No existing record, create new
            AttendanceRecord newRecord = AttendanceRecord.createAuto(
                    session,
                    student,
                    newStatus,
                    markedAt,
                    request.confidence().orElse(Double.NaN),
                    request.notes().orElse(null));
            return AttendanceManager.update(newRecord);
        }
    }

    private AttendanceRecord.Status computeStatus(Session session, Instant eventTime, ZoneId zoneId) {
        Instant sessionStart = session.getSessionDate()
                .atTime(session.getStartTime())
                .atZone(zoneId)
                .toInstant();
        Instant lateCutoff = sessionStart
                .plus(Duration.ofMinutes(Configuration.getInstance().getLateThresholdMinutes()));
        return eventTime.isAfter(lateCutoff) ? AttendanceRecord.Status.LATE : AttendanceRecord.Status.PRESENT;
    }
}
