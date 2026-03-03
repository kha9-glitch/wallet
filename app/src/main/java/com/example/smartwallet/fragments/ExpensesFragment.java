package com.example.smartwallet.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
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
    private EditText etSearch;
    private FloatingActionButton fab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        rvExpenses = view.findViewById(R.id.rv_expenses);
        etSearch = view.findViewById(R.id.et_search_expenses);
        fab = view.findViewById(R.id.fab_add_expense);
        ImageButton btnFilter = view.findViewById(R.id.btn_filter_expenses);

        rvExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        
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

        btnFilter.setOnClickListener(v -> {
            // Show a simple sort/filter menu
            android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), btnFilter);
            popup.getMenu().add("Sort by Date (Newest)");
            popup.getMenu().add("Sort by Date (Oldest)");
            popup.getMenu().add("Sort by Amount (High-Low)");
            popup.getMenu().add("Sort by Amount (Low-High)");
            
            popup.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if (title.contains("Newest")) {
                    allExpenses.sort((e1, e2) -> e2.getDate().compareTo(e1.getDate()));
                } else if (title.contains("Oldest")) {
                    allExpenses.sort((e1, e2) -> e1.getDate().compareTo(e2.getDate()));
                } else if (title.contains("High-Low")) {
                    allExpenses.sort((e1, e2) -> Double.compare(e2.getAmount(), e1.getAmount()));
                } else if (title.contains("Low-High")) {
                    allExpenses.sort((e1, e2) -> Double.compare(e1.getAmount(), e2.getAmount()));
                }
                adapter.updateList(new ArrayList<>(allExpenses));
                return true;
            });
            popup.show();
        });

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
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.expenseDao().delete(expense);
                    loadExpenses();
                    // Sync with Firebase
                    com.example.smartwallet.firebase.FirebaseSyncManager.getInstance(getContext()).syncExpenses();
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
