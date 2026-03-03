package com.example.smartwallet.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.smartwallet.firebase.FirebaseSyncManager;
import com.google.firebase.auth.FirebaseAuth;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting Auto Cloud Backup...");
        
        if (FirebaseAuth.getInstance().getUid() == null) {
            return Result.failure();
        }

        FirebaseSyncManager syncManager = FirebaseSyncManager.getInstance(getApplicationContext());
        try {
            syncManager.syncExpenses();
            syncManager.syncDocuments();
            Log.d(TAG, "Cloud Backup successful");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Cloud Backup failed", e);
            return Result.retry();
        }
    }
}
