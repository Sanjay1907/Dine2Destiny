package com.example.dine2destiny;

import android.util.Log; // Import for logging

public class BugReport {
    private static final String TAG = "BugReport"; // Tag for logging
    private String id;
    private String description;
    private String imageUrl; // Image URL for the bug report

    public BugReport() {
        // Default constructor required for Firebase
        Log.d(TAG, "BugReport default constructor called.");
    }

    public BugReport(String id, String description, String imageUrl) {
        this.id = id;
        this.description = description;
        this.imageUrl = imageUrl;
        Log.d(TAG, "BugReport constructor called with id: " + id + ", description: " + description + ", imageUrl: " + imageUrl);
    }

    public String getId() {
        Log.d(TAG, "getId() called.");
        return id;
    }

    public String getDescription() {
        Log.d(TAG, "getDescription() called.");
        return description;
    }

    public String getImageUrl() {
        Log.d(TAG, "getImageUrl() called.");
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        Log.d(TAG, "setImageUrl() called with imageUrl: " + imageUrl);
    }
}
