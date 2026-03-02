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

import com.example.smartwallet.R;
import com.example.smartwallet.activities.AddDocumentActivity;
import com.example.smartwallet.activities.AddExpenseActivity;
import com.example.smartwallet.database.AppDatabase;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private TextView tvTodayExpense, tvMonthlyExpense, tvTotalDocs, tvGreeting, tvBudgetRemaining, tvBudgetTotal;
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

        Button btnAddExpense = view.findViewById(R.id.btn_add_expense_quick);
        Button btnAddDoc = view.findViewById(R.id.btn_add_doc_quick);

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

        double todayTotal = db.expenseDao().getTodaysTotal(userId, today);
        double monthlyTotal = db.expenseDao().getMonthlyTotal(userId, month);
        double monthlyBudget = db.budgetDao().getBudget(userId);

        int totalDocs = db.documentDao().getDocumentCount(userId);

        tvTodayExpense.setText("₹" + String.format("%.0f", todayTotal));
        tvMonthlyExpense.setText("₹" + String.format("%.0f", monthlyTotal));
        tvTotalDocs.setText(String.valueOf(totalDocs));

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
    }
}
