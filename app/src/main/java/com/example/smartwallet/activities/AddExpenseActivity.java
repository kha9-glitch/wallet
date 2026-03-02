package com.example.smartwallet.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartwallet.R;
import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.firebase.FirebaseSyncManager;
import com.example.smartwallet.models.Expense;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {

    private EditText etAmount, etDate, etNotes;
    private Spinner spinnerCategory, spinnerPaymentMode;
    private AppDatabase db;
    private int expenseId = -1;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            finish();
            return;
        }

        etAmount = findViewById(R.id.et_amount);
        etDate = findViewById(R.id.et_date);
        etNotes = findViewById(R.id.et_notes);
        spinnerCategory = findViewById(R.id.spinner_category);
        spinnerPaymentMode = findViewById(R.id.spinner_payment_mode);
        Button btnSave = findViewById(R.id.btn_save_expense);

        db = AppDatabase.getInstance(this);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        etDate.setText(today);

        etDate.setOnClickListener(v -> showDatePicker());

        if (getIntent().hasExtra("expense_id")) {
            expenseId = getIntent().getIntExtra("expense_id", -1);
            loadExpenseData();
            btnSave.setText("Update Expense");
        }

        btnSave.setOnClickListener(v -> saveExpense());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            etDate.setText(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadExpenseData() {
        Expense expense = db.expenseDao().getExpenseById(userId, expenseId);
        if (expense != null) {
            etAmount.setText(String.valueOf(expense.getAmount()));
            etDate.setText(expense.getDate());
            etNotes.setText(expense.getNotes());

            ArrayAdapter categoryAdapter = (ArrayAdapter) spinnerCategory.getAdapter();
            spinnerCategory.setSelection(categoryAdapter.getPosition(expense.getCategory()));

            ArrayAdapter paymentAdapter = (ArrayAdapter) spinnerPaymentMode.getAdapter();
            spinnerPaymentMode.setSelection(paymentAdapter.getPosition(expense.getPaymentMode()));
        }
    }

    private void saveExpense() {
        String amountStr = etAmount.getText().toString();
        String category = spinnerCategory.getSelectedItem().toString();
        String payment = spinnerPaymentMode.getSelectedItem().toString();
        String date = etDate.getText().toString();
        String notes = etNotes.getText().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        Expense expense = new Expense(userId, amount, category, payment, date, notes);

        if (expenseId == -1) {
            db.expenseDao().insert(expense);
            Toast.makeText(this, "Expense Saved Locally", Toast.LENGTH_SHORT).show();
        } else {
            expense.setId(expenseId);
            db.expenseDao().update(expense);
            Toast.makeText(this, "Expense Updated Locally", Toast.LENGTH_SHORT).show();
        }

        // Trigger Sync
        FirebaseSyncManager.getInstance(this).syncExpenses();

        finish();
    }
}
