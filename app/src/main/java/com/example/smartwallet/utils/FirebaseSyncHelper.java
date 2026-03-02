package com.example.smartwallet.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseSyncHelper {
    public static void syncBudget(String userId, double amount) {
        if (userId == null) userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(userId)
                    .child("budget")
                    .setValue(amount);
        }
    }
}
