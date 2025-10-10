package com.group5.smartattendance.core;

import java.time.Instant;

public abstract class Entity {

    private final String id;
    private final Instant createdAt;
    private Instant updatedAt;

    protected Entity(String id) {
        this.id = id;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public final String getId() {
        return id;
    }

    public final Instant getCreatedAt() {
        return createdAt;
    }

    public final Instant getUpdatedAt() {
        return updatedAt;
    }
}
