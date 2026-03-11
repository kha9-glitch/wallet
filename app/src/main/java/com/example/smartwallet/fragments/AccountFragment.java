package com.example.smartwallet.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;
import com.example.smartwallet.utils.CurrencyUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.smartwallet.R;
import com.example.smartwallet.activities.BudgetActivity;
import com.example.smartwallet.activities.ExportActivity;
import com.example.smartwallet.activities.LoginActivity;
import com.example.smartwallet.activities.PinActivity;
import com.example.smartwallet.databinding.FragmentAccountBinding;
import com.example.smartwallet.firebase.FirebaseSyncManager;
import com.example.smartwallet.utils.SecurityUtils;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();
            
            if (name != null && !name.isEmpty()) {
                binding.tvUserName.setText(name);
            } else if (email != null) {
                binding.tvUserName.setText(email.split("@")[0]);
            }
            binding.tvUserEmail.setVisibility(View.GONE);
        }

        binding.btnSetBudget.setOnClickListener(v -> startActivity(new Intent(getActivity(), BudgetActivity.class)));

        binding.btnSyncNow.setOnClickListener(v -> {
            FirebaseSyncManager.getInstance(getContext()).syncExpenses();
            FirebaseSyncManager.getInstance(getContext()).syncDocuments();
            Toast.makeText(getContext(), "Sync Started", Toast.LENGTH_SHORT).show();
        });

        binding.btnViewCharts.setOnClickListener(v -> {
            try {
               Navigation.findNavController(v).navigate(R.id.navigation_charts);
            } catch (Exception e) {
               Toast.makeText(getContext(), "Analytics screen opening...", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        binding.switchAppLock.setChecked(SecurityUtils.isLockEnabled(getContext()));
        binding.btnChangePin.setVisibility(SecurityUtils.isLockEnabled(getContext()) ? View.VISIBLE : View.GONE);

        binding.switchAppLock.setOnClickListener(v -> {
            boolean isCurrentlyEnabled = SecurityUtils.isLockEnabled(getContext());
            Intent intent = new Intent(getActivity(), PinActivity.class);
            if (isCurrentlyEnabled) {
                // User wants to disable
                intent.putExtra("mode", "DISABLE");
                // Reset toggle state until verified
                binding.switchAppLock.setChecked(true);
            } else {
                // User wants to enable
                intent.putExtra("mode", "SET");
                binding.switchAppLock.setChecked(false);
            }
            startActivity(intent);
        });

        binding.btnChangePin.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PinActivity.class);
            intent.putExtra("mode", "CHANGE");
            startActivity(intent);
        });

        binding.switchBiometric.setChecked(SecurityUtils.isBiometricEnabled(getContext()));

        binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (SecurityUtils.isLockEnabled(getContext())) {
                    SecurityUtils.setBiometricEnabled(getContext(), true);
                    Toast.makeText(getContext(), "Biometric Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    binding.switchBiometric.setChecked(false);
                    Toast.makeText(getContext(), "Please set PIN first", Toast.LENGTH_SHORT).show();
                }
            } else {
                SecurityUtils.setBiometricEnabled(getContext(), false);
                Toast.makeText(getContext(), "Biometric Disabled", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnExportData.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ExportActivity.class));
        });

        String currentSymbol = CurrencyUtils.getCurrencySymbol(getContext());
        String[] options = getResources().getStringArray(R.array.currency_options);
        for (int i = 0; i < options.length; i++) {
            if (options[i].contains("(" + currentSymbol + ")")) {
                binding.spinnerCurrency.setSelection(i);
                break;
            }
        }

        binding.spinnerCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = options[position];
                String symbol = selected.substring(selected.indexOf("(") + 1, selected.indexOf(")"));
                if (!symbol.equals(CurrencyUtils.getCurrencySymbol(getContext()))) {
                    CurrencyUtils.setCurrencySymbol(getContext(), symbol);
                    Toast.makeText(getContext(), "Currency Updated to " + symbol, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

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
        updateLockButtons();
        
        if (binding != null) {
            binding.switchBiometric.setChecked(SecurityUtils.isBiometricEnabled(getContext()));
            binding.switchAppLock.setChecked(SecurityUtils.isLockEnabled(getContext()));
        }
    }

    private void updateLockButtons() {
        if (binding == null) return;

        boolean isEnabled = SecurityUtils.isLockEnabled(getContext());
        binding.switchAppLock.setChecked(isEnabled);
        binding.btnChangePin.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
    }
}
