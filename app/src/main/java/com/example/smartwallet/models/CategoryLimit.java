package com.example.smartwallet.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "category_limits", primaryKeys = {"userId", "category"})
public class CategoryLimit {
    @NonNull
    private String userId;
    @NonNull
    private String category;
    private double limitAmount;

    public CategoryLimit(@NonNull String userId, @NonNull String category, double limitAmount) {
        this.userId = userId;
        this.category = category;
        this.limitAmount = limitAmount;
    }

    @NonNull
    public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }

    @NonNull
    public String getCategory() { return category; }
    public void setCategory(@NonNull String category) { this.category = category; }

    public double getLimitAmount() { return limitAmount; }
    public void setLimitAmount(double limitAmount) { this.limitAmount = limitAmount; }
}
