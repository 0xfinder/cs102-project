// Myat
package com.group5.smartattendance.gui;

import com.group5.smartattendance.persistence.StudentManager;
import com.group5.smartattendance.student.Student;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;

public class StudentView extends JFrame {

    private JLabel headerLabel;
    private JTextArea studentTextArea;
    private JButton btnBack;

    public StudentView() {
        setTitle("Registered Students");
        setLayout(null);

        final int window_w = 600;
        final int window_h = 400;

        headerLabel = new JLabel("Student List", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Dialog", Font.BOLD, 20));
        headerLabel.setBounds(0, 20, window_w, 30);
        add(headerLabel);

        studentTextArea = new JTextArea();
        studentTextArea.setBounds(40, 70, window_w - 80, 220);
        studentTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(studentTextArea);
        scrollPane.setBounds(40, 70, window_w - 80, 220);
        add(scrollPane);

        btnBack = new JButton("Back");
        btnBack.setBounds((window_w - 100) / 2, 310, 100, 30);
        add(btnBack);

        // Load real student data
        loadStudentData();

        btnBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose(); // close window
            }
        });

        setSize(window_w, window_h);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    private void loadStudentData() {
        try {
            List<Student> students = StudentManager.findAll();
            StringBuilder sb = new StringBuilder();
            if (students.isEmpty()) {
                sb.append("No students registered yet.");
            } else {
                for (Student student : students) {
                    sb.append("ID: ").append(student.getId()).append(" - ").append(student.getName());
                    if (student.getClassGroup() != null) {
                        sb.append(" (Class: ").append(student.getClassGroup()).append(")").append("\n");
                    }
                }
            }
            studentTextArea.setText(sb.toString());
        } catch (SQLException e) {
            studentTextArea.setText("Error loading student data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

