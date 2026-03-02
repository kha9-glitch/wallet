package com.example.smartwallet.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartwallet.R;
import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.models.Budget;
import com.example.smartwallet.utils.FirebaseSyncHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class BudgetActivity extends AppCompatActivity {

    private TextInputEditText etBudget;
    private AppDatabase db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        etBudget = findViewById(R.id.et_budget_amount);
        Button btnSave = findViewById(R.id.btn_save_budget);
        ImageButton btnBack = findViewById(R.id.btn_back);

        db = AppDatabase.getInstance(this);
        userId = FirebaseAuth.getInstance().getUid();

        loadCurrentBudget();

        btnSave.setOnClickListener(v -> saveBudget());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadCurrentBudget() {
        if (userId != null) {
            double currentBudget = db.budgetDao().getBudget(userId);
            if (currentBudget > 0) {
                etBudget.setText(String.valueOf(currentBudget));
            }
        }
    }

    private void saveBudget() {
        String amountStr = etBudget.getText().toString();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        Budget budget = new Budget(userId, amount);
        
        // Save to Local DB
        db.budgetDao().insertOrUpdate(budget);
        
        // Sync to Firebase (Basic implementation)
        FirebaseSyncHelper.syncBudget(userId, amount);

        Toast.makeText(this, "Budget updated successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
