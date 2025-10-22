// Myat
package com.group5.smartattendance.gui;

import com.group5.smartattendance.persistence.StudentManager;
import com.group5.smartattendance.session.Session;
import com.group5.smartattendance.session.SessionManager;
import com.group5.smartattendance.student.Student;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentViewV2 extends JFrame {

    private JLabel headerLabel;

    private JTable studentTable;
    private DefaultTableModel studentModel;
    private JButton btnEdit, btnDelete, btnBack;
    private List<Student> students = new ArrayList<>();

    public StudentViewV2() {
        final int window_w = 800;
        final int window_h = 500;

        setTitle("Registered Students");
        setLayout(null);
        setSize(window_w, window_h);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        headerLabel = new JLabel("Student List", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Dialog", Font.BOLD, 22));
        headerLabel.setBounds(0, 20, 800, 30);
        add(headerLabel);

        // Table setup
        String[] columns = { "SID", "Name", "Class/Group", "Email", "Phone No.", "Enrollment Date" };
        studentModel = new DefaultTableModel(null, columns) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        studentTable = new JTable(studentModel);
        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setBounds(50, 70, 700, 250);
        add(scrollPane);

        // Load student data
        loadStudentData();

        // Edit button
        btnEdit = new JButton("Edit Student");
        btnEdit.setBounds(55, 350, 150, 30);
        btnEdit.addActionListener(e -> editStudent());
        add(btnEdit);

        // Delete student
        btnDelete = new JButton("Delete Student");
        btnDelete.setBounds(415, 350, 150, 30);
        btnDelete.addActionListener(e -> deleteStudent());
        add(btnDelete);

        btnBack = new JButton("Back");
        btnBack.setBounds(595, 350, 150, 30);
        btnBack.addActionListener(e -> dispose());
        add(btnBack);

        setVisible(true);
    }

    private void loadStudentData() {
        try {
            students = StudentManager.findAll();
            studentModel.setRowCount(0);
            for (Student student : students) {
                studentModel.addRow(new Object[] {
                        student.getId(),
                        student.getName(),
                        student.getClassGroup(),
                        student.getEmail(),
                        student.getPhone(),
                        student.getEnrollmentDate().toString()
                });
            }
        } catch (SQLException e) {
            new AlertBoxView("Error loading student data: " + e.getMessage(), "Error loading student data");
            e.printStackTrace();
        }
    }

    private void editStudent() {
        System.out.println("edit student");
    }

    private void deleteStudent() {
        System.out.println("delete student");
    }
}
