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
import com.example.smartwallet.activities.ExportActivity;
import com.example.smartwallet.activities.LoginActivity;
import com.example.smartwallet.activities.PinActivity;
import com.example.smartwallet.firebase.FirebaseSyncManager;
import com.example.smartwallet.utils.SecurityUtils;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;

public class AccountFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        TextView tvName = view.findViewById(R.id.tv_user_name);
        TextView tvEmail = view.findViewById(R.id.tv_user_email);
        Button btnBudget = view.findViewById(R.id.btn_set_budget);
        Button btnSync = view.findViewById(R.id.btn_sync_now);
        Button btnCharts = view.findViewById(R.id.btn_view_charts);
        Button btnLogout = view.findViewById(R.id.btn_logout);

        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();
            
            if (name != null && !name.isEmpty()) {
                tvName.setText(name);
            }
            tvEmail.setText(email);
        }

        btnBudget.setOnClickListener(v -> startActivity(new Intent(getActivity(), BudgetActivity.class)));

        btnSync.setOnClickListener(v -> {
            FirebaseSyncManager.getInstance(getContext()).syncExpenses();
            FirebaseSyncManager.getInstance(getContext()).syncDocuments();
            Toast.makeText(getContext(), "Sync Started", Toast.LENGTH_SHORT).show();
        });

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

        SwitchMaterial switchAppLock = view.findViewById(R.id.switch_app_lock);
        Button btnChangePin = view.findViewById(R.id.btn_change_pin);

        switchAppLock.setChecked(SecurityUtils.isLockEnabled(getContext()));
        btnChangePin.setVisibility(SecurityUtils.isLockEnabled(getContext()) ? View.VISIBLE : View.GONE);

        switchAppLock.setOnClickListener(v -> {
            boolean isCurrentlyEnabled = SecurityUtils.isLockEnabled(getContext());
            Intent intent = new Intent(getActivity(), PinActivity.class);
            if (isCurrentlyEnabled) {
                // User wants to disable
                intent.putExtra("mode", "DISABLE");
                // Reset toggle state until verified
                switchAppLock.setChecked(true);
            } else {
                // User wants to enable
                intent.putExtra("mode", "SET");
                switchAppLock.setChecked(false);
            }
            startActivity(intent);
        });

        btnChangePin.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PinActivity.class);
            intent.putExtra("mode", "CHANGE");
            startActivity(intent);
        });

        SwitchMaterial switchBiometric = view.findViewById(R.id.switch_biometric);
        switchBiometric.setChecked(SecurityUtils.isBiometricEnabled(getContext()));

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (SecurityUtils.isLockEnabled(getContext())) {
                    SecurityUtils.setBiometricEnabled(getContext(), true);
                    Toast.makeText(getContext(), "Biometric Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    switchBiometric.setChecked(false);
                    Toast.makeText(getContext(), "Please set PIN first", Toast.LENGTH_SHORT).show();
                }
            } else {
                SecurityUtils.setBiometricEnabled(getContext(), false);
                Toast.makeText(getContext(), "Biometric Disabled", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btn_export_data).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ExportActivity.class));
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLockButtons();
        
        SwitchMaterial switchBiometric = getView().findViewById(R.id.switch_biometric);
        if (switchBiometric != null) {
            switchBiometric.setChecked(SecurityUtils.isBiometricEnabled(getContext()));
        }

        SwitchMaterial switchAppLock = getView().findViewById(R.id.switch_app_lock);
        if (switchAppLock != null) {
            switchAppLock.setChecked(SecurityUtils.isLockEnabled(getContext()));
        }
    }

    private void updateLockButtons() {
        View view = getView();
        if (view == null) return;

        SwitchMaterial switchAppLock = view.findViewById(R.id.switch_app_lock);
        Button btnChangePin = view.findViewById(R.id.btn_change_pin);

        boolean isEnabled = SecurityUtils.isLockEnabled(getContext());
        if (switchAppLock != null) {
            switchAppLock.setChecked(isEnabled);
        }
        if (btnChangePin != null) {
            btnChangePin.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        }
    }
}
