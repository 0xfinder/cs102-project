package com.group5.smartattendance.persistence;

import com.group5.smartattendance.student.Student;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StudentManager {

    public static Student save(String name, String classGroup, String email, String phone, Instant enrollmentDate) throws SQLException {
        Objects.requireNonNull(name, "name must not be null");
        String sql = "INSERT INTO students (name, class_group, email, phone, enrollment_date) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, classGroup);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, enrollmentDate.toString());
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long generatedId = generatedKeys.getLong(1);
                    return new Student(Long.toString(generatedId), name, classGroup, email, phone, enrollmentDate);
                } else {
                    throw new SQLException("Creating student failed, no ID obtained.");
                }
            }
        }
    }

    public static List<Student> findAll() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT id, name, class_group, email, phone, enrollment_date FROM students";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String classGroup = rs.getString("class_group");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                Instant enrollmentDate = Instant.parse(rs.getString("enrollment_date"));
                students.add(new Student(id, name, classGroup, email, phone, enrollmentDate));
            }
        }
        return students;
    }

    public static Optional<Student> findById(String id) throws SQLException {
        String sql = "SELECT id, name, class_group, email, phone, enrollment_date FROM students WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, Long.parseLong(id));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    String classGroup = rs.getString("class_group");
                    String email = rs.getString("email");
                    String phone = rs.getString("phone");
                    Instant enrollmentDate = Instant.parse(rs.getString("enrollment_date"));
                    return Optional.of(new Student(id, name, classGroup, email, phone, enrollmentDate));
                }
            }
        }
        return Optional.empty();
    }

    public static void update(Student student) throws SQLException {
        String sql = "UPDATE students SET name = ?, class_group = ?, email = ?, phone = ?, enrollment_date = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, student.getName());
            stmt.setString(2, student.getClassGroup());
            stmt.setString(3, student.getEmail());
            stmt.setString(4, student.getPhone());
            stmt.setString(5, student.getEnrollmentDate().toString());
            stmt.setLong(6, Long.parseLong(student.getId()));
            stmt.executeUpdate();
        }
    }

    public static void delete(String id) throws SQLException {
        String sql = "DELETE FROM students WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, Long.parseLong(id));
            stmt.executeUpdate();
        }
    }
}
