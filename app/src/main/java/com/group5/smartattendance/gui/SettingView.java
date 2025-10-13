// Myat
package com.group5.smartattendance.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SettingView extends JFrame {

    private JLabel headerLabel;
    private JTextField cameraIndexField;
    private JTextField thresholdField;
    private JTextField dbPathField;
    private JButton btnSave;
    private JButton btnCancel;

    public SettingView() {
        setTitle("Settings");
        setLayout(null);

        final int window_w = 500;
        final int window_h = 300;

        headerLabel = new JLabel("System Settings", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Dialog", Font.BOLD, 20));
        headerLabel.setBounds(0, 20, window_w, 30);
        add(headerLabel);

        JLabel camLabel = new JLabel("Camera Index:");
        camLabel.setBounds(50, 70, 150, 25);
        add(camLabel);

        cameraIndexField = new JTextField("0"); // Default camera
        cameraIndexField.setBounds(200, 70, 200, 25);
        add(cameraIndexField);

        JLabel thresholdLabel = new JLabel("Recognition Threshold:");
        thresholdLabel.setBounds(50, 110, 150, 25);
        add(thresholdLabel);

        thresholdField = new JTextField("0.7");
        thresholdField.setBounds(200, 110, 200, 25);
        add(thresholdField);

        JLabel dbPathLabel = new JLabel("Database Path:");
        dbPathLabel.setBounds(50, 150, 150, 25);
        add(dbPathLabel);

        dbPathField = new JTextField("data/database.db");
        dbPathField.setBounds(200, 150, 200, 25);
        add(dbPathField);

        btnSave = new JButton("Save");
        btnSave.setBounds(120, 200, 100, 30);
        add(btnSave);

        btnCancel = new JButton("Cancel");
        btnCancel.setBounds(280, 200, 100, 30);
        add(btnCancel);

        // Save settings (you can connect to a Configuration class later)
        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String cameraIndex = cameraIndexField.getText();
                String threshold = thresholdField.getText();
                String dbPath = dbPathField.getText();

                // Here you'd update your Configuration singleton or write to file
                System.out.println("Saving Settings:");
                System.out.println("Camera Index: " + cameraIndex);
                System.out.println("Threshold: " + threshold);
                System.out.println("DB Path: " + dbPath);

                JOptionPane.showMessageDialog(null, "Settings saved successfully!");
                dispose();
            }
        });

        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose(); // Close window
            }
        });

        setSize(window_w, window_h);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
    }
}