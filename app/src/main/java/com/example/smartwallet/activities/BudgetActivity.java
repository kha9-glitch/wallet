package com.example.smartwallet.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartwallet.R;
import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.models.Budget;
import com.example.smartwallet.models.CategoryLimit;
import com.example.smartwallet.utils.CurrencyUtils;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class BudgetActivity extends AppCompatActivity {

    private TextInputEditText etBudget, etLimitAmount;
    private Spinner spinnerLimitCategory;
    private AppDatabase db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        etBudget = findViewById(R.id.et_budget_amount);
        etLimitAmount = findViewById(R.id.et_limit_amount);
        spinnerLimitCategory = findViewById(R.id.spinner_limit_category);
        Button btnSave = findViewById(R.id.btn_save_budget);
        Button btnSaveLimit = findViewById(R.id.btn_save_limit);
        ImageButton btnBack = findViewById(R.id.btn_back);

        db = AppDatabase.getInstance(this);
        userId = FirebaseAuth.getInstance().getUid();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.expense_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLimitCategory.setAdapter(adapter);

        String symbol = CurrencyUtils.getCurrencySymbol(this);
        ((TextInputLayout)findViewById(R.id.til_budget)).setPrefixText(symbol);
        ((TextInputLayout)findViewById(R.id.til_limit)).setPrefixText(symbol);
        ((TextInputLayout)findViewById(R.id.til_budget)).setHint("Amount (" + symbol + ")");
        ((TextInputLayout)findViewById(R.id.til_limit)).setHint("Limit Amount (" + symbol + ")");

        loadCurrentBudget();

        btnSave.setOnClickListener(v -> saveBudget());
        btnSaveLimit.setOnClickListener(v -> saveCategoryLimit());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadCurrentBudget() {
        new Thread(() -> {
            double budget = db.budgetDao().getBudget(userId);
            runOnUiThread(() -> etBudget.setText(String.valueOf(budget)));
        }).start();
    }

    private void saveBudget() {
        String amountStr = etBudget.getText().toString();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter budget amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        Budget budget = new Budget(userId, amount);

        new Thread(() -> db.budgetDao().insertOrUpdate(budget)).start();
        Toast.makeText(this, "Budget updated successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveCategoryLimit() {
        String amountStr = etLimitAmount.getText().toString();
        String category = spinnerLimitCategory.getSelectedItem().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter limit amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        CategoryLimit limit = new CategoryLimit(userId, category, amount);

        new Thread(() -> {
            db.categoryLimitDao().insertOrUpdate(limit);
            runOnUiThread(() -> {
                Toast.makeText(this, "Limit set for " + category, Toast.LENGTH_SHORT).show();
                etLimitAmount.setText("");
            });
        }).start();
    }
}
