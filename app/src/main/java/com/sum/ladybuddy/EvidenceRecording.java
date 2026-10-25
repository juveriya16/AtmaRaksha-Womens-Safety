package com.sum.ladybuddy;

import java.io.Serializable;

public class EvidenceRecording implements Serializable {
    private long id;
    private String filename;
    private String filePath;
    private int durationSeconds;
    private long fileSizeBytes;
    private String timestamp;
    private String notes;

    public EvidenceRecording() {
    }

    public EvidenceRecording(long id, String filename, String filePath, int durationSeconds,
                             long fileSizeBytes, String timestamp, String notes) {
        this.id = id;
        this.filename = filename;
        this.filePath = filePath;
        this.durationSeconds = durationSeconds;
        this.fileSizeBytes = fileSizeBytes;
        this.timestamp = timestamp;
        this.notes = notes;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename != null ? filename : "AudioEvidence.m4a";
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilePath() {
        return filePath != null ? filePath : "";
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getTimestamp() {
        return timestamp != null ? timestamp : "";
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotes() {
        return notes != null ? notes : "";
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
