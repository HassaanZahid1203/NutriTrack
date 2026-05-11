package com.nutritrack.app.models;

import com.google.firebase.firestore.PropertyName;

/**
 * User profile model. Stored in Firestore under users/{uid}.
 */
public class User {
    private String uid;
    private String email;
    private String displayName;
    private int dailyCalorieTarget;     // kcal/day, default 2000
    private double weightCurrentKg;
    private double weightGoalKg;
    private long createdAt;
    private boolean isAdmin;

    public User() { /* required no-arg ctor for Firestore */ }

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.dailyCalorieTarget = 2000;
        this.weightCurrentKg = 72.6;
        this.weightGoalKg = 70.0;
        this.createdAt = System.currentTimeMillis();
        this.isAdmin = false;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public int getDailyCalorieTarget() { return dailyCalorieTarget; }
    public void setDailyCalorieTarget(int dailyCalorieTarget) { this.dailyCalorieTarget = dailyCalorieTarget; }
    public double getWeightCurrentKg() { return weightCurrentKg; }
    public void setWeightCurrentKg(double weightCurrentKg) { this.weightCurrentKg = weightCurrentKg; }
    public double getWeightGoalKg() { return weightGoalKg; }
    public void setWeightGoalKg(double weightGoalKg) { this.weightGoalKg = weightGoalKg; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @PropertyName("isAdmin")
    public boolean isAdmin() { return isAdmin; }
    @PropertyName("isAdmin")
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public String getInitials() {
        if (displayName == null || displayName.isEmpty()) return "U";
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
