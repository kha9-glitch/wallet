package com.example.smartwallet.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartwallet.activities.AddDocumentActivity;
import com.example.smartwallet.activities.AddExpenseActivity;
import com.example.smartwallet.activities.UpiPaymentActivity;
import com.example.smartwallet.adapters.ExpenseAdapter;
import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.databinding.FragmentDashboardBinding;
import com.example.smartwallet.models.Document;
import com.example.smartwallet.models.Expense;
import com.example.smartwallet.utils.ExpenseAnalysisUtil;
import com.example.smartwallet.utils.CurrencyUtils;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment implements ExpenseAdapter.OnExpenseClickListener {

    private FragmentDashboardBinding binding;
    private AppDatabase db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        db = AppDatabase.getInstance(getContext());
        userId = FirebaseAuth.getInstance().getUid();

        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            String email = user.getEmail();
            if (displayName != null && !displayName.isEmpty()) {
                binding.tvGreeting.setText("Hi, " + displayName + "!");
            } else if (email != null) {
                String name = email.split("@")[0];
                binding.tvGreeting.setText("Hi, " + name + "!");
            } else {
                binding.tvGreeting.setText("Hi, User!");
            }
        }

        binding.btnAddExpenseQuick.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddExpenseActivity.class)));
        binding.btnAddDocQuick.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddDocumentActivity.class)));
        binding.btnUpiPayQuick.setOnClickListener(v -> startActivity(new Intent(getActivity(), UpiPaymentActivity.class)));

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
            String symbol = CurrencyUtils.getCurrencySymbol(getContext());
            ExpenseAnalysisUtil.AnalysisResult analysis = ExpenseAnalysisUtil.performAnalysis(db, userId, symbol);

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (binding == null) return; // Prevent crashes if fragment is destroyed

                binding.tvTodayExpense.setText(symbol + String.format("%.0f", todayTotal));
                binding.tvMonthlyExpense.setText(symbol + String.format("%.0f", monthlyTotal));
                binding.tvTotalDocs.setText(String.valueOf(totalDocs));

                // Display Analysis
                binding.tvAiTopCat.setText("Highest Spending: " + analysis.highestCategory + " (" + symbol + String.format("%.0f", analysis.highestAmount) + ")");
                binding.tvAiComparison.setText(analysis.comparisonMessage);
                binding.tvAiSuggestion.setText(analysis.suggestion);
                if (analysis.budgetWarning != null) {
                    binding.tvAiWarning.setText(analysis.budgetWarning);
                    binding.tvAiWarning.setVisibility(View.VISIBLE);
                } else {
                    binding.tvAiWarning.setVisibility(View.GONE);
                }

                if (monthlyBudget > 0) {
                    double remaining = monthlyBudget - monthlyTotal;
                    binding.tvBudgetTotal.setText("Budget: " + symbol + String.format("%.0f", monthlyBudget));
                    binding.tvBudgetRemaining.setText(symbol + String.format("%.0f", Math.max(0, remaining)) + " left");
                    int progress = (int) ((monthlyTotal / monthlyBudget) * 100);
                    binding.budgetProgress.setProgress(Math.min(100, progress));
                } else {
                    binding.tvBudgetTotal.setText("No Budget Set");
                    binding.tvBudgetRemaining.setText(symbol + "0 left");
                    binding.budgetProgress.setProgress(0);
                }

                if (nextExpiry != null) {
                    binding.cardNextExpiry.setVisibility(View.VISIBLE);
                    binding.tvExpiryName.setText(nextExpiry.getDocumentName());
                    binding.tvExpiryDate.setText("Expires: " + nextExpiry.getExpiryDate());
                } else {
                    binding.cardNextExpiry.setVisibility(View.GONE);
                }

                if (recentExpenses != null && !recentExpenses.isEmpty()) {
                    ExpenseAdapter adapter = new ExpenseAdapter(recentExpenses, this);
                    binding.rvRecentExpenses.setAdapter(adapter);
                    binding.rvRecentExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
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
