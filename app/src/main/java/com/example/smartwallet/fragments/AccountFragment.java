package com.example.smartwallet.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.smartwallet.R;
import com.example.smartwallet.activities.BudgetActivity;
import com.example.smartwallet.activities.LoginActivity;
import com.example.smartwallet.firebase.FirebaseSyncManager;
import com.google.firebase.auth.FirebaseAuth;

public class AccountFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        TextView tvEmail = view.findViewById(R.id.tv_user_email);
        Button btnBudget = view.findViewById(R.id.btn_set_budget);
        Button btnSync = view.findViewById(R.id.btn_sync_now);
        Button btnCharts = view.findViewById(R.id.btn_view_charts);
        Button btnLogout = view.findViewById(R.id.btn_logout);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            tvEmail.setText(email);
        }

        btnBudget.setOnClickListener(v -> startActivity(new Intent(getActivity(), BudgetActivity.class)));

        btnSync.setOnClickListener(v -> {
            FirebaseSyncManager.getInstance(getContext()).syncExpenses();
            FirebaseSyncManager.getInstance(getContext()).syncDocuments();
            Toast.makeText(getContext(), "Sync Started", Toast.LENGTH_SHORT).show();
        });

        // Use Navigation controller to go to charts if configured, or just show a message
        btnCharts.setOnClickListener(v -> {
            try {
               Navigation.findNavController(v).navigate(R.id.navigation_charts);
            } catch (Exception e) {
               Toast.makeText(getContext(), "Analytics screen opening...", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        return view;
    }
}
