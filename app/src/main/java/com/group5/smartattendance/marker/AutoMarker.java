package com.group5.smartattendance.marker;

import com.group5.smartattendance.session.Session;
import com.group5.smartattendance.student.Student;

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
        // double confidence = request.confidence();
        // if (confidence < confirmationLowerBound) {
        // throw new IllegalArgumentException("Recognition confidence below minimum
        // threshold");
        // }

        // if (confidence < recognitionThreshold) {
        // boolean confirmed = request.confirmationHandler()
        // .map(handler -> handler.confirm(request.session(), request.student(),
        // confidence))
        // .orElse(false);
        // if (!confirmed) {
        // throw new IllegalArgumentException("Low-confidence recognition was not
        // confirmed");
        // }
        // }

        Session session = request.getSession();
        Student student = request.getStudent();

        if (session.getStatus() != Session.Status.OPEN) {
            throw new IllegalArgumentException("Cannot mark attendance for a closed session");
        }

        if (!session.getRoster().contains(student.getId())) {
            throw new IllegalArgumentException("Student is not part of the session roster");
        }

        Instant markedAt = request.markedAt();

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
            // TODO: prob extra but prioritise presence over confidence
            if (markedAt.isAfter(sessionStart.plus(Duration.ofMinutes(15)))) {
                current = AttendanceManager.update(current.setStatus(AttendanceRecord.Status.LATE));
            } else {
                current = AttendanceManager.update(current.setStatus(AttendanceRecord.Status.PRESENT));
            }

            // if there is no method, update as auto
            // guard clause for no method
            if (current.getMethod().isEmpty()) {
                return AttendanceManager.update(current.setAuto(
                        markedAt,
                        request.confidence().orElse(Double.NaN),
                        request.notes().orElse(null)));
            }

            // if manual record, return
            if (current.getMethod().get() == AttendanceRecord.Method.MANUAL) {
                return current;
            }

            // if auto record, check if confidence is higher than current, if so, update
            if (current.getMethod().get() == AttendanceRecord.Method.AUTO) {
                double confidence = request.confidence().orElse(Double.NaN);
                if (Double.isNaN(confidence) || confidence > current.getConfidence()) {
                    return AttendanceManager.update(current.setAuto(
                            markedAt,
                            request.confidence().orElse(Double.NaN),
                            request.notes().orElse(null)));
                }
            }
        }

        AttendanceRecord.Status computedStatus = computeStatus(session, markedAt, ZoneId.systemDefault());
        AttendanceRecord newRecord = AttendanceRecord.createAuto(
                session,
                student,
                computedStatus,
                markedAt,
                request.confidence().orElse(Double.NaN),
                request.notes().orElse(null));
        return AttendanceManager.update(newRecord);
    }

    private AttendanceRecord.Status computeStatus(Session session, Instant eventTime, ZoneId zoneId) {
        Instant sessionStart = session.getSessionDate()
                .atTime(session.getStartTime())
                .atZone(zoneId)
                .toInstant();
        Instant lateCutoff = sessionStart.plus(Duration.ofMinutes(15));
        return eventTime.isAfter(lateCutoff) ? AttendanceRecord.Status.LATE : AttendanceRecord.Status.PRESENT;
    }
}
