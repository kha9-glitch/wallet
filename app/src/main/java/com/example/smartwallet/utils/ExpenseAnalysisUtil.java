package com.example.smartwallet.utils;

import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.models.Expense;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseAnalysisUtil {

    public static class AnalysisResult {
        public String highestCategory;
        public double highestAmount;
        public String comparisonMessage;
        public String budgetWarning;
        public String suggestion;
    }

    public static AnalysisResult performAnalysis(AppDatabase db, String userId, String symbol) {
        AnalysisResult result = new AnalysisResult();
        
        Calendar cal = Calendar.getInstance();
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());
        
        cal.add(Calendar.MONTH, -1);
        String lastMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());

        // 1. Highest Spending Category
        List<Expense> currentExpenses = db.expenseDao().getAllExpenses(userId);
        Map<String, Double> categoryMap = new HashMap<>();
        double currentTotal = 0;
        
        for (Expense e : currentExpenses) {
            if (e.getDate().startsWith(currentMonth)) {
                categoryMap.put(e.getCategory(), categoryMap.getOrDefault(e.getCategory(), 0.0) + e.getAmount());
                currentTotal += e.getAmount();
            }
        }

        String topCat = "None";
        double topAmt = 0;
        for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
            if (entry.getValue() > topAmt) {
                topAmt = entry.getValue();
                topCat = entry.getKey();
            }
        }
        result.highestCategory = topCat;
        result.highestAmount = topAmt;

        // 2. Spending Increase/Decrease
        double lastMonthTotal = db.expenseDao().getMonthlyTotal(userId, lastMonth);
        if (lastMonthTotal > 0) {
            double diff = ((currentTotal - lastMonthTotal) / lastMonthTotal) * 100;
            if (diff > 0) {
                result.comparisonMessage = String.format(Locale.getDefault(), "Spending increased by %.1f%% compared to last month.", diff);
            } else {
                result.comparisonMessage = String.format(Locale.getDefault(), "Spending decreased by %.1f%% compared to last month.", Math.abs(diff));
            }
        } else {
            result.comparisonMessage = "No data for last month to compare.";
        }

        // 3. Budget Warning
        double budget = db.budgetDao().getBudget(userId);
        if (budget > 0 && currentTotal > budget) {
            result.budgetWarning = String.format(Locale.getDefault(), "You exceeded your budget by %s%.0f!", symbol, (currentTotal - budget));
        }

        // 4. Suggestion (Feature 3)
        if (topAmt > 0) {
            double possibleSavings = topAmt * 0.10;
            result.suggestion = String.format(Locale.getDefault(), "If you reduce %s spending by 10%%, you can save %s%.0f per month.", topCat, symbol, possibleSavings);
        } else {
            result.suggestion = "Track more expenses to get personalized saving tips.";
        }

        return result;
    }
}
