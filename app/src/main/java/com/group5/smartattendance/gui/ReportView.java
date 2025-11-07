package com.group5.smartattendance.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.group5.smartattendance.session.Session;
import com.group5.smartattendance.session.SessionManager;
import com.group5.smartattendance.student.Student;
import com.group5.smartattendance.marker.AttendanceManager;
import com.group5.smartattendance.marker.AttendanceRecord;

public class ReportView extends JFrame {

    private JLabel headerLabel;
    private JComboBox<Session> sessionSelector;
    private JTable summaryTable;
    private JTable detailTable;
    private JButton btnExport;
    private JButton btnBack;
    private SessionManager sessionManager;

    public ReportView() {
        setTitle("Attendance Reports");
        setLayout(null);

        final int window_w = 1000;
        final int window_h = 700;

        sessionManager = new SessionManager();

        // Header
        headerLabel = new JLabel("Attendance Reports & Export", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        headerLabel.setBounds(0, 20, window_w, 35);
        add(headerLabel);

        // Session selection
        JLabel filterLabel = new JLabel("Select Session:");
        filterLabel.setBounds(50, 70, 120, 25);
        add(filterLabel);

        sessionSelector = new JComboBox<>();
        sessionSelector.setBounds(180, 70, 300, 25);
        add(sessionSelector);

        // Load closed sessions
        loadClosedSessions();

        sessionSelector.addActionListener(e -> loadReportData());

        // Summary table
        JLabel summaryLabel = new JLabel("Session Summary:");
        summaryLabel.setBounds(50, 110, 200, 25);
        add(summaryLabel);

        DefaultTableModel summaryModel = new DefaultTableModel(0, 2);
        summaryTable = new JTable(summaryModel);
        summaryTable.setTableHeader(null);
        JScrollPane summaryScrollPane = new JScrollPane(summaryTable);
        summaryScrollPane.setBounds(50, 140, window_w - 100, 85);
        add(summaryScrollPane);

        // Detail table
        JLabel detailLabel = new JLabel("Detailed Records:");
        detailLabel.setBounds(50, 230, 200, 25);
        add(detailLabel);

        String[] detailColumns = { "ID", "Name", "Status", "Time", "Confidence", "Method", "Notes" };
        DefaultTableModel detailModel = new DefaultTableModel(null, detailColumns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // all cells non-editable
            }
        };

        detailTable = new JTable(detailModel);
        detailTable.setModel(detailModel);
        JScrollPane detailScrollPane = new JScrollPane(detailTable);
        detailScrollPane.setBounds(50, 260, window_w - 100, 300);
        add(detailScrollPane);

        // Export and Back buttons
        btnExport = new JButton("Export CSV...");
        btnExport.setBounds((window_w / 2) - 75, 580, 150, 30);
        btnExport.addActionListener(e -> exportCSV());
        add(btnExport);

        btnBack = new JButton("Back");
        btnBack.setBounds((window_w / 2) + 90, 580, 100, 30);
        btnBack.addActionListener(e -> dispose());
        add(btnBack);

        // Window setup
        setSize(window_w, window_h);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);

        // Initial load
        loadReportData();
    }

    private void loadClosedSessions() {
        List<Session> closedSessions = sessionManager.listSessions().stream()
                .filter(s -> s.getStatus() == Session.Status.CLOSED)
                .toList();
        DefaultComboBoxModel<Session> model = new DefaultComboBoxModel<>();
        for (Session s : closedSessions) {
            model.addElement(s);
        }
        sessionSelector.setModel(model);
        sessionSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                if (value instanceof Session session) {
                    value = session.getCourseName() + " (" + session.getSessionDate() + ")";
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
    }

    private void loadReportData() {
        Session session = (Session) sessionSelector.getSelectedItem();
        if (session == null)
            return;

        try {
            List<Student> allStudents = session.getRoster().getStudents();
            List<AttendanceRecord> records = AttendanceManager.findBySession(session);

            // Summary table
            int total = allStudents.size();
            long present = records.stream()
                    .filter(r -> r.getStatus() == AttendanceRecord.Status.PRESENT
                            || r.getStatus() == AttendanceRecord.Status.LATE)
                    .count();
            long late = records.stream()
                    .filter(r -> r.getStatus() == AttendanceRecord.Status.LATE)
                    .count();
            long absent = total - present;
            String presentPct = total > 0 ? String.format("%.1f%%", present * 100.0 / total) : "-";

            DefaultTableModel summaryModel = new DefaultTableModel(0, 2) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // make all cells non-editable
                }
            };
            summaryTable.setModel(summaryModel);
            summaryModel.setRowCount(0);
            summaryModel.addRow(new Object[] { "Total Students", total });
            summaryModel.addRow(new Object[] { "Present Count", present });
            summaryModel.addRow(new Object[] { "Late Count", late });
            summaryModel.addRow(new Object[] { "Absent Count", absent });
            summaryModel.addRow(new Object[] { "Present Percentage", presentPct });

            // Detail table
            DefaultTableModel detailModel = (DefaultTableModel) detailTable.getModel();
            detailModel.setRowCount(0);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
                    .withZone(ZoneId.systemDefault());

            for (AttendanceRecord r : records) {
                String confidenceStr = r.getConfidence() >= 0 ? r.getConfidence() + "%" : "-";
                String timestamp = r.getMarkedAt().map(instant -> fmt.format(instant)).orElse("-");

                detailModel.addRow(new Object[] {
                        r.getStudent().getId(),
                        r.getStudent().getName(),
                        r.getStatus().name(),
                        timestamp,
                        confidenceStr,
                        r.getMethod().map(Object::toString).orElse("-"),
                        r.getNotes().orElse("")
                });
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load report: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save CSV Report");
        fileChooser.setSelectedFile(new File("attendance_report.csv"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION)
            return;

        File file = fileChooser.getSelectedFile();
        try (FileWriter writer = new FileWriter(file)) {
            // Summary
            writer.write("=== Session Summary ===\n");
            DefaultTableModel summaryModel = (DefaultTableModel) summaryTable.getModel();
            for (int i = 0; i < summaryModel.getRowCount(); i++) {
                writer.write(summaryModel.getValueAt(i, 0) + "," + summaryModel.getValueAt(i, 1) + "\n");
            }

            // Detail
            writer.write("\n=== Detailed Records ===\n");
            DefaultTableModel detailModel = (DefaultTableModel) detailTable.getModel();
            for (int i = 0; i < detailModel.getColumnCount(); i++) {
                writer.write(detailModel.getColumnName(i));
                if (i < detailModel.getColumnCount() - 1)
                    writer.write(",");
            }
            writer.write("\n");

            for (int i = 0; i < detailModel.getRowCount(); i++) {
                for (int j = 0; j < detailModel.getColumnCount(); j++) {
                    Object value = detailModel.getValueAt(i, j);
                    writer.write("\"" + (value != null ? value.toString() : "") + "\"");
                    if (j < detailModel.getColumnCount() - 1)
                        writer.write(",");
                }
                writer.write("\n");
            }

            JOptionPane.showMessageDialog(this, "CSV export successful!", "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
