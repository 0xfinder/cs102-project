package com.group5.smartattendance.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * accountability logging for sensitive operations.
 */
public class AuditLogger {
    private static final Logger logger = LoggerFactory.getLogger("AUDIT");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String currentUser;

    public AuditLogger(String currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Log a session reopening action.
     *
     * @param sessionId   the session being reopened
     * @param sessionName the name of the session
     */
    public void logSessionReopened(String sessionId, String sessionName) {
        String timestamp = LocalDateTime.now().format(formatter);
        String message = String.format("AUDIT: Session reopened - ID: %s, Name: '%s', User: %s, Timestamp: %s",
                sessionId, sessionName, currentUser, timestamp);
        logger.info(message);
    }

    /**
     * Log a session archival action.
     *
     * @param sessionId   the session being archived
     * @param sessionName the name of the session
     */
    public void logSessionArchived(String sessionId, String sessionName) {
        String timestamp = LocalDateTime.now().format(formatter);
        String message = String.format("AUDIT: Session archived - ID: %s, Name: '%s', User: %s, Timestamp: %s",
                sessionId, sessionName, currentUser, timestamp);
        logger.info(message);
    }

    /**
     * Log a general audit action.
     *
     * @param action  description of the action
     * @param details additional details about what changed
     */
    public void logAction(String action, String details) {
        String timestamp = LocalDateTime.now().format(formatter);
        String message = String.format("AUDIT: %s - User: %s, Details: %s, Timestamp: %s",
                action, currentUser, details, timestamp);
        logger.info(message);
    }

    public void setCurrentUser(String user) {
        this.currentUser = user;
    }

    public String getCurrentUser() {
        return currentUser;
    }
}
