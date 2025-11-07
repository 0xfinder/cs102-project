package com.group5.smartattendance.marker;

import com.group5.smartattendance.persistence.DatabaseManager;
import com.group5.smartattendance.session.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttendanceManager {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceManager.class);

    private static final String SELECT_BY_SESSION_AND_STUDENT = "SELECT id, session_id, student_id, status, marked_at, method, confidence, notes "
            + "FROM attendance_records WHERE session_id = ? AND student_id = ?";

    private static final String UPDATE_SQL = "UPDATE attendance_records SET status = ?, marked_at = ?, method = ?, confidence = ?, notes = ? WHERE id = ?";

    private static final String SELECT_BY_SESSION_ID = "SELECT id, session_id, student_id, status, marked_at, method, confidence, notes FROM attendance_records WHERE session_id = ?";

    // find an attendance record by session and student
    public static Optional<AttendanceRecord> findBySessionAndStudentId(String sessionId, String studentId)
            throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_BY_SESSION_AND_STUDENT)) {
            statement.setLong(1, Long.parseLong(sessionId));
            statement.setLong(2, Long.parseLong(studentId));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(AttendanceRecord.map(rs));
                }
            }
        } catch (SQLException ex) {
            logger.error("Failed to find attendance record for Student {} in Session {}", studentId, sessionId, ex);
            throw ex;
        }
        return Optional.empty();
    }

    // update an attendance record
    public static AttendanceRecord update(AttendanceRecord record) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            record.bindUpdate(statement);
            int updatedRows = statement.executeUpdate();
            if (updatedRows > 0) {
                logger.info("Attendance marked: Student {} in Session {} - Status: {}, Method: {}, Time: {}",
                        record.getStudent().getId(), record.getSession().getId(), record.getStatus(),
                        record.getMethod().map(m -> m.name()).orElse("Unknown"), record.getMarkedAt().orElse(null));
            } else {
                logger.warn("No attendance record updated for Student {} in Session {}", record.getStudent().getId(),
                        record.getSession().getId());
            }
            return record;
        } catch (SQLException ex) {
            logger.error("Failed to update attendance for Student {} in Session {}", record.getStudent().getId(),
                    record.getSession().getId(), ex);
            throw ex;
        }
    }

    // find all attendance records by session id
    public static List<AttendanceRecord> findBySession(Session session) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_BY_SESSION_ID)) {
            statement.setLong(1, Long.parseLong(session.getId()));
            try (ResultSet rs = statement.executeQuery()) {
                List<AttendanceRecord> records = new ArrayList<>();
                while (rs.next()) {
                    AttendanceRecord record = AttendanceRecord.map(rs);
                    records.add(record);
                }
                logger.debug("Found {} attendance records for Session {}", records.size(), session.getId());
                return records;
            }
        } catch (SQLException ex) {
            logger.error("Failed to find attendance records for Session {}", session.getId(), ex);
            throw ex;
        }
    }
}
