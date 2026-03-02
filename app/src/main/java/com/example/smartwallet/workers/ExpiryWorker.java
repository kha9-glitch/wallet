package com.example.smartwallet.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.models.Document;
import com.example.smartwallet.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpiryWorker extends Worker {

    public ExpiryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return Result.success();

        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        List<Document> documents = db.documentDao().getAllDocuments(userId);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        for (Document doc : documents) {
            if (doc.getExpiryDate() != null && !doc.getExpiryDate().isEmpty()) {
                try {
                    Date expiryDate = sdf.parse(doc.getExpiryDate());
                    if (expiryDate != null) {
                        long diff = expiryDate.getTime() - today.getTimeInMillis();
                        long days = diff / (24 * 60 * 60 * 1000);

                        if (days == 30 || days == 7 || days == 1) {
                            String message = doc.getName() + " will expire in " + days + " days";
                            NotificationHelper.showNotification(getApplicationContext(), "Document Expiry", message, doc.getId());
                        } else if (days == 0) {
                            String message = doc.getName() + " expires today!";
                            NotificationHelper.showNotification(getApplicationContext(), "Document Expiry", message, doc.getId());
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        return Result.success();
    }
}
