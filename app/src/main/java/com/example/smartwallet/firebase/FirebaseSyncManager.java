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
        // Explicitly set the database URL to fix connection issues
        dbRef = FirebaseDatabase.getInstance("https://smart-wallet-975e9-default-rtdb.firebaseio.com/").getReference();
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

        new Thread(() -> {
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
                    new Thread(() -> localDb.expenseDao().markSynced(expense.getId(), expense.getFirebaseId())).start();
                });
            }
        }).start();
    }

    public void syncDocuments() {
        if (userId == null)
            userId = FirebaseAuth.getInstance().getUid();
        if (userId == null)
            return;

        new Thread(() -> {
            List<Document> unsynced = localDb.documentDao().getUnsyncedDocuments();
            for (Document doc : unsynced) {
                String path = doc.getFilePath();
                if (path != null && !path.isEmpty() && !path.startsWith("http")) {
                    Uri fileUri;
                    if (path.startsWith("content://")) {
                        fileUri = Uri.parse(path);
                    } else {
                        fileUri = Uri.fromFile(new java.io.File(path));
                    }
                    
                    StorageReference fileRef = storageRef.child("users").child(userId).child("docs")
                            .child(doc.getDocumentName() + "_" + System.currentTimeMillis());
                    try {
                        fileRef.putFile(fileUri).addOnSuccessListener(taskSnapshot -> {
                            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                doc.setFilePath(uri.toString());
                                uploadDocToDb(doc);
                            });
                        }).addOnFailureListener(e -> {
                            e.printStackTrace();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Skip this document if file access fails
                    }
                } else {
                    uploadDocToDb(doc);
                }
            }
        }).start();
    }

    private void uploadDocToDb(Document doc) {
        new Thread(() -> {
            DatabaseReference newRef;
            if (doc.getFirebaseId() != null && !doc.getFirebaseId().isEmpty()) {
                newRef = dbRef.child("users").child(userId).child("documents").child(doc.getFirebaseId());
            } else {
                newRef = dbRef.child("users").child(userId).child("documents").push();
                doc.setFirebaseId(newRef.getKey());
            }

            doc.setSynced(true);
            newRef.setValue(doc).addOnSuccessListener(aVoid -> {
                new Thread(() -> localDb.documentDao().markSynced(doc.getId(), doc.getFirebaseId())).start();
            });
        }).start();
    }

    public void downloadAllData() {
        if (userId == null)
            userId = FirebaseAuth.getInstance().getUid();
        if (userId == null)
            return;

        dbRef.child("users").child(userId).child("expenses").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                new Thread(() -> {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Expense firebaseExp = ds.getValue(Expense.class);
                        if (firebaseExp != null && firebaseExp.getFirebaseId() != null) {
                            Expense existing = localDb.expenseDao().getExpenseByFirebaseId(userId, firebaseExp.getFirebaseId());
                            if (existing == null) {
                                firebaseExp.setSynced(true);
                                localDb.expenseDao().insert(firebaseExp);
                            }
                        }
                    }
                }).start();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        dbRef.child("users").child(userId).child("documents").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                new Thread(() -> {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Document firebaseDoc = ds.getValue(Document.class);
                        if (firebaseDoc != null && firebaseDoc.getFirebaseId() != null) {
                            Document existing = localDb.documentDao().getDocumentByFirebaseId(userId, firebaseDoc.getFirebaseId());
                            if (existing == null) {
                                firebaseDoc.setSynced(true);
                                localDb.documentDao().insert(firebaseDoc);
                            }
                        }
                    }
                }).start();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void startRealTimeSync() {
        if (userId == null) userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        // Real-time listener for Expenses
        dbRef.child("users").child(userId).child("expenses").addChildEventListener(new com.google.firebase.database.ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Expense firebaseExp = snapshot.getValue(Expense.class);
                if (firebaseExp != null && firebaseExp.getFirebaseId() != null) {
                    new Thread(() -> {
                        Expense existing = localDb.expenseDao().getExpenseByFirebaseId(userId, firebaseExp.getFirebaseId());
                        if (existing == null) {
                            firebaseExp.setSynced(true);
                            localDb.expenseDao().insert(firebaseExp);
                        }
                    }).start();
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Real-time listener for Documents
        dbRef.child("users").child(userId).child("documents").addChildEventListener(new com.google.firebase.database.ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Document firebaseDoc = snapshot.getValue(Document.class);
                if (firebaseDoc != null && firebaseDoc.getFirebaseId() != null) {
                    new Thread(() -> {
                        Document existing = localDb.documentDao().getDocumentByFirebaseId(userId, firebaseDoc.getFirebaseId());
                        if (existing == null) {
                            firebaseDoc.setSynced(true);
                            localDb.documentDao().insert(firebaseDoc);
                        }
                    }).start();
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
