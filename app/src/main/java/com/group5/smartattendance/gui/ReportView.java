package com.group5.smartattendance.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;

public class ReportView extends JFrame {

    private JLabel headerLabel;
    private JComboBox<String> sessionSelector;
    private JTable summaryTable;
    private JTable detailTable;
    private JButton btnExport;
    private JButton btnBack;

    public ReportView() {
        setTitle("Attendance Reports");
        setLayout(null);

        final int window_w = 800;
        final int window_h = 650;

        // Header
        headerLabel = new JLabel("Attendance Reports & Export", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        headerLabel.setBounds(0, 20, window_w, 35);
        add(headerLabel);

        // Session selection
        JLabel filterLabel = new JLabel("Select Session:");
        filterLabel.setBounds(50, 70, 100, 25);
        add(filterLabel);

        String[] sessions = { "CS102 (2025-10-15)", "CS203 (2025-10-14)", "All Sessions" };
        sessionSelector = new JComboBox<>(sessions);
        sessionSelector.setBounds(160, 70, 250, 25);
        sessionSelector.addActionListener(this::loadReportData);
        add(sessionSelector);

        // Summary table
        JLabel summaryLabel = new JLabel("Session Summary:");
        summaryLabel.setBounds(50, 110, 200, 25);
        add(summaryLabel);

        String[] summaryColumns = { "Statistic", "Value" };
        DefaultTableModel summaryModel = new DefaultTableModel(null, summaryColumns);
        summaryTable = new JTable(summaryModel);
        JScrollPane summaryScrollPane = new JScrollPane(summaryTable);
        summaryScrollPane.setBounds(50, 140, window_w - 100, 100);
        add(summaryScrollPane);

        // Detail table with Notes column
        JLabel detailLabel = new JLabel("Detailed Records:");
        detailLabel.setBounds(50, 260, 200, 25);
        add(detailLabel);

        String[] detailColumns = { "ID", "Name", "Status", "Time", "Confidence", "Method", "Notes" };
        DefaultTableModel detailModel = new DefaultTableModel(null, detailColumns);
        detailTable = new JTable(detailModel);
        JScrollPane detailScrollPane = new JScrollPane(detailTable);
        detailScrollPane.setBounds(50, 290, window_w - 100, 250);
        add(detailScrollPane);

        // Export and back buttons
        btnExport = new JButton("Export Report...");
        btnExport.setBounds((window_w / 2) - 160, 560, 150, 30);
        btnExport.addActionListener(e -> showExportDialog());
        add(btnExport);

        btnBack = new JButton("Back");
        btnBack.setBounds((window_w / 2) + 10, 560, 100, 30);
        btnBack.addActionListener(e -> dispose());
        add(btnBack);

        // Window setup
        setSize(window_w, window_h);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);

        // Load initial data
        loadReportData(null);
    }

