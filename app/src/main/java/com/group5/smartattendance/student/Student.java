package com.group5.smartattendance.student;

import com.group5.smartattendance.core.Entity;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;

public class Student extends Entity {

    private final String name;
    private final String classGroup;
    private final String email;
    private final String phone;
    private final Instant enrollmentDate;
    private final FaceData faceData;

    private final Path faceImagesPath;
    private final String saveFolder = "images";

    public Student(String id, String name, String classGroup, String email, String phone, Instant enrollmentDate) {
        super(id);
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.classGroup = classGroup;
        this.email = email;
        this.phone = phone;
        this.enrollmentDate = enrollmentDate != null ? enrollmentDate : Instant.now();

        faceImagesPath = Paths.get(saveFolder, id);
        this.faceData = new FaceData(faceImagesPath.toString());
        this.faceData.setStudentID(id);
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

    public Path getFaceImagesPath() {
        return faceImagesPath;
    }

    public FaceData getFaceData() {
        return faceData;
    }
}
