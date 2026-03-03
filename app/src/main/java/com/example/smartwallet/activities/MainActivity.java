package com.example.smartwallet.activities;

import android.os.Bundle;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.smartwallet.R;
import com.example.smartwallet.activities.PinActivity;
import com.example.smartwallet.databinding.ActivityMainBinding;
import com.example.smartwallet.workers.ExpiryWorker;
import com.example.smartwallet.utils.SecurityUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    public static boolean isPinVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SecurityUtils.isLockEnabled(this) && !isPinVerified) {
            Intent intent = new Intent(this, PinActivity.class);
            intent.putExtra("mode", "ENTER");
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_dashboard, R.id.navigation_expenses, 
                R.id.navigation_documents, R.id.navigation_charts, R.id.navigation_account)
                .build();
                
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.navView, navController);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Start Real-Time Sync (Feature 5)
        com.example.smartwallet.firebase.FirebaseSyncManager syncManager = com.example.smartwallet.firebase.FirebaseSyncManager.getInstance(this);
        syncManager.downloadAllData();
        syncManager.startRealTimeSync();

        scheduleExpiryCheck();
        scheduleSync();
    }

    private void scheduleSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                com.example.smartwallet.workers.SyncWorker.class,
                24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "CloudSync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }

    private void scheduleExpiryCheck() {
        PeriodicWorkRequest expiryWorkRequest = new PeriodicWorkRequest.Builder(
                ExpiryWorker.class, 1, TimeUnit.DAYS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ExpiryCheckWork",
                ExistingPeriodicWorkPolicy.KEEP,
                expiryWorkRequest
        );
    }
}
