package com.sum.ladybuddy;

import java.io.Serializable;

public class JourneyRecord implements Serializable {
    private long id;
    private String origin;
    private String destination;
    private String startTime;
    private String endTime;
    private int durationMinutes;
    private String eta;
    private String status; // ACTIVE, CHECK_IN_REQUIRED, ALERT_SENT, COMPLETED, CANCELLED
    private int checkinsCompleted;
    private int missedCheckins;
    private String contactsShared;
    private String routeSummary;

    public JourneyRecord() {
    }

    public JourneyRecord(long id, String origin, String destination, String startTime, String endTime,
                         int durationMinutes, String eta, String status, int checkinsCompleted,
                         int missedCheckins, String contactsShared, String routeSummary) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.eta = eta;
        this.status = status;
        this.checkinsCompleted = checkinsCompleted;
        this.missedCheckins = missedCheckins;
        this.contactsShared = contactsShared;
        this.routeSummary = routeSummary;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOrigin() {
        return origin != null ? origin : "Current Location";
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination != null ? destination : "Destination";
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getStartTime() {
        return startTime != null ? startTime : "";
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime != null ? endTime : "";
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getEta() {
        return eta != null ? eta : "";
    }

    public void setEta(String eta) {
        this.eta = eta;
    }

    public String getStatus() {
        return status != null ? status : "COMPLETED";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCheckinsCompleted() {
        return checkinsCompleted;
    }

    public void setCheckinsCompleted(int checkinsCompleted) {
        this.checkinsCompleted = checkinsCompleted;
    }

    public int getMissedCheckins() {
        return missedCheckins;
    }

    public void setMissedCheckins(int missedCheckins) {
        this.missedCheckins = missedCheckins;
    }

    public String getContactsShared() {
        return contactsShared != null ? contactsShared : "";
    }

    public void setContactsShared(String contactsShared) {
        this.contactsShared = contactsShared;
    }

    public String getRouteSummary() {
        return routeSummary != null ? routeSummary : "";
    }

    public void setRouteSummary(String routeSummary) {
        this.routeSummary = routeSummary;
    }

    public int getCheckInsCompleted() {
        return getCheckinsCompleted();
    }

    public int getMissedCheckIns() {
        return getMissedCheckins();
    }

    public String getMonitoredContacts() {
        return getContactsShared();
    }
}
