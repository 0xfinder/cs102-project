package com.group5.smartattendance.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.time.Instant;

import com.group5.smartattendance.persistence.StudentManager;
import com.group5.smartattendance.session.Roster;
import com.group5.smartattendance.session.Session;
import com.group5.smartattendance.session.SessionManager;
import com.group5.smartattendance.student.Student;
import com.group5.smartattendance.marker.AttendanceManager;
import com.group5.smartattendance.marker.AttendanceRecord;
import com.group5.smartattendance.marker.ManualMarker;
import com.group5.smartattendance.marker.MarkingRequest;
import com.group5.smartattendance.user.AuthManager;
import com.group5.smartattendance.core.AuditLogger;

public class SessionView extends JFrame {

    private JTable sessionTable;
    private DefaultTableModel sessionModel;
    private JButton btnEdit, btnNewSession, btnDelete, btnBack, btnCloseSession, btnReopenSession;
    private SessionManager sessionManager;
    private List<Session> sessions = new ArrayList<>();
    private AuditLogger auditLogger;

    public SessionView() {
        // initialize session manager and audit logger
        sessionManager = new SessionManager();
        String currentUser = AuthManager.getCurrentUser() != null ? AuthManager.getCurrentUser().getEmail() : "SYSTEM";
        auditLogger = new AuditLogger(currentUser);

        setTitle("All Sessions");
        setLayout(null);
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel lblHeader = new JLabel("Session Management", SwingConstants.CENTER);
        lblHeader.setFont(new Font("Dialog", Font.BOLD, 22));
        lblHeader.setBounds(0, 20, 800, 30);
        add(lblHeader);

        // Table setup
        String[] columns = { "Course", "Date", "Start Time", "End Time", "Location", "Status" };
        sessionModel = new DefaultTableModel(null, columns) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        sessionTable = new JTable(sessionModel);
        JScrollPane scrollPane = new JScrollPane(sessionTable);
        scrollPane.setBounds(50, 70, 700, 250);
        add(scrollPane);

        int btnWidth = 150;
        int btnHeight = 35;
        int btnSpacingX = 30;
        int btnSpacingY = 15;
        int windowWidth = 800;

        int totalTopWidth = 4 * btnWidth + 3 * btnSpacingX;
        int startXTop = (windowWidth - totalTopWidth) / 2;
        int startYTop = 350;

        int totalBottomWidth = 2 * btnWidth + btnSpacingX;
        int startXBottom = (windowWidth - totalBottomWidth) / 2;
        int startYBottom = startYTop + btnHeight + btnSpacingY;

        // Edit button
        btnEdit = new JButton("Edit Session");
        btnEdit.setBounds(startXTop, startYTop, btnWidth, btnHeight);
        btnEdit.addActionListener(e -> openSelectedSession());
        add(btnEdit);

        // New session
        btnNewSession = new JButton("New Session");
        btnNewSession.setBounds(startXTop + btnWidth + btnSpacingX, startYTop, btnWidth, btnHeight);
        btnNewSession.addActionListener(e -> createNewSession());
        add(btnNewSession);

        // Close session
        btnCloseSession = new JButton("Close Session");
        btnCloseSession.setBounds(startXTop + 2 * (btnWidth + btnSpacingX), startYTop, btnWidth, btnHeight);
        btnCloseSession.addActionListener(e -> closeSelectedSession());
        add(btnCloseSession);

        // Delete session
        btnDelete = new JButton("Delete Session");
        btnDelete.setBounds(startXTop + 3 * (btnWidth + btnSpacingX), startYTop, btnWidth, btnHeight);
        btnDelete.addActionListener(e -> deleteSelectedSession());
        add(btnDelete);

        // Reopen session
        btnReopenSession = new JButton("Reopen Session");
        btnReopenSession.setBounds(startXBottom, startYBottom, btnWidth, btnHeight);
        btnReopenSession.addActionListener(e -> reopenSelectedSession());
        add(btnReopenSession);

        // Back
        btnBack = new JButton("Back");
        btnBack.setBounds(startXBottom + btnWidth + btnSpacingX, startYBottom, btnWidth, btnHeight);
        btnBack.addActionListener(e -> dispose());
        add(btnBack);

        // Load sessions from persistence (after buttons are created)
        loadSessions();

        setVisible(true);
    }

