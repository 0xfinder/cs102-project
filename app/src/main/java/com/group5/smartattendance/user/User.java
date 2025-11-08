package com.group5.smartattendance.user;

import com.group5.smartattendance.core.Entity;

import java.util.Objects;

public class User extends Entity {

    private final String email;
    private final String fullName;
    private final String passwordHash;

    public User(String id, String email, String fullName, String passwordHash) {
        super(id);
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.fullName = fullName;
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