    /**
     * Loads dummy data for selected session into tables.
     */
    private void loadReportData(ActionEvent e) {
        String selectedSession = (String) sessionSelector.getSelectedItem();

        Object[][] summaryData;
        Object[][] detailData;

        switch (selectedSession) {
            case "CS102 (2025-10-15)":
                summaryData = new Object[][] {
                        { "Total Students", 20 },
                        { "Present Count", 16 },
                        { "Absent Count", 4 },
                        { "Present Percentage", "80%" }
                };
                detailData = new Object[][] {
                        { "S001", "Alice Tan", "Present", "10:01:34", "95%", "Auto", "" },
                        { "S002", "Bob Lee", "Late", "10:16:50", "88%", "Auto", "Traffic delay" },
                        { "S003", "Charlie Lin", "Absent", "-", "-", "-", "Sick leave" },
                        { "S004", "Jane Doe", "Present", "10:00:15", "92%", "Auto", "" }
                };
                break;

            case "CS203 (2025-10-14)":
                summaryData = new Object[][] {
                        { "Total Students", 18 },
                        { "Present Count", 15 },
                        { "Absent Count", 3 },
                        { "Present Percentage", "83.3%" }
                };
                detailData = new Object[][] {
                        { "S010", "David Khoo", "Present", "09:59:00", "91%", "Auto", "" },
                        { "S011", "Ella Wong", "Absent", "-", "-", "-", "Medical" },
                        { "S012", "Faisal Ahmad", "Present", "10:00:50", "89%", "Auto", "" },
                        { "S013", "Grace Lim", "Late", "10:17:05", "85%", "Auto", "Overslept" }
                };
                break;

            case "All Sessions":
            default:
                summaryData = new Object[][] {
                        { "Total Students", 38 }, // or calculate dynamically
                        { "Present Count", 31 },
                        { "Absent Count", 7 },
                        { "Present Percentage", "81.6%" }
                };

                detailData = new Object[][] {
                        // From CS102
                        { "S001", "Alice Tan", "Present", "10:01:34", "95%", "Auto", "" },
                        { "S002", "Bob Lee", "Late", "10:16:50", "88%", "Auto", "Traffic delay" },
                        { "S003", "Charlie Lin", "Absent", "-", "-", "-", "Sick leave" },
                        { "S004", "Jane Doe", "Present", "10:00:15", "92%", "Auto", "" },

                        // From CS203
                        { "S010", "David Khoo", "Present", "09:59:00", "91%", "Auto", "" },
                        { "S011", "Ella Wong", "Absent", "-", "-", "-", "Medical" },
                        { "S012", "Faisal Ahmad", "Present", "10:00:50", "89%", "Auto", "" },
                        { "S013", "Grace Lim", "Late", "10:17:05", "85%", "Auto", "Overslept" }
                };
                break;

        }

        // Update summary table
        DefaultTableModel summaryModel = (DefaultTableModel) summaryTable.getModel();
        summaryModel.setRowCount(0);
        for (Object[] row : summaryData) {
            summaryModel.addRow(row);
        }

        // Update detail table
        DefaultTableModel detailModel = (DefaultTableModel) detailTable.getModel();
        detailModel.setRowCount(0);
        for (Object[] row : detailData) {
            detailModel.addRow(row);
        }
    }

    /**
     * Shows export format options and triggers CSV export.
     */
    private void showExportDialog() {
        String[] exportOptions = { "CSV", "XLSX", "PDF" };
        String choice = (String) JOptionPane.showInputDialog(
                this,
                "Choose export format:",
                "Export Report",
                JOptionPane.QUESTION_MESSAGE,
                null,
                exportOptions,
                exportOptions[0]);

        if (choice == null)
            return;

        if (choice.equals("CSV")) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save CSV Report");
            fileChooser.setSelectedFile(new File("attendance_report.csv"));

            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                exportTablesToCSV(fileToSave);
            }
        } else {
            JOptionPane.showMessageDialog(this, choice + " export not implemented yet.");
        }
    }

    /**
     * Exports both summary and detail tables to CSV.
     */
    private void exportTablesToCSV(File file) {
        try (FileWriter writer = new FileWriter(file)) {

            writer.write("=== Session Summary ===\n");
            DefaultTableModel summaryModel = (DefaultTableModel) summaryTable.getModel();
            for (int i = 0; i < summaryModel.getRowCount(); i++) {
                writer.write(summaryModel.getValueAt(i, 0) + "," + summaryModel.getValueAt(i, 1) + "\n");
            }

            writer.write("\n=== Detailed Records ===\n");
            DefaultTableModel detailModel = (DefaultTableModel) detailTable.getModel();

            // Write header
            for (int i = 0; i < detailModel.getColumnCount(); i++) {
                writer.write(detailModel.getColumnName(i));
                if (i < detailModel.getColumnCount() - 1)
                    writer.write(",");
            }
            writer.write("\n");

            // Write rows
            for (int i = 0; i < detailModel.getRowCount(); i++) {
                for (int j = 0; j < detailModel.getColumnCount(); j++) {
                    Object value = detailModel.getValueAt(i, j);
                    writer.write(value != null ? value.toString() : "");
                    if (j < detailModel.getColumnCount() - 1)
                        writer.write(",");
                }
                writer.write("\n");
            }

            writer.flush();
            JOptionPane.showMessageDialog(this, "CSV export successful!", "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
