package com.example.smartwallet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartwallet.R;
import com.example.smartwallet.models.Expense;
import com.example.smartwallet.utils.CurrencyUtils;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenseList;
    private OnExpenseClickListener listener;

    public interface OnExpenseClickListener {
        void onDeleteClick(Expense expense);
        void onEditClick(Expense expense);
    }

    public ExpenseAdapter(List<Expense> expenseList, OnExpenseClickListener listener) {
        this.expenseList = expenseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.tvCategory.setText(expense.getCategory());
        holder.tvDate.setText(expense.getDate());
        holder.tvPayment.setText(expense.getPaymentMode());
        String symbol = CurrencyUtils.getCurrencySymbol(holder.itemView.getContext());
        holder.tvAmount.setText(symbol + expense.getAmount());

        holder.itemView.setOnClickListener(v -> listener.onEditClick(expense));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(expense));
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public void updateList(List<Expense> newList) {
        this.expenseList = newList;
        notifyDataSetChanged();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDate, tvPayment, tvAmount;
        ImageButton btnDelete;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_item_category);
            tvDate = itemView.findViewById(R.id.tv_item_date);
            tvPayment = itemView.findViewById(R.id.tv_item_payment);
            tvAmount = itemView.findViewById(R.id.tv_item_amount);
            btnDelete = itemView.findViewById(R.id.btn_delete_expense);
        }
    }
}
