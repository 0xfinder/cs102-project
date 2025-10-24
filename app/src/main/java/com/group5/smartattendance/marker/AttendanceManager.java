package com.group5.smartattendance.marker;

import com.group5.smartattendance.persistence.DatabaseManager;
import com.group5.smartattendance.session.Session;
import com.group5.smartattendance.student.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttendanceManager {

    private static final String SELECT_BY_SESSION_AND_STUDENT = "SELECT id, session_id, student_id, status, marked_at, method, confidence, notes "
            + "FROM attendance_records WHERE session_id = ? AND student_id = ?";

    private static final String UPDATE_SQL = "UPDATE attendance_records SET status = ?, marked_at = ?, method = ?, confidence = ?, notes = ? WHERE id = ?";

    private static final String SELECT_BY_SESSION_ID = "SELECT id, session_id, student_id, status, marked_at, method, confidence, notes FROM attendance_records WHERE session_id = ?";

    // find an attendance record by session and student
    public static Optional<AttendanceRecord> findBySessionAndStudent(Session session, Student student)
            throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_BY_SESSION_AND_STUDENT)) {
            statement.setLong(1, Long.parseLong(session.getId()));
            statement.setLong(2, Long.parseLong(student.getId()));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(AttendanceRecord.map(rs));
                }
            }
        }
        return Optional.empty();
    }

    // update an attendance record
    public static AttendanceRecord update(AttendanceRecord record) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            record.bindUpdate(statement);
            statement.executeUpdate();
            return record;
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
                return records;
            }
        }
    }
}
