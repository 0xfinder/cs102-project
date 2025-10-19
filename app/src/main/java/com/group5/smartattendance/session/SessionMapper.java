package com.group5.smartattendance.session;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

// class to convert between JDBC representations of session records and the
public final class SessionMapper {

    private SessionMapper() {
    }

    public static Session map(ResultSet rs) throws SQLException {
        return map(rs, null);
    }

    public static Session map(ResultSet rs, Roster roster) throws SQLException {
        String id = rs.getString("id");
        String courseName = rs.getString("course_name");
        LocalDate sessionDate = LocalDate.parse(rs.getString("session_date"));
        LocalTime startTime = LocalTime.parse(rs.getString("start_time"));
        LocalTime endTime = LocalTime.parse(rs.getString("end_time"));
        String location = getNullableString(rs, "location");
        Session.Status status = parseStatus(rs.getString("status"));
        return new Session(id, courseName, sessionDate, startTime, endTime, location, status, roster);
    }

    public static void bindInsert(PreparedStatement stmt, Session session) throws SQLException {
        stmt.setString(1, session.getCourseName());
        stmt.setString(2, session.getSessionDate().toString());
        stmt.setString(3, session.getStartTime().toString());
        stmt.setString(4, session.getEndTime().toString());
        setNullableString(stmt, 5, session.getLocation().orElse(null));
        stmt.setString(6, session.getStatus().name());
    }

    public static void bindUpdate(PreparedStatement stmt, Session session) throws SQLException {
        bindInsert(stmt, session);
        stmt.setString(7, session.getId());
    }

    private static Session.Status parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return Session.Status.OPEN;
        }
        return Session.Status.valueOf(rawStatus.toUpperCase(Locale.ROOT));
    }

    private static String getNullableString(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        if (rs.wasNull()) {
            return null;
        }
        return value;
    }

    private static void setNullableString(PreparedStatement stmt, int parameterIndex, String value)
            throws SQLException {
        if (value == null) {
            stmt.setNull(parameterIndex, Types.VARCHAR);
        } else {
            stmt.setString(parameterIndex, value);
        }
    }
}