    private void loadSessions() {
        sessions = sessionManager.listSessions();
        sessionModel.setRowCount(0);
        for (Session session : sessions) {
            sessionModel.addRow(new Object[] {
                    session.getCourseName(),
                    session.getSessionDate().toString(),
                    session.getStartTime().toString(),
                    session.getEndTime().toString(),
                    session.getLocation().orElse(""),
                    session.getStatus().name()
            });
        }
    }

    private void openSelectedSession() {
        int selected = sessionTable.getSelectedRow();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this, "Please select a session to edit.");
            return;
        }

        Session session = sessions.get(selected);

        try {
            Roster roster = sessionManager.loadRoster(session.getId());
            List<Object[]> rosterRows = toRosterRows(roster);
            new SessionDetailDialog(this, session, rosterRows, this::loadSessions);
        } catch (SessionManager.SessionManagerException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load roster: " + ex.getMessage(),
                    "Session Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void addPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(Color.GRAY);

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().trim().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(Color.GRAY);
                }
            }
        });
    }

    private void createNewSession() {
        // create inputs for course name, session date, start time, end time, location
        JTextField courseNameField = new JTextField(20);
        JTextField sessionDateField = new JTextField(20);
        JTextField startTimeField = new JTextField(20);
        JTextField endTimeField = new JTextField(20);
        JTextField locationField = new JTextField(20);

        // add placeholder text to fields to show example format of inputs
        // TODO: make placeholder text greyed out
        LocalDate todayDate = LocalDate.now();
        addPlaceholder(courseNameField, "Enter course name");
        sessionDateField.setText(todayDate.toString());
        sessionDateField.setForeground(Color.BLACK);
        addPlaceholder(startTimeField, "Enter start time (HH:MM)");
        addPlaceholder(endTimeField, "Enter end time (HH:MM)");
        addPlaceholder(locationField, "Enter location (optional)");

        // create a panel with the input fields
        JPanel panel = new JPanel(new GridLayout(5, 2));
        panel.add(new JLabel("Course Name:"));
        panel.add(courseNameField);
        panel.add(new JLabel("Session Date:"));
        panel.add(sessionDateField);
        panel.add(new JLabel("Start Time:"));
        panel.add(startTimeField);
        panel.add(new JLabel("End Time:"));
        panel.add(endTimeField);
        panel.add(new JLabel("Location:"));
        panel.add(locationField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Create New Session",
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String courseName = courseNameField.getText();
            String sessionDate = sessionDateField.getText();
            String startTime = startTimeField.getText();
            String endTime = endTimeField.getText();
            String location = locationField.getText();

            // show a dialog until all required fields are valid; preserve entered values
            LocalDate parsedSessionDate = null;
            LocalTime parsedStartTime = null;
            LocalTime parsedEndTime = null;
            boolean valid = false;
            while (!valid) {
                valid = true;
                StringBuilder errorMsg = new StringBuilder();
                parsedSessionDate = null;
                parsedStartTime = null;
                parsedEndTime = null;

                // validate fields
                if (courseName == null || courseName.trim().isEmpty() || courseName.equals("Enter course name")) {
                    errorMsg.append("Course name is required.\n");
                    valid = false;
                } else {
                    courseName = courseName.trim();
                }
                if (sessionDate == null || sessionDate.trim().isEmpty()) {
                    // auto set to today's date
                    sessionDate = LocalDate.now().toString();
                    sessionDateField.setText(sessionDate);
                    parsedSessionDate = LocalDate.parse(sessionDate);
                } else {
                    sessionDate = sessionDate.trim();
                    try {
                        parsedSessionDate = LocalDate.parse(sessionDate);
                    } catch (DateTimeParseException ex) {
                        errorMsg.append("Session date must be in ISO format (YYYY-MM-DD).\n");
                        valid = false;
                    }
                }
                if (startTime == null || startTime.trim().isEmpty()) {
                    errorMsg.append("Start time is required.\n");
                    valid = false;
                } else {
                    startTime = startTime.trim();
                    try {
                        parsedStartTime = LocalTime.parse(startTime);
                    } catch (DateTimeParseException ex) {
                        errorMsg.append("Start time must be in 24-hour format (HH:MM).\n");
                        valid = false;
                    }
                }
                if (endTime == null || endTime.trim().isEmpty()) {
                    errorMsg.append("End time is required.");
                    valid = false;
                } else {
                    endTime = endTime.trim();
                    try {
                        parsedEndTime = LocalTime.parse(endTime);
                    } catch (DateTimeParseException ex) {
                        errorMsg.append("End time must be in 24-hour format (HH:MM).");
                        valid = false;
                    }
                }
                // TODO: validate that start time is before end time
                if (valid && parsedStartTime != null && parsedEndTime != null) {
                    if (!parsedStartTime.isBefore(parsedEndTime)) {
                        errorMsg.append("Start time must be before end time.\n");
                        valid = false;
                    }
                }

                if (!valid) {
                    JOptionPane.showMessageDialog(this, errorMsg.toString().trim(), "Validation Error",
                            JOptionPane.ERROR_MESSAGE);

                    // pop up the input dialog again, with current values retained
                    courseNameField.setText(courseName != null ? courseName : "");
                    sessionDateField.setText(sessionDate != null ? sessionDate : "");
                    startTimeField.setText(startTime != null ? startTime : "");
                    endTimeField.setText(endTime != null ? endTime : "");
                    locationField.setText(location != null ? location : "");

                    JPanel retryPanel = new JPanel(new GridLayout(5, 2));
                    retryPanel.add(new JLabel("Course Name:"));
                    retryPanel.add(courseNameField);
                    retryPanel.add(new JLabel("Session Date:"));
                    retryPanel.add(sessionDateField);
                    retryPanel.add(new JLabel("Start Time:"));
                    retryPanel.add(startTimeField);
                    retryPanel.add(new JLabel("End Time:"));
                    retryPanel.add(endTimeField);
                    retryPanel.add(new JLabel("Location:"));
                    retryPanel.add(locationField);

                    int dialogResult = JOptionPane.showConfirmDialog(this, retryPanel, "Create New Session",
                            JOptionPane.OK_CANCEL_OPTION);

                    if (dialogResult != JOptionPane.OK_OPTION) {
                        // user cancelled dialog, stop the loop/creation process
                        return;
                    }

                    // re-fetch user input
                    courseName = courseNameField.getText();
                    sessionDate = sessionDateField.getText();
                    startTime = startTimeField.getText();
                    endTime = endTimeField.getText();
                    location = locationField.getText();

                }
            }

            location = (location != null) ? location.trim() : "";

            if (location.isEmpty() || location.equals("Enter location (optional)")) {
                location = "";
            }
            sessionManager.createSession(courseName, parsedSessionDate, parsedStartTime,
                    parsedEndTime, location, new Roster());
            loadSessions();
            JOptionPane.showMessageDialog(this, "Session created successfully.");
        }
    }

    private void deleteSelectedSession() {
        int selected = sessionTable.getSelectedRow();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this, "Select a session to delete.");
            return;
        }

        Session session = sessions.get(selected);
        if (session.getStatus() == Session.Status.OPEN) {
            JOptionPane.showMessageDialog(this, "Cannot delete an active (open) session.");
            return;
        }

        try {
            sessionManager.deleteSession(session.getId());
            loadSessions();
            JOptionPane.showMessageDialog(this, "Session deleted successfully.");
        } catch (SessionManager.SessionManagerException ex) {
            JOptionPane.showMessageDialog(this, "Failed to delete session: " + ex.getMessage(),
                    "Session Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeSelectedSession() {
        int selected = sessionTable.getSelectedRow();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this, "Please select a session to close.");
            return;
        }

        Session session = sessions.get(selected);
        if (session.getStatus() == Session.Status.CLOSED) {
            JOptionPane.showMessageDialog(this, "Cannot close an already closed session.",
                    "Session Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to close this session?\nOnce closed, it will be read-only until reopened.",
                "Confirm Close", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                sessionManager.closeSession(session.getId());
                auditLogger.logSessionArchived(session.getId(), session.getCourseName());
                loadSessions();
                JOptionPane.showMessageDialog(this, "Session closed successfully.");
            } catch (SessionManager.SessionManagerException ex) {
                JOptionPane.showMessageDialog(this, "Failed to close session: " + ex.getMessage(),
                        "Session Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void reopenSelectedSession() {
        int selected = sessionTable.getSelectedRow();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this, "Please select a session to reopen.");
            return;
        }

        Session session = sessions.get(selected);
        if (session.getStatus() == Session.Status.OPEN) {
            JOptionPane.showMessageDialog(this, "Cannot reopen an already open session.",
                    "Session Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reopen this archived session?\nThis will allow editing and new attendance marking.",
                "Confirm Reopen", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                sessionManager.reopenSession(session.getId());
                auditLogger.logSessionReopened(session.getId(), session.getCourseName());
                loadSessions();
                JOptionPane.showMessageDialog(this, "Session reopened successfully.");
            } catch (SessionManager.SessionManagerException ex) {
                JOptionPane.showMessageDialog(this, "Failed to reopen session: " + ex.getMessage(),
                        "Session Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private List<Object[]> toRosterRows(Roster roster) {
        List<Object[]> rows = new ArrayList<>();
        if (roster == null) {
            return rows;
        }
        for (Student student : roster.getStudents()) {
            rows.add(new Object[] {
                    student.getId(),
                    student.getName(),
                    "Pending",
                    "-",
                    "-" });
        }
        return rows;
    }

    // Inner dialog class for editing a single session's attendance roster
    private static class SessionDetailDialog extends JDialog {
        private final Session session;
        private JLabel lblHeader, lblCounters;
        private JTable attendanceTable;
        private DefaultTableModel attendanceModel;
        private JButton btnSaveChanges, btnCloseSession;
        private final List<String> lateAlerts = new ArrayList<>();

        public SessionDetailDialog(JFrame parent, Session session, List<Object[]> rosterData, Runnable onUpdate) {
            super(parent, "Session Detail - " + session.getCourseName()
                    + (session.getStatus() == Session.Status.CLOSED ? " (READ-ONLY)" : ""), true);
            this.session = session;
            boolean isClosed = session.getStatus() == Session.Status.CLOSED;
            setLayout(null);

            final int window_w = 1000;
            final int window_h = 500;

            // === Header ===
            lblHeader = new JLabel(
                    "Session: " + session.getCourseName() + " - " + session.getSessionDate(),
                    SwingConstants.CENTER);
            lblHeader.setFont(new Font("Dialog", Font.BOLD, 18));
            lblHeader.setBounds(0, 10, window_w, 25);
            add(lblHeader);

            // === Editable session info fields ===
            JLabel lblCourseName = new JLabel("Course:");
            JTextField txtCourseName = new JTextField(session.getCourseName(), 15);

            JLabel lblDate = new JLabel("Date (YYYY-MM-DD):");
            JTextField txtDate = new JTextField(session.getSessionDate().toString(), 10);

            JLabel lblStart = new JLabel("Start Time (HH:MM):");
            JTextField txtStart = new JTextField(session.getStartTime().toString(), 8);

            JLabel lblEnd = new JLabel("End Time (HH:MM):");
            JTextField txtEnd = new JTextField(session.getEndTime().toString(), 8);

            JLabel lblLocation = new JLabel("Location:");
            JTextField txtLocation = new JTextField(session.getLocation().orElse(""), 15);

            // If closed, disable all fields
            if (isClosed) {
                txtCourseName.setEditable(false);
                txtDate.setEditable(false);
                txtStart.setEditable(false);
                txtEnd.setEditable(false);
                txtLocation.setEditable(false);
            }

            JPanel detailsPanel = new JPanel(new GridLayout(2, 5, 5, 5));
            detailsPanel.add(lblCourseName);
            detailsPanel.add(txtCourseName);
            detailsPanel.add(lblDate);
            detailsPanel.add(txtDate);
            detailsPanel.add(lblLocation);
            detailsPanel.add(txtLocation);
            detailsPanel.add(lblStart);
            detailsPanel.add(txtStart);
            detailsPanel.add(lblEnd);
            detailsPanel.add(txtEnd);
            detailsPanel.add(new JLabel(""));

            detailsPanel.setBounds(20, 40, window_w - 40, 60);
            add(detailsPanel);

            // === Attendance table ===
            List<Student> allStudents;
            List<Object[]> tableRows;
            try {
                allStudents = StudentManager.findAll();
                List<AttendanceRecord> records = AttendanceManager.findBySession(session);
                tableRows = buildAttendanceTableRows(allStudents, records);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to load attendance data: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                tableRows = new ArrayList<>();
            }

            String[] columns = { "Included", "ID", "Name", "Status", "Timestamp", "Method", "Notes" };
            attendanceModel = new DefaultTableModel(tableRows.toArray(new Object[0][]), columns) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    if (isClosed)
                        return false; // No edits if closed
                    if (col == 0)
                        return true; // checkbox
                    Boolean included = (Boolean) getValueAt(row, 0);
                    return included != null && included && (col == 3 || col == 6);
                }

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return columnIndex == 0 ? Boolean.class : String.class;
                }
            };

            attendanceTable = new JTable(attendanceModel);

            // set column widths
            attendanceTable.getColumnModel().getColumn(0).setPreferredWidth(50); // Included
            attendanceTable.getColumnModel().getColumn(1).setPreferredWidth(30); // ID
            attendanceTable.getColumnModel().getColumn(2).setPreferredWidth(120); // Name
            attendanceTable.getColumnModel().getColumn(3).setPreferredWidth(50); // Status
            attendanceTable.getColumnModel().getColumn(4).setPreferredWidth(150); // Timestamp
            attendanceTable.getColumnModel().getColumn(5).setPreferredWidth(70); // Method
            attendanceTable.getColumnModel().getColumn(6).setPreferredWidth(150); // Notes

            TableColumn statusColumn = attendanceTable.getColumnModel().getColumn(3);
            statusColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>(
                    new String[] { "PENDING", "PRESENT", "ABSENT", "LATE" })));

            JScrollPane scrollPane = new JScrollPane(attendanceTable);
            scrollPane.setBounds(20, 110, window_w - 40, 250);
            add(scrollPane);

            lblCounters = new JLabel("", SwingConstants.LEFT);
            lblCounters.setFont(new Font("Dialog", Font.PLAIN, 14));
            lblCounters.setBounds(20, 370, window_w - 40, 25);
            add(lblCounters);

            // === Buttons ===
            btnSaveChanges = new JButton("Save Changes");
            btnSaveChanges.setBounds(250, 410, 180, 30);
            if (isClosed) {
                btnSaveChanges.setEnabled(false);
            }
            btnSaveChanges.addActionListener(e -> {
                try {
                    String courseName = txtCourseName.getText().trim();
                    LocalDate date = LocalDate.parse(txtDate.getText().trim());
                    LocalTime start = LocalTime.parse(txtStart.getText().trim());
                    LocalTime end = LocalTime.parse(txtEnd.getText().trim());
                    String location = txtLocation.getText().trim();

                    if (courseName.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Course name cannot be empty.");
                        return;
                    }
                    if (start.isAfter(end)) {
                        JOptionPane.showMessageDialog(this, "Start time must be before end time.");
                        return;
                    }

                    Session updatedSession = new Session(
                            session.getId(),
                            courseName,
                            date,
                            start,
                            end,
                            location.isEmpty() ? null : location,
                            session.getStatus(),
                            session.getRoster());

                    SessionManager sm = new SessionManager();
                    sm.updateSession(updatedSession);

                    saveManualChanges();
                    updateCountersAndAlerts();
                    lblHeader.setText("Session: " + courseName + " - " + date);
                    // JOptionPane.showMessageDialog(this, "Session updated successfully.");

                    if (onUpdate != null) {
                        onUpdate.run();
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Failed to save changes: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            add(btnSaveChanges);

            btnCloseSession = new JButton("Back");
            btnCloseSession.setBounds(560, 410, 150, 30);
            btnCloseSession.addActionListener(e -> dispose());
            add(btnCloseSession);

            setSize(window_w, window_h);
            setLocationRelativeTo(parent);
            updateCountersAndAlerts();

            setVisible(true);
        }

        private void saveManualChanges() {
            if (attendanceTable.isEditing()) {
                attendanceTable.getCellEditor().stopCellEditing();
            }
            try {
                // 1) Collect included students from the checkbox column
                List<Student> includedStudents = new ArrayList<>();
                for (int i = 0; i < attendanceModel.getRowCount(); i++) {
                    Boolean included = (Boolean) attendanceModel.getValueAt(i, 0);
                    if (included != null && included) {
                        String studentId = (String) attendanceModel.getValueAt(i, 1);
                        StudentManager.findById(studentId).ifPresent(includedStudents::add);
                    }
                }

                // 2) Persist roster inclusion/exclusion
                SessionManager sessionManager = new SessionManager();
                sessionManager.updateRoster(session.getId(), includedStudents);

                // 3) Update attendance records only if status or notes changed
                ManualMarker manualMarker = new ManualMarker();
                for (int i = 0; i < attendanceModel.getRowCount(); i++) {
                    Boolean included = (Boolean) attendanceModel.getValueAt(i, 0);
                    if (included == null || !included)
                        continue;

                    String studentId = (String) attendanceModel.getValueAt(i, 1);
                    String statusStr = (String) attendanceModel.getValueAt(i, 3);
                    String notesStr = (String) attendanceModel.getValueAt(i, 6);

                    AttendanceRecord.Status newStatus;
                    try {
                        newStatus = AttendanceRecord.Status.valueOf(statusStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        newStatus = AttendanceRecord.Status.PENDING;
                    }

                    Optional<AttendanceRecord> existingOpt = AttendanceManager
                            .findBySessionAndStudentId(session.getId(), studentId);

                    if (existingOpt.isPresent()) {
                        AttendanceRecord current = existingOpt.get();
                        AttendanceRecord.Status oldStatus = current.getStatus();

                        boolean statusChanged = !oldStatus.equals(newStatus);
                        boolean notesChanged = !Objects.equals(current.getNotes().orElse(""), notesStr);

                        if (statusChanged || notesChanged) {
                            // Use ManualMarker to mark attendance with proper status override logic
                            String finalNotes = notesStr;
                            String currentEmail = AuthManager.getCurrentUser().getEmail();
                            String markedByPattern = "\\(marked by [^)]+\\)";
                            String oldNotes = current.getNotes().orElse("");

                            // Strip any marked by entry from notesStr (user input)
                            String strippedNotes = notesStr.replaceAll("\\s*" + markedByPattern, "").trim();

                            // Extract marked by entry from old notes if it exists
                            java.util.regex.Pattern p = java.util.regex.Pattern.compile(markedByPattern);
                            java.util.regex.Matcher m = p.matcher(oldNotes);
                            String existingMarkedBy = m.find() ? m.group() : null;

                            // Update marked by entry if status or notes changed
                            if (existingMarkedBy != null && existingMarkedBy.contains(currentEmail)) {
                                // Same user, keep stripped notes as is
                                finalNotes = strippedNotes;
                            } else {
                                // Different user or no existing entry, add/overwrite
                                finalNotes = strippedNotes + (strippedNotes.isEmpty() ? "" : " ") + "(marked by "
                                        + currentEmail + ")";
                            }

                            MarkingRequest request = MarkingRequest.builder(session.getId(), studentId)
                                    .desiredStatus(newStatus)
                                    .markedAt(statusChanged ? Instant.now() : current.getMarkedAt().orElse(null))
                                    .notes(finalNotes)
                                    .build();
                            manualMarker.markAttendance(request);
                        }
                    }

                }

                // 4) Refresh table with latest records
                List<Student> allStudents = StudentManager.findAll();
                List<AttendanceRecord> updatedRecords = AttendanceManager.findBySession(session);
                attendanceModel.setRowCount(0);
                for (Object[] row : buildAttendanceTableRows(allStudents, updatedRecords)) {
                    attendanceModel.addRow(row);
                }

                updateCountersAndAlerts();
                JOptionPane.showMessageDialog(this, "Changes saved successfully.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to save attendance: " + ex.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        private void updateCountersAndAlerts() {
            int total = 0; // count only included students
            int present = 0, late = 0;
            lateAlerts.clear();

            for (int i = 0; i < attendanceModel.getRowCount(); i++) {
                Boolean included = (Boolean) attendanceModel.getValueAt(i, 0);
                if (included == null || !included) {
                    continue; // skip students not included
                }

                total++; // only count included students

                String name = (String) attendanceModel.getValueAt(i, 2);
                String status = ((String) attendanceModel.getValueAt(i, 3)).toLowerCase();

                if (status.equals("present") || status.equals("late"))
                    present++;
                if (status.equals("late")) {
                    late++;
                    lateAlerts.add(name);
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Present: ").append(present).append("/").append(total);
            sb.append(" | Late: ").append(late);
            if (!lateAlerts.isEmpty()) {
                sb.append(" | Alerts: Late - ").append(String.join(", ", lateAlerts));
            }

            lblCounters.setText(sb.toString());
        }

        private List<Object[]> buildAttendanceTableRows(List<Student> allStudents, List<AttendanceRecord> records) {
            Map<String, AttendanceRecord> recordMap = new HashMap<>();
            for (AttendanceRecord record : records) {
                recordMap.put(record.getStudent().getId(), record);
            }

            List<Object[]> allRows = new ArrayList<>();

            for (Student student : allStudents) {
                AttendanceRecord record = recordMap.get(student.getId());

                boolean included = (record != null);
                String status = (record != null) ? record.getStatus().name() : "-";
                String notes = (record != null && record.getNotes().isPresent()) ? record.getNotes().get() : "";
                String timestamp = "";
                if (record != null && record.getMarkedAt().isPresent()) {
                    Instant instant = record.getMarkedAt().get();
                    java.time.ZonedDateTime localTime = instant.atZone(java.time.ZoneId.systemDefault());
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                            .ofPattern("dd MMM yyyy, hh:mm a");
                    timestamp = localTime.format(formatter);
                }
                String method = (record != null && record.getMethod().isPresent())
                        ? record.getMethod().get().name()
                        : "-";

                allRows.add(new Object[] {
                        included,
                        student.getId(),
                        student.getName(),
                        status,
                        timestamp,
                        method,
                        notes
                });
            }

            return allRows;
        }

    }

    // Main method for testing standalone

}
