package com.example.smartwallet.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartwallet.R;
import com.example.smartwallet.activities.AddExpenseActivity;
import com.example.smartwallet.adapters.ExpenseAdapter;
import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.models.Expense;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExpensesFragment extends Fragment implements ExpenseAdapter.OnExpenseClickListener {

    private RecyclerView rvExpenses;
    private ExpenseAdapter adapter;
    private AppDatabase db;
    private String userId;
    private List<Expense> allExpenses = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        rvExpenses = view.findViewById(R.id.rv_expenses);
        FloatingActionButton fab = view.findViewById(R.id.fab_add_expense);
        EditText etSearch = view.findViewById(R.id.et_search_expenses);

        db = AppDatabase.getInstance(getContext());
        userId = FirebaseAuth.getInstance().getUid();

        adapter = new ExpenseAdapter(new ArrayList<>(), this);
        rvExpenses.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fab.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddExpenseActivity.class)));

        return view;
    }

    private void filter(String query) {
        if (query.isEmpty()) {
            adapter.updateList(allExpenses);
        } else {
            List<Expense> filtered = allExpenses.stream()
                    .filter(e -> e.getCategory().toLowerCase().contains(query.toLowerCase()) || 
                                (e.getNotes() != null && e.getNotes().toLowerCase().contains(query.toLowerCase())))
                    .collect(Collectors.toList());
            adapter.updateList(filtered);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userId != null) {
            loadExpenses();
        }
    }

    private void loadExpenses() {
        allExpenses = db.expenseDao().getAllExpenses(userId);
        adapter.updateList(allExpenses);
    }

    @Override
    public void onDeleteClick(Expense expense) {
        db.expenseDao().delete(expense);
        loadExpenses();
    }

    @Override
    public void onEditClick(Expense expense) {
        Intent intent = new Intent(getActivity(), AddExpenseActivity.class);
        intent.putExtra("expense_id", expense.getId());
        startActivity(intent);
    }
}
