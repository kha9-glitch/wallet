package com.example.smartwallet.firebase;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.smartwallet.database.AppDatabase;
import com.example.smartwallet.models.Document;
import com.example.smartwallet.models.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class FirebaseSyncManager {
    private static FirebaseSyncManager instance;
    private DatabaseReference dbRef;
    private StorageReference storageRef;
    private AppDatabase localDb;
    private String userId;

    private FirebaseSyncManager(Context context) {
        dbRef = FirebaseDatabase.getInstance().getReference();
        storageRef = FirebaseStorage.getInstance().getReference();
        localDb = AppDatabase.getInstance(context);
        userId = FirebaseAuth.getInstance().getUid();
    }

    public static synchronized FirebaseSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseSyncManager(context);
        }
        return instance;
    }

    public void syncExpenses() {
        if (userId == null)
            userId = FirebaseAuth.getInstance().getUid();
        if (userId == null)
            return;

        List<Expense> unsynced = localDb.expenseDao().getUnsyncedExpenses();
        for (Expense expense : unsynced) {
            DatabaseReference newRef;
            if (expense.getFirebaseId() != null && !expense.getFirebaseId().isEmpty()) {
                newRef = dbRef.child("users").child(userId).child("expenses").child(expense.getFirebaseId());
            } else {
                newRef = dbRef.child("users").child(userId).child("expenses").push();
                expense.setFirebaseId(newRef.getKey());
            }

            expense.setSynced(true);
            newRef.setValue(expense).addOnSuccessListener(aVoid -> {
                localDb.expenseDao().markSynced(expense.getId(), expense.getFirebaseId());
            });
        }
    }

    public void syncDocuments() {
        if (userId == null)
            userId = FirebaseAuth.getInstance().getUid();
        if (userId == null)
            return;

        List<Document> unsynced = localDb.documentDao().getUnsyncedDocuments();
        for (Document doc : unsynced) {
            if (doc.getFilePath() != null && !doc.getFilePath().isEmpty() && !doc.getFilePath().startsWith("http")) {
                Uri fileUri = Uri.parse(doc.getFilePath());
                StorageReference fileRef = storageRef.child("users").child(userId).child("docs")
                        .child(doc.getDocumentName() + "_" + System.currentTimeMillis());
                fileRef.putFile(fileUri).addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        doc.setFilePath(uri.toString());
                        uploadDocToDb(doc);
                    });
                });
            } else {
                uploadDocToDb(doc);
            }
        }
    }

    private void uploadDocToDb(Document doc) {
        DatabaseReference newRef;
        if (doc.getFirebaseId() != null && !doc.getFirebaseId().isEmpty()) {
            newRef = dbRef.child("users").child(userId).child("documents").child(doc.getFirebaseId());
        } else {
            newRef = dbRef.child("users").child(userId).child("documents").push();
            doc.setFirebaseId(newRef.getKey());
        }

        doc.setSynced(true);
        newRef.setValue(doc).addOnSuccessListener(aVoid -> {
            localDb.documentDao().markSynced(doc.getId(), doc.getFirebaseId());
        });
    }

    public void downloadAllData() {
        if (userId == null)
            userId = FirebaseAuth.getInstance().getUid();
        if (userId == null)
            return;

        dbRef.child("users").child(userId).child("expenses").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Expense firebaseExp = ds.getValue(Expense.class);
                    if (firebaseExp != null) {
                        firebaseExp.setSynced(true);
                        // We check if it exists locally by firebaseId (would need a DAO method, but for
                        // simplicity we rely on manual care)
                        localDb.expenseDao().insert(firebaseExp);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        dbRef.child("users").child(userId).child("documents").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Document firebaseDoc = ds.getValue(Document.class);
                    if (firebaseDoc != null) {
                        firebaseDoc.setSynced(true);
                        localDb.documentDao().insert(firebaseDoc);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
