package com.example.smartwallet.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.smartwallet.models.Expense;

import java.util.List;

@Dao
public interface ExpenseDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    long insert(Expense expense);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    List<Expense> getAllExpenses(String userId);

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date = :date")
    double getTodaysTotal(String userId, String date);

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date LIKE :month || '%'")
    double getMonthlyTotal(String userId, String month);

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY id DESC LIMIT 1")
    Expense getLastAddedExpense(String userId);

    @Query("SELECT * FROM expenses WHERE userId = :userId AND id = :id")
    Expense getExpenseById(String userId, int id);

    @Query("SELECT * FROM expenses WHERE synced = 0")
    List<Expense> getUnsyncedExpenses();

    @Query("UPDATE expenses SET synced = 1, firebaseId = :firebaseId WHERE id = :id")
    void markSynced(int id, String firebaseId);

    @Query("SELECT * FROM expenses WHERE userId = :userId AND firebaseId = :firebaseId")
    Expense getExpenseByFirebaseId(String userId, String firebaseId);

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC LIMIT 3")
    List<Expense> getRecentExpenses(String userId);
}
