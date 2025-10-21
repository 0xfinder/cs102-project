package com.group5.smartattendance.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.group5.smartattendance.session.Roster;
import com.group5.smartattendance.session.Session;
import com.group5.smartattendance.session.SessionManager;
import com.group5.smartattendance.student.Student;

public class SessionView extends JFrame {

    private JTable sessionTable;
    private DefaultTableModel sessionModel;
    private JButton btnEdit, btnNewSession, btnDelete;
    private SessionManager sessionManager;
    private List<Session> sessions = new ArrayList<>();

    public SessionView() {
        // initialize session manager
        sessionManager = new SessionManager();

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

        // Load sessions from persistence
        loadSessions();

        // Edit button
        btnEdit = new JButton("Edit Session");
        btnEdit.setBounds(100, 350, 150, 30);
        btnEdit.addActionListener(e -> openSelectedSession());
        add(btnEdit);

        // Add new session
        btnNewSession = new JButton("New Session");
        btnNewSession.setBounds(300, 350, 150, 30);
        btnNewSession.addActionListener(e -> createNewSession());
        add(btnNewSession);

        // Delete session
        btnDelete = new JButton("Delete Session");
        btnDelete.setBounds(500, 350, 150, 30);
        btnDelete.addActionListener(e -> deleteSelectedSession());
        add(btnDelete);

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

        List<Object[]> rosterRows;
        try {
            Roster roster = sessionManager.loadRoster(session.getId());
            rosterRows = toRosterRows(roster);
        } catch (SessionManager.SessionManagerException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load roster: " + ex.getMessage(),
                    "Session Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new SessionDetailDialog(this, session, rosterRows);
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
        courseNameField.setText("Enter course name");
        sessionDateField.setText("Enter session date (YYYY-MM-DD)");
        startTimeField.setText("Enter start time (HH:MM)");
        endTimeField.setText("Enter end time (HH:MM)");
        locationField.setText("Enter location (optional)");

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
                if (courseName == null || courseName.trim().isEmpty()) {
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

            location = (location != null && !location.trim().isEmpty()) ? location.trim() : "";
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
        private JLabel lblHeader, lblCounters;
        private JTable attendanceTable;
        private DefaultTableModel attendanceModel;
        private JButton btnSaveChanges, btnCloseSession;
        private final List<String> lateAlerts = new ArrayList<>();

        public SessionDetailDialog(JFrame parent, Session session, List<Object[]> rosterData) {
            super(parent, "Session Detail - " + session.getCourseName(), true);
            setLayout(null);

            final int window_w = 700;
            final int window_h = 500;

            lblHeader = new JLabel(
                    "Session: " + session.getCourseName() + " - " + session.getSessionDate(),
                    SwingConstants.CENTER);
            lblHeader.setFont(new Font("Dialog", Font.BOLD, 18));
            lblHeader.setBounds(0, 10, window_w, 25);
            add(lblHeader);

            String[] columns = { "ID", "Name", "Status", "Timestamp", "Method" };
            attendanceModel = new DefaultTableModel(rosterData.toArray(new Object[0][]), columns) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    // Only allow editing on Status, Timestamp, and Method columns
                    return col >= 2;
                }
            };

            attendanceTable = new JTable(attendanceModel);
            JScrollPane scrollPane = new JScrollPane(attendanceTable);
            scrollPane.setBounds(20, 50, window_w - 40, 300);
            add(scrollPane);

            lblCounters = new JLabel("", SwingConstants.LEFT);
            lblCounters.setFont(new Font("Dialog", Font.PLAIN, 14));
            lblCounters.setBounds(20, 360, window_w - 40, 25);
            add(lblCounters);

            btnSaveChanges = new JButton("Save Manual Changes");
            btnSaveChanges.setBounds(150, 400, 180, 30);
            btnSaveChanges.addActionListener(e -> {
                saveManualChanges();
                updateCountersAndAlerts();
            });
            add(btnSaveChanges);

            btnCloseSession = new JButton("Close Session");
            btnCloseSession.setBounds(360, 400, 150, 30);
            btnCloseSession.addActionListener(e -> dispose());
            add(btnCloseSession);

            setSize(window_w, window_h);
            setLocationRelativeTo(parent);
            updateCountersAndAlerts();
            setVisible(true);
        }

        private void saveManualChanges() {
            System.out.println("Saving manual changes:");
            for (int i = 0; i < attendanceModel.getRowCount(); i++) {
                String id = (String) attendanceModel.getValueAt(i, 0);
                String status = (String) attendanceModel.getValueAt(i, 2);
                String timestamp = (String) attendanceModel.getValueAt(i, 3);
                String method = (String) attendanceModel.getValueAt(i, 4);
                System.out.println(id + ": " + status + " at " + timestamp + " via " + method);
            }
            JOptionPane.showMessageDialog(this, "Changes saved!");
        }

        private void updateCountersAndAlerts() {
            int total = attendanceModel.getRowCount();
            int present = 0, late = 0;
            lateAlerts.clear();

            for (int i = 0; i < total; i++) {
                String name = (String) attendanceModel.getValueAt(i, 1);
                String status = ((String) attendanceModel.getValueAt(i, 2)).toLowerCase();

                if (status.equals("present"))
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
    }

    // Main method for testing standalone

}
