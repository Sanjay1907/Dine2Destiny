package com.example.dine2destiny;
public class BugReport {
    private String id;
    private String description;
    private String imageUrl; // Image URL for the bug report

    public BugReport() {
        // Default constructor required for Firebase
    }

    public BugReport(String id, String description, String imageUrl) {
        this.id = id;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
