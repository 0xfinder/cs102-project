package com.group5.smartattendance.gui;

import com.group5.smartattendance.persistence.StudentManager;
import com.group5.smartattendance.student.Student;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class StudentView extends JFrame {

    private JTable studentTable;
    private DefaultTableModel studentModel;


    Instant now = Instant.now();
    String formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(now);

    public StudentView() {
        setTitle("Student List");
        setLayout(null);
        setSize(800, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel lblHeader = new JLabel("Student List", SwingConstants.CENTER);
        lblHeader.setFont(new Font("Dialog", Font.BOLD, 22));
        lblHeader.setBounds(0, 20, 800, 30);
        add(lblHeader);

        // Table
        String[] columns = { "Student ID", "Name", "Class Group", "Email", "Phone","Date Registered"};
        studentModel = new DefaultTableModel(null, columns) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        studentTable = new JTable(studentModel);
        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setBounds(50, 70, 700, 300);
        add(scrollPane);

        // Buttons
        JButton btnAdd = new JButton("Add");
        btnAdd.setBounds(150, 390, 100, 30);
        btnAdd.addActionListener(e -> addStudent());
        add(btnAdd);

        JButton btnEdit = new JButton("Edit");
        btnEdit.setBounds(325, 390, 100, 30);
        btnEdit.addActionListener(e -> editStudent());
        add(btnEdit);

        JButton btnDelete = new JButton("Delete");
        btnDelete.setBounds(500, 390, 100, 30);
        btnDelete.addActionListener(e -> deleteStudent());
        add(btnDelete);

        JButton btnBack = new JButton("Back");
        btnBack.setBounds(325, 440, 100, 30);
        btnBack.addActionListener(e -> dispose());
        add(btnBack);

        loadStudents();
        setVisible(true);
    }

    private void loadStudents() {
        try {
            studentModel.setRowCount(0);
            List<Student> students = StudentManager.findAll();
            for (Student s : students) {
                String formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault())
                        .format(s.getEnrollmentDate());
    
                studentModel.addRow(new Object[] {
                    s.getId(),
                    s.getName(),
                    s.getClassGroup(),
                    s.getEmail(),
                    s.getPhone(),
                    formattedDate // show registration date
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void addStudent() {
        Student student = showStudentDialog(null);
        if (student != null) {
            try {
                Student inserted = StudentManager.insert(
                    student.getName(),
                    student.getClassGroup(),
                    student.getEmail(),
                    student.getPhone(),
                    student.getEnrollmentDate()
                );
                loadStudents();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Insert failed: " + e.getMessage());
            }
        }
    }

    private void editStudent() {
        int row = studentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a student to edit.");
            return;
        }

        String id = studentModel.getValueAt(row, 0).toString();
        String name = studentModel.getValueAt(row, 1).toString();
        String classGroup = studentModel.getValueAt(row, 2).toString();
        String email = studentModel.getValueAt(row, 3).toString();
        String phone = studentModel.getValueAt(row, 4).toString();

        Student updated = showStudentDialog(new Student(id, name, classGroup, email, phone, Instant.now()));
        if (updated != null) {
            try {
                StudentManager.update(updated);
                loadStudents();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Update failed: " + e.getMessage());
            }
        }
    }

    private void deleteStudent() {
        int row = studentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a student to delete.");
            return;
        }

        String id = studentModel.getValueAt(row, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this student?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                StudentManager.delete(id);
                loadStudents();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage());
            }
        }
    }

    private Student showStudentDialog(Student existing) {
        JTextField tfName = new JTextField(existing != null ? existing.getName() : "");
        JTextField tfClass = new JTextField(existing != null ? existing.getClassGroup() : "");
        JTextField tfEmail = new JTextField(existing != null ? existing.getEmail() : "");
        JTextField tfPhone = new JTextField(existing != null ? existing.getPhone() : "");
    
        JLabel lblDate = new JLabel();
        if (existing != null) {
            String formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(existing.getEnrollmentDate());
            lblDate.setText("Registered on: " + formattedDate);
        }
    
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Name:")); panel.add(tfName);
        panel.add(new JLabel("Class Group:")); panel.add(tfClass);
        panel.add(new JLabel("Email:")); panel.add(tfEmail);
        panel.add(new JLabel("Phone:")); panel.add(tfPhone);
        if (existing != null) panel.add(lblDate);
    
        int result = JOptionPane.showConfirmDialog(this, panel,
                existing == null ? "Add Student" : "Edit Student",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    
        if (result == JOptionPane.OK_OPTION) {
            return new Student(
                existing != null ? existing.getId() : UUID.randomUUID().toString(),
                tfName.getText(),
                tfClass.getText(),
                tfEmail.getText(),
                tfPhone.getText(),
                existing != null ? existing.getEnrollmentDate() : Instant.now()
            );
        }
    
        return null;
    }
}





