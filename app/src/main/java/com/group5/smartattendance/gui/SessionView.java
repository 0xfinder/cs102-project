package com.group5.smartattendance.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SessionView extends JFrame {

    private JTable sessionTable;
    private DefaultTableModel sessionModel;
    private JButton btnEdit, btnNewSession, btnDelete;
    private List<SessionData> sessions = new ArrayList<>();

    public SessionView() {
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
        String[] columns = { "Course", "Date", "Start Time", "End Time", "Status" };
        sessionModel = new DefaultTableModel(null, columns) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        sessionTable = new JTable(sessionModel);
        JScrollPane scrollPane = new JScrollPane(sessionTable);
        scrollPane.setBounds(50, 70, 700, 250);
        add(scrollPane);

        // Load dummy data
        loadDummySessions();

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

    private void loadDummySessions() {
        sessions.clear();

        sessions.add(new SessionData("CS102", LocalDate.now().toString(), "10:00", "11:00", true));
        sessions.add(new SessionData("CS203", LocalDate.now().minusDays(1).toString(), "09:00", "10:00", false));

        sessionModel.setRowCount(0);
        for (SessionData session : sessions) {
            sessionModel.addRow(new Object[] {
                    session.courseName,
                    session.date,
                    session.startTime,
                    session.endTime,
                    session.isOpen ? "Open" : "Closed"
            });
        }
    }

    private void openSelectedSession() {
        int selected = sessionTable.getSelectedRow();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this, "Please select a session to edit.");
            return;
        }

        SessionData session = sessions.get(selected);

        // Hardcoded roster data for demo
        List<Object[]> roster = new ArrayList<>();
        roster.add(new Object[] { "S001", "Alice Tan", "Present", "10:01:34", "Auto" });
        roster.add(new Object[] { "S002", "Bob Lee", "Late", "10:16:50", "Auto" });
        roster.add(new Object[] { "S003", "Charlie Lin", "Absent", "-", "-" });
        roster.add(new Object[] { "S004", "Jane Doe", "Present", "10:00:15", "Auto" });

        new SessionDetailDialog(this, session.courseName, roster);
    }

    private void createNewSession() {
        String course = JOptionPane.showInputDialog(this, "Enter course name:");
        if (course == null || course.trim().isEmpty())
            return;

        SessionData newSession = new SessionData(course, LocalDate.now().toString(), "10:00", "11:00", true);
        sessions.add(newSession);
        sessionModel.addRow(new Object[] {
                newSession.courseName,
                newSession.date,
                newSession.startTime,
                newSession.endTime,
                "Open"
        });
    }

    private void deleteSelectedSession() {
        int selected = sessionTable.getSelectedRow();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this, "Select a session to delete.");
            return;
        }

        SessionData session = sessions.get(selected);
        if (session.isOpen) {
            JOptionPane.showMessageDialog(this, "Cannot delete an active (open) session.");
            return;
        }

        sessions.remove(selected);
        sessionModel.removeRow(selected);
    }

    // Inner class to simulate session data
    private static class SessionData {
        String courseName, date, startTime, endTime;
        boolean isOpen;

        public SessionData(String courseName, String date, String startTime, String endTime, boolean isOpen) {
            this.courseName = courseName;
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isOpen = isOpen;
        }
    }

    // Inner dialog class for editing a single session's attendance roster
    private static class SessionDetailDialog extends JDialog {
        private JLabel lblHeader, lblCounters;
        private JTable attendanceTable;
        private DefaultTableModel attendanceModel;
        private JButton btnSaveChanges, btnCloseSession;
        private final List<String> lateAlerts = new ArrayList<>();

        public SessionDetailDialog(JFrame parent, String courseName, List<Object[]> rosterData) {
            super(parent, "Session Detail - " + courseName, true);
            setLayout(null);

            final int window_w = 700;
            final int window_h = 500;

            lblHeader = new JLabel("Session: " + courseName + " - " + LocalDate.now(), SwingConstants.CENTER);
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
