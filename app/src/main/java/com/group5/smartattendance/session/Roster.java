package com.group5.smartattendance.session;

import com.group5.smartattendance.student.Student;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Roster {

    private final Map<String, Student> studentsById = new LinkedHashMap<>();

    public Roster() {
    }

    public Roster(Collection<Student> students) {
        this();
        if (students != null) {
            students.forEach(this::addStudent);
        }
    }

    public static Roster of(Student... students) {
        Roster roster = new Roster();
        if (students != null) {
            for (Student student : students) {
                roster.addStudent(student);
            }
        }
        return roster;
    }

    public void addStudent(Student student) {
        Student toAdd = Objects.requireNonNull(student, "student must not be null");
        String studentId = Objects.requireNonNull(toAdd.getId(), "student id must not be null");
        if (studentsById.containsKey(studentId)) {
            throw new IllegalArgumentException("Student with id " + studentId + " already in roster");
        }
        studentsById.put(studentId, toAdd);
    }

    public void removeStudentById(String studentId) {
        studentsById.remove(studentId);
    }

    public boolean contains(String studentId) {
        return studentsById.containsKey(studentId);
    }

    public List<Student> getStudents() {
        return Collections.unmodifiableList(new ArrayList<>(studentsById.values()));
    }

    public boolean isEmpty() {
        return studentsById.isEmpty();
    }

    public int size() {
        return studentsById.size();
    }
}
