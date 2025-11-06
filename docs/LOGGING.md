# Logging Recommendations for Smart Attendance System

Log events that track key operations, errors, and system health for debugging, auditing, and monitoring. Based on the project requirements, focus on INFO-level logs for normal activities and ERROR/WARN for issues. Here's what to log:

## Usage Instructions

This project uses SLF4J with Logback for logging. Logs are output to both console (for development) and file.

### Initialization

In each class that needs logging, declare a logger instance at the top:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(ClassName.class);
```

Replace `ClassName` with the actual class name.

### Log Methods

Use the following methods to log at different levels:

- `logger.trace(String message)` - For fine-grained debugging information
- `logger.debug(String message)` - For detailed debugging information
- `logger.info(String message)` - For general information about normal operations
- `logger.warn(String message)` - For potentially harmful situations
- `logger.error(String message)` - For error conditions

### Parameterized Logging

For better performance and to avoid string concatenation, use parameterized logging:

```java
logger.info("Student enrolled: ID={}, name={}", studentId, studentName);
```

### Structured Logging with Context

Include relevant context in logs for better debugging:

```java
logger.info("Attendance marked: studentId={}, status={}, method={}, timestamp={}",
            studentId, status, method, timestamp);
```

### Best Practices

- Use appropriate log levels based on severity
- Avoid logging sensitive information (e.g., passwords, full face images)
- Include timestamps and relevant IDs in logs
- Test logging by running `./gradlew test` to ensure logs appear correctly

## Enrollment & Management

- Student enrolled (ID, name)
- Student deleted (ID)
- Face images captured/imported (student ID, count)
- Validation failures (e.g., poor image quality)

## Sessions

- Session created (name, date, time, roster size)
- Session opened/closed (session ID)
- Roster updates (added/removed students)

## Recognition & Marking

- Face detection started/stopped
- Recognition match (student ID, confidence score)
- Attendance marked (student ID, status: present/late/absent, method: auto/manual, timestamp)
- Low confidence prompts (user confirmations)
- Duplicates prevented (cooldown triggered)
- Unrecognized faces (with optional details)

## Errors & Issues

- Database connection/query failures
- Camera access problems (index invalid, no camera)
- OpenCV/JavaCV errors (detection/recognition failures)
- Configuration load errors
- File I/O issues (saving reports, logs)

## General

- Application startup/shutdown
- Settings changes (if implemented)
- Report generation/export (file type, records exported)

Use structured logs with context (e.g., timestamps, user IDs) and avoid sensitive data. Log to file via Logback and console for development. This provides an audit trail and helps troubleshoot issues.
