package com.group5.smartattendance.marker;

import java.sql.SQLException;

public interface AttendanceMarker {

    AttendanceRecord markAttendance(MarkingRequest request) throws SQLException;
}
