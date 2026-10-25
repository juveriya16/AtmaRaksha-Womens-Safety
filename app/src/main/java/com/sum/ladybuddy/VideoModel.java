package com.sum.ladybuddy;

public class VideoModel {
    private String videoTitle;
    private String videoUri;

    public VideoModel(String videoTitle, String videoUri) {
        this.videoTitle = videoTitle;
        this.videoUri = videoUri;
    }

    public String getVideoTitle() { return videoTitle; }
    public String getVideoUri() { return videoUri; }
}