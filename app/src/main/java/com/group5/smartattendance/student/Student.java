package com.group5.smartattendance.student;

import com.group5.smartattendance.core.Entity;

import java.time.Instant;
import java.util.Objects;

public class Student extends Entity {

    private final String name;
    private final String classGroup;
    private final String email;
    private final String phone;
    private final Instant enrollmentDate;

    public Student(String id, String name, String classGroup, String email, String phone, Instant enrollmentDate) {
        super(id);
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.classGroup = classGroup;
        this.email = email;
        this.phone = phone;
        this.enrollmentDate = enrollmentDate != null ? enrollmentDate : Instant.now();
    }

    public Student(String id, String name) {
        this(id, name, null, null, null, null);
    }

    public String getName() {
        return name;
    }

    public String getClassGroup() {
        return classGroup;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Instant getEnrollmentDate() {
        return enrollmentDate;
    }
}
