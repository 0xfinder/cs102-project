package com.group5.smartattendance.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.group5.smartattendance.gui.AlertBoxView;
import com.group5.smartattendance.persistence.StudentManager;
import com.group5.smartattendance.student.Student;

public class RegisterStudentView extends JFrame {
    // open new alertBoxView window
    // Student has been registered with SID: ...
    // close alertBoxView and currentView
    // Button Element
    final int window_w = 640;
    final int window_h = 540;

    final int label_w = 120;
    final int label_h = 40;

    final int textField_w = 360;
    final int textField_h = 40;

    final int margin_x = 80;
    final int margin_y = 100;
    final int gapSize = 10;

    private JLabel headerText;

    private JLabel stdNameLabel;
    private JLabel stdClassGroupLabel;
    private JLabel stdEmailLabel;
    private JLabel stdPhoneLabel;
    private JLabel stdDateLabel;

    private JTextField stdNameField;
    private JTextField stdClassGroupField;
    private JTextField stdEmailField;
    private JTextField stdPhoneField;
    private JLabel stdDateDisplayField; // Date will always show today

    private JButton btnRegister;

    public RegisterStudentView() {
        // Designing UI
        setLayout(null);

        // JFrame Title
        this.setTitle("Register New Student");

        // UI Elements
        btnRegister = new JButton("capture");
        btnRegister.setBounds((window_w - 80) / 2, window_h - 120, 80, 40);
        add(btnRegister);

        // row 1
        stdNameLabel = new JLabel("Name: ");
        stdNameLabel.setBounds(margin_x, margin_y, label_w, label_h);
        add(stdNameLabel);

        stdNameField = new JTextField("");
        stdNameField.setBounds(window_w - margin_x - textField_w, margin_y, textField_w, textField_h);
        add(stdNameField);

        // row 2
        stdClassGroupLabel = new JLabel("Class Group: ");
        stdClassGroupLabel.setBounds(margin_x, margin_y + 1 * (label_h + gapSize), label_w, label_h);
        add(stdClassGroupLabel);

        stdClassGroupField = new JTextField("");
        stdClassGroupField.setBounds(window_w - margin_x - textField_w, margin_y + 1 * (label_h + gapSize), textField_w,
                textField_h);
        add(stdClassGroupField);

        // row 3
        stdEmailLabel = new JLabel("Email Address: ");
        stdEmailLabel.setBounds(margin_x, margin_y + 2 * (label_h + gapSize), label_w, label_h);
        add(stdEmailLabel);

        stdEmailField = new JTextField("");
        stdEmailField.setBounds(window_w - margin_x - textField_w, margin_y + 2 * (label_h + gapSize), textField_w,
                textField_h);
        add(stdEmailField);

        // row 4
        stdPhoneLabel = new JLabel("Phone Number: ");
        stdPhoneLabel.setBounds(margin_x, margin_y + 3 * (label_h + gapSize), label_w, label_h);
        add(stdPhoneLabel);

        stdPhoneField = new JTextField("");
        stdPhoneField.setBounds(window_w - margin_x - textField_w, margin_y + 3 * (label_h + gapSize), textField_w,
                textField_h);
        add(stdPhoneField);

        // row 5
        stdDateLabel = new JLabel("Date: ");
        stdDateLabel.setBounds(margin_x, margin_y + 4 * (label_h + gapSize), label_w, label_h);
        add(stdDateLabel);

        stdDateDisplayField = new JLabel(LocalDate.now().toString());
        stdDateDisplayField.setBounds(window_w - margin_x - textField_w, margin_y + 4 * (label_h + gapSize),
                textField_w,
                textField_h);
        add(stdDateDisplayField);

        // Header Text
        headerText = new JLabel("Student Attendence System", SwingConstants.CENTER);
        final int headerMargin_t = 20;
        headerText.setBounds(0, headerMargin_t, window_w, 40);
        headerText.setFont(new Font("Dialog", Font.PLAIN, 24));
        add(headerText);

        setSize(new Dimension(window_w, window_h)); // w, h
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        buttonEvents();
    }

    private void buttonEvents() {
        btnRegister.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Student student = registerStudent();
                if (!student.getId().isBlank()) {
                    new AlertBoxView("Student registered with SID: " + student.getId(),
                            "Student Successfully Registered");
                    dispose();
                } else {
                    new AlertBoxView("Error registering student", "Student Unsuccessfully Registered");
                }
            }
        });
    }

    private Student registerStudent() {
        // add to db
        String stdName = stdNameField.getText();
        String stdClassGrp = stdClassGroupField.getText();
        String stdEmail = stdEmailField.getText();
        String stdPhone = stdPhoneField.getText();
        LocalDate date = LocalDate.now();
        Instant stdDate = date.atStartOfDay(ZoneId.systemDefault()).toInstant();

        try {
            return StudentManager.save(stdName, stdClassGrp, stdEmail, stdPhone, stdDate);
        } catch (Exception e) {
            return null;
        }
    }
}
