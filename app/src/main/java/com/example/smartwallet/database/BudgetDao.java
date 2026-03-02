package com.example.smartwallet.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.smartwallet.models.Budget;

@Dao
public interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(Budget budget);

    @Query("SELECT monthlyBudget FROM budgets WHERE userId = :userId LIMIT 1")
    double getBudget(String userId);
}
