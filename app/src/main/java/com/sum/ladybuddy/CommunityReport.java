package com.sum.ladybuddy;

import java.io.Serializable;

public class CommunityReport implements Serializable {
    private long id;
    private String title;
    private String category; // POOR_LIGHTING, HARASSMENT_SPOT, DESERTED_ALLEY, BROKEN_INFRA, SAFE_HAVEN
    private String description;
    private double latitude;
    private double longitude;
    private String address;
    private String timestamp;
    private String status; // VERIFIED, COMMUNITY_REPORTED, RESOLVED
    private int upvotes;
    private int flags;

    public CommunityReport() {
    }

    public CommunityReport(long id, String title, String category, String description,
                           double latitude, double longitude, String address, String timestamp,
                           String status, int upvotes, int flags) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.timestamp = timestamp;
        this.status = status;
        this.upvotes = upvotes;
        this.flags = flags;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title != null ? title : "Safety Report";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category != null ? category : "COMMUNITY_REPORTED";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address != null ? address : "";
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTimestamp() {
        return timestamp != null ? timestamp : "";
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status != null ? status : "COMMUNITY_REPORTED";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(int upvotes) {
        this.upvotes = upvotes;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }
}
