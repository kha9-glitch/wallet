package com.example.smartwallet.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.models.Expense;
import com.example.smartwallet.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) return;

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        for (Object pdu : pdus) {
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
            String messageBody = smsMessage.getMessageBody();
            String address = smsMessage.getOriginatingAddress();

            Log.d(TAG, "SMS Received from: " + address + ", Body: " + messageBody);

            if (isTransactionSMS(messageBody)) {
                processTransaction(context, messageBody);
            }
        }
    }

    private boolean isTransactionSMS(String body) {
        String lowerBody = body.toLowerCase();
        return (lowerBody.contains("debited") || lowerBody.contains("spent") || lowerBody.contains("withdrawn") || lowerBody.contains("transaction"))
                && (lowerBody.contains("rs") || lowerBody.contains("inr") || lowerBody.contains("amt"));
    }

    private void processTransaction(Context context, String body) {
        try {
            double amount = extractAmount(body);
            if (amount <= 0) return;

            String userId = FirebaseAuth.getInstance().getUid();
            if (userId == null) return;

            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            Expense expense = new Expense();
            expense.setUserId(userId);
            expense.setAmount(amount);
            expense.setDate(date);
            expense.setCategory("Other");
            expense.setPaymentMode("Bank");
            expense.setNotes("Auto-detected from SMS: " + body);
            expense.setSynced(false);

            new Thread(() -> {
                AppDatabase.getInstance(context).expenseDao().insert(expense);
                Log.d(TAG, "Auto Expense Saved: " + amount);
            }).start();

            NotificationHelper.showNotification(context, "Auto Expense Detected", "Added ₹" + amount + " to expenses.");

        } catch (Exception e) {
            Log.e(TAG, "Error processing transaction", e);
        }
    }

    private double extractAmount(String body) {
        // Look for patterns like Rs. 500, Rs 500, INR 500, 500.00 debited
        Pattern pattern = Pattern.compile("(?i)(?:rs|inr|amt)\\.?\\s*([\\d,]+\\.?\\d*)");
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            String amtStr = matcher.group(1).replace(",", "");
            return Double.parseDouble(amtStr);
        }
        return 0;
    }
}
