package com.example.smartwallet.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.smartwallet.models.Document;
import com.example.smartwallet.models.Expense;
import com.example.smartwallet.models.Budget;

@Database(entities = {Expense.class, Document.class, Budget.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract ExpenseDao expenseDao();
    public abstract DocumentDao documentDao();
    public abstract BudgetDao budgetDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "smart_wallet_db")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // For simplicity in this example
                    .build();
        }
        return instance;
    }
}
