package com.group5.smartattendance.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Properties;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private static Configuration instance;

    private Properties properties;

    private Configuration() {
        properties = new Properties();
        loadProperties();
    }

    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    private void loadProperties() {
        try (FileInputStream input = new FileInputStream("app/config/application.properties")) {
            properties.load(input);
            logger.info("Loaded properties from app/config/application.properties");
        } catch (FileNotFoundException e) {
            // try loading from resources
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
                if (input != null) {
                    properties.load(input);
                    logger.info("Loaded properties from resources/application.properties");
                } else {
                    // fallback defaults
                    properties.setProperty("log.file", "logs/app.log");
                    properties.setProperty("db.path", "data/attendance.db");
                    properties.setProperty("recognition.threshold", "0.7");
                    properties.setProperty("late.threshold.minutes", "15");
                    properties.setProperty("cooldown.seconds", "10");
                    properties.setProperty("camera.index", "0");
                    logger.info("Using default properties");
                }
            } catch (IOException ex) {
                logger.error("Error loading properties from resources", ex);
            }
        } catch (IOException e) {
            logger.error("Error loading properties from file", e);
        }
    }

    public String getDbPath() {
        return properties.getProperty("db.path");
    }

    public void setDbPath(String dbPath) {
        properties.setProperty("db.path", dbPath);
    }

    public String getLogFile() {
        return properties.getProperty("log.file");
    }

    public void setLogFile(String logFile) {
        properties.setProperty("log.file", logFile);
    }

    public double getRecognitionThreshold() {
        return Double.parseDouble(properties.getProperty("recognition.threshold", "0.7"));
    }

    public void setRecognitionThreshold(double recognitionThreshold) {
        properties.setProperty("recognition.threshold", String.valueOf(recognitionThreshold));
    }

    public int getLateThresholdMinutes() {
        return Integer.parseInt(properties.getProperty("late.threshold.minutes", "15"));
    }

    public void setLateThresholdMinutes(int lateThresholdMinutes) {
        properties.setProperty("late.threshold.minutes", String.valueOf(lateThresholdMinutes));
    }

    public int getCooldownSeconds() {
        return Integer.parseInt(properties.getProperty("cooldown.seconds", "10"));
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        properties.setProperty("cooldown.seconds", String.valueOf(cooldownSeconds));
    }

    public int getCameraIndex() {
        return Integer.parseInt(properties.getProperty("camera.index", "0"));
    }

    public void setCameraIndex(int cameraIndex) {
        properties.setProperty("camera.index", String.valueOf(cameraIndex));
    }

    // method to save changes back to file
    public void save() {
        try {
            Path configPath = Paths.get("app", "config", "application.properties");
            Files.createDirectories(configPath.getParent());
            try (FileOutputStream output = new FileOutputStream(configPath.toString())) {
                properties.store(output, "Smart Attendance System Configuration");
                logger.info("Configuration saved to app/config/application.properties");
            }
        } catch (IOException e) {
            logger.error("Error saving properties to file", e);
        }
    }
}