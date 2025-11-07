
package com.group5.smartattendance.gui;

import com.group5.smartattendance.core.Configuration;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingView extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(SettingView.class);

    private JLabel headerLabel;
    private JTextField dbPathField;
    private JTextField thresholdField;
    private JTextField lateThresholdField;
    private JTextField cooldownField;
    private JTextField cameraIndexField;
    private JButton btnSave;
    private JButton btnCancel;
    private JButton btnBrowse;

    public SettingView() {
        setTitle("Settings");
        setLayout(null);

        final int window_w = 550;
        final int window_h = 380;

        headerLabel = new JLabel("System Settings", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Dialog", Font.BOLD, 20));
        headerLabel.setBounds(0, 20, window_w, 30);
        add(headerLabel);

        JLabel dbPathLabel = new JLabel("Database Path:");
        dbPathLabel.setBounds(50, 70, 150, 25);
        add(dbPathLabel);

        dbPathField = new JTextField();
        dbPathField.setBounds(200, 70, 200, 25);
        add(dbPathField);

        JLabel thresholdLabel = new JLabel("Recognition Threshold:");
        thresholdLabel.setBounds(50, 110, 150, 25);
        add(thresholdLabel);

        thresholdField = new JTextField();
        thresholdField.setBounds(200, 110, 200, 25);
        add(thresholdField);

        JLabel lateThresholdLabel = new JLabel("Late Threshold (minutes):");
        lateThresholdLabel.setBounds(50, 150, 150, 25);
        add(lateThresholdLabel);

        lateThresholdField = new JTextField();
        lateThresholdField.setBounds(200, 150, 200, 25);
        add(lateThresholdField);

        JLabel cooldownLabel = new JLabel("Cooldown Seconds:");
        cooldownLabel.setBounds(50, 190, 150, 25);
        add(cooldownLabel);

        cooldownField = new JTextField();
        cooldownField.setBounds(200, 190, 200, 25);
        add(cooldownField);

        JLabel camLabel = new JLabel("Camera Index:");
        camLabel.setBounds(50, 230, 150, 25);
        add(camLabel);

        cameraIndexField = new JTextField();
        cameraIndexField.setBounds(200, 230, 200, 25);
        add(cameraIndexField);

        // Load current configuration values
        Configuration configuration = Configuration.getInstance();
        dbPathField.setText(configuration.getDbPath());
        thresholdField.setText(String.valueOf(configuration.getRecognitionThreshold()));
        lateThresholdField.setText(String.valueOf(configuration.getLateThresholdMinutes()));
        cooldownField.setText(String.valueOf(configuration.getCooldownSeconds()));
        cameraIndexField.setText(String.valueOf(configuration.getCameraIndex()));

        btnBrowse = new JButton("Browse");
        btnBrowse.setBounds(410, 70, 90, 25);
        add(btnBrowse);

        btnSave = new JButton("Save");
        btnSave.setBounds(120, 280, 100, 30);
        add(btnSave);

        btnCancel = new JButton("Cancel");
        btnCancel.setBounds(280, 280, 100, 30);
        add(btnCancel);

        // BROWSE button logic
        btnBrowse.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select SQLite DB file");
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                dbPathField.setText(selectedFile.getAbsolutePath());
            }
        });

        // SAVE button logic
        btnSave.addActionListener(e -> {
            try {
                String dbPath = dbPathField.getText().trim();
                double threshold = Double.parseDouble(thresholdField.getText().trim());
                int lateThreshold = Integer.parseInt(lateThresholdField.getText().trim());
                int cooldown = Integer.parseInt(cooldownField.getText().trim());
                int cameraIndex = Integer.parseInt(cameraIndexField.getText().trim());

                Configuration config = Configuration.getInstance();
                config.setDbPath(dbPath);
                config.setRecognitionThreshold(threshold);
                config.setLateThresholdMinutes(lateThreshold);
                config.setCooldownSeconds(cooldown);
                config.setCameraIndex(cameraIndex);
                config.save();

                logger.info("Settings saved: DB Path={}, Threshold={}, Late Threshold={}, Cooldown={}, Camera Index={}",
                        dbPath, threshold, lateThreshold, cooldown, cameraIndex);

                JOptionPane.showMessageDialog(null, "Settings saved successfully!");
                dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null,
                        "Invalid input. Please enter valid numbers for all numeric fields.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                logger.error("Invalid input in settings: {}", ex.getMessage());
            }
        });

        btnCancel.addActionListener(e -> dispose());

        setSize(window_w, window_h);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
    }
}