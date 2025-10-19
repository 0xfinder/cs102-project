package com.group5.smartattendance.session;

import com.group5.smartattendance.core.Entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

public class Session extends Entity {

    public enum Status {
        OPEN,
        CLOSED
    }

    private final String courseName;
    private final LocalDate sessionDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private String location; // optional
    private Status status;
    private final Roster roster;

    public Session(
            String id,
            String courseName,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String location,
            Status status,
            Roster roster) {
        super(id);
        this.courseName = Objects.requireNonNull(courseName, "courseName must not be null");
        this.sessionDate = Objects.requireNonNull(sessionDate, "sessionDate must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.location = location;
        this.status = status == null ? Status.OPEN : status;
        this.roster = roster == null ? new Roster() : roster;
    }

    public Session(
            String id,
            String courseName,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime) {
        this(id, courseName, sessionDate, startTime, endTime, null, Status.OPEN, new Roster());
    }

    public Session(
            String id,
            String courseName,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String location,
            Status status) {
        this(id, courseName, sessionDate, startTime, endTime, location, status, new Roster());
    }

    public String getCourseName() {
        return courseName;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Optional<String> getLocation() {
        return Optional.ofNullable(location);
    }

    public Status getStatus() {
        return status;
    }

    public Roster getRoster() {
        return roster;
    }

    public void updateLocation(String location) {
        this.location = location;
    }

    public void clearLocation() {
        this.location = null;
    }

    public void open() {
        this.status = Status.OPEN;
    }

    public void close() {
        this.status = Status.CLOSED;
    }

    public Session copyWithRoster(Roster roster) {
        return new Session(
                getId(),
                courseName,
                sessionDate,
                startTime,
                endTime,
                location,
                status,
                roster);
    }
}
