package com.example.smartwallet.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartwallet.R;
import com.example.smartwallet.activities.AddDocumentActivity;
import com.example.smartwallet.activities.AddExpenseActivity;
import com.example.smartwallet.activities.UpiPaymentActivity;
import com.example.smartwallet.adapters.ExpenseAdapter;
import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.models.Document;
import com.example.smartwallet.models.Expense;
import com.example.smartwallet.utils.ExpenseAnalysisUtil;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment implements ExpenseAdapter.OnExpenseClickListener {

    private TextView tvTodayExpense, tvMonthlyExpense, tvTotalDocs, tvGreeting, tvBudgetRemaining, tvBudgetTotal;
    private TextView tvExpiryName, tvExpiryDate;
    private TextView tvAiTopCat, tvAiComparison, tvAiSuggestion, tvAiWarning;
    private MaterialCardView cardExpiry;
    private RecyclerView rvRecentExpenses;
    private LinearProgressIndicator budgetProgress;
    private AppDatabase db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvTodayExpense = view.findViewById(R.id.tv_today_expense);
        tvMonthlyExpense = view.findViewById(R.id.tv_monthly_expense);
        tvTotalDocs = view.findViewById(R.id.tv_total_docs);
        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvBudgetRemaining = view.findViewById(R.id.tv_budget_remaining);
        tvBudgetTotal = view.findViewById(R.id.tv_budget_total);
        budgetProgress = view.findViewById(R.id.budget_progress);
        
        tvExpiryName = view.findViewById(R.id.tv_expiry_name);
        tvExpiryDate = view.findViewById(R.id.tv_expiry_date);
        cardExpiry = view.findViewById(R.id.card_next_expiry);
        rvRecentExpenses = view.findViewById(R.id.rv_recent_expenses);

        tvAiTopCat = view.findViewById(R.id.tv_ai_top_cat);
        tvAiComparison = view.findViewById(R.id.tv_ai_comparison);
        tvAiSuggestion = view.findViewById(R.id.tv_ai_suggestion);
        tvAiWarning = view.findViewById(R.id.tv_ai_warning);

        Button btnAddExpense = view.findViewById(R.id.btn_add_expense_quick);
        Button btnAddDoc = view.findViewById(R.id.btn_add_doc_quick);
        Button btnUpiPay = view.findViewById(R.id.btn_upi_pay_quick);

        db = AppDatabase.getInstance(getContext());
        userId = FirebaseAuth.getInstance().getUid();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            if (email != null) {
                String name = email.split("@")[0];
                tvGreeting.setText("Hi, " + name + "!");
            }
        }

        btnAddExpense.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddExpenseActivity.class)));
        btnAddDoc.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddDocumentActivity.class)));
        btnUpiPay.setOnClickListener(v -> startActivity(new Intent(getActivity(), UpiPaymentActivity.class)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userId != null) {
            updateDashboard();
        }
    }

    private void updateDashboard() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String month = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        new Thread(() -> {
            double todayTotal = db.expenseDao().getTodaysTotal(userId, today);
            double monthlyTotal = db.expenseDao().getMonthlyTotal(userId, month);
            double monthlyBudget = db.budgetDao().getBudget(userId);
            int totalDocs = db.documentDao().getDocumentCount(userId);
            
            List<Expense> recentExpenses = db.expenseDao().getRecentExpenses(userId);
            Document nextExpiry = db.documentDao().getNextExpiryDocument(userId, today);
            ExpenseAnalysisUtil.AnalysisResult analysis = ExpenseAnalysisUtil.performAnalysis(db, userId);

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                tvTodayExpense.setText("₹" + String.format("%.0f", todayTotal));
                tvMonthlyExpense.setText("₹" + String.format("%.0f", monthlyTotal));
                tvTotalDocs.setText(String.valueOf(totalDocs));

                // Display Analysis
                tvAiTopCat.setText("Highest Spending: " + analysis.highestCategory + " (₹" + String.format("%.0f", analysis.highestAmount) + ")");
                tvAiComparison.setText(analysis.comparisonMessage);
                tvAiSuggestion.setText(analysis.suggestion);
                if (analysis.budgetWarning != null) {
                    tvAiWarning.setText(analysis.budgetWarning);
                    tvAiWarning.setVisibility(View.VISIBLE);
                } else {
                    tvAiWarning.setVisibility(View.GONE);
                }

                if (monthlyBudget > 0) {
                    double remaining = monthlyBudget - monthlyTotal;
                    tvBudgetTotal.setText("Budget: ₹" + String.format("%.0f", monthlyBudget));
                    tvBudgetRemaining.setText("₹" + String.format("%.0f", Math.max(0, remaining)) + " left");
                    int progress = (int) ((monthlyTotal / monthlyBudget) * 100);
                    budgetProgress.setProgress(Math.min(100, progress));
                } else {
                    tvBudgetTotal.setText("No Budget Set");
                    tvBudgetRemaining.setText("₹0 left");
                    budgetProgress.setProgress(0);
                }

                if (nextExpiry != null) {
                    cardExpiry.setVisibility(View.VISIBLE);
                    tvExpiryName.setText(nextExpiry.getDocumentName());
                    tvExpiryDate.setText("Expires: " + nextExpiry.getExpiryDate());
                } else {
                    cardExpiry.setVisibility(View.GONE);
                }

                if (recentExpenses != null && !recentExpenses.isEmpty()) {
                    ExpenseAdapter adapter = new ExpenseAdapter(recentExpenses, this);
                    rvRecentExpenses.setAdapter(adapter);
                    rvRecentExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
                }
            });
        }).start();
    }

    @Override
    public void onDeleteClick(Expense expense) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    new Thread(() -> {
                        db.expenseDao().delete(expense);
                        getActivity().runOnUiThread(this::updateDashboard);
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onEditClick(Expense expense) {
        Intent intent = new Intent(getActivity(), AddExpenseActivity.class);
        intent.putExtra("expense_id", expense.getId());
        startActivity(intent);
    }
}
