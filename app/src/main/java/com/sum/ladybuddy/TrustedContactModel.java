package com.sum.ladybuddy;

import java.io.Serializable;

public class TrustedContactModel implements Serializable {
    private String id;
    private String name;
    private String phoneNumber;
    private String relationship;
    private boolean isSelectedForMonitoring;

    public TrustedContactModel(String id, String name, String phoneNumber, String relationship, boolean isSelectedForMonitoring) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.relationship = relationship;
        this.isSelectedForMonitoring = isSelectedForMonitoring;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name != null && !name.trim().isEmpty() ? name : "Guardian";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber != null ? phoneNumber : "";
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getRelationship() {
        return relationship != null && !relationship.trim().isEmpty() ? relationship : "Trusted Circle";
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public boolean isSelectedForMonitoring() {
        return isSelectedForMonitoring;
    }

    public boolean isMonitoringEnabled() {
        return isSelectedForMonitoring;
    }

    public void setMonitoringEnabled(boolean monitoringEnabled) {
        this.isSelectedForMonitoring = monitoringEnabled;
    }

    public void setSelectedForMonitoring(boolean selectedForMonitoring) {
        isSelectedForMonitoring = selectedForMonitoring;
    }
}
