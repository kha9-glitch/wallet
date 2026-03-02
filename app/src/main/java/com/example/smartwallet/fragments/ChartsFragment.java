package com.example.smartwallet.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smartwallet.R;
import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.models.Expense;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartsFragment extends Fragment {

    private PieChart pieChart;
    private BarChart barChart;
    private AppDatabase db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charts, container, false);

        pieChart = view.findViewById(R.id.pie_chart);
        barChart = view.findViewById(R.id.bar_chart);

        db = AppDatabase.getInstance(getContext());
        userId = FirebaseAuth.getInstance().getUid();

        setupCharts();
        loadData();

        return view;
    }

    private void setupCharts() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.getLegend().setTextColor(Color.WHITE);

        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setTextColor(Color.WHITE);
        barChart.getAxisLeft().setTextColor(Color.WHITE);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setTextColor(Color.WHITE);
    }

    private void loadData() {
        if (userId == null) return;

        List<Expense> expenses = db.expenseDao().getAllExpenses(userId);

        // Category-wise Map
        Map<String, Float> categoryMap = new HashMap<>();
        // Monthly Map (Format: MM-YYYY)
        Map<String, Float> monthlyMap = new HashMap<>();

        for (Expense e : expenses) {
            String cat = e.getCategory();
            categoryMap.put(cat, categoryMap.getOrDefault(cat, 0f) + (float) e.getAmount());

            // Simple month parsing from YYYY-MM-DD
            if (e.getDate() != null && e.getDate().length() >= 7) {
                String month = e.getDate().substring(5, 7);
                monthlyMap.put(month, monthlyMap.getOrDefault(month, 0f) + (float) e.getAmount());
            }
        }

        setupPieChart(categoryMap);
        setupBarChart(monthlyMap);
    }

    private void setupPieChart(Map<String, Float> map) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : map.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void setupBarChart(Map<String, Float> map) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Float> entry : map.entrySet()) {
            entries.add(new BarEntry(i++, entry.getValue()));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Monthly Spend");
        dataSet.setColors(ColorTemplate.LIBERTY_COLORS);
        dataSet.setValueTextColor(Color.WHITE);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.invalidate();
    }
}
