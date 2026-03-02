package com.example.smartwallet.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.smartwallet.models.Document;

import java.util.List;

@Dao
public interface DocumentDao {
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    long insert(Document document);

    @Update
    void update(Document document);

    @Delete
    void delete(Document document);

    @Query("SELECT * FROM documents WHERE userId = :userId")
    List<Document> getAllDocuments(String userId);

    @Query("SELECT COUNT(*) FROM documents WHERE userId = :userId")
    int getDocumentCount(String userId);

    @Query("SELECT * FROM documents WHERE userId = :userId AND expiryDate IS NOT NULL AND expiryDate != '' ORDER BY expiryDate ASC")
    List<Document> getDocumentsByExpiry(String userId);

    @Query("SELECT * FROM documents WHERE userId = :userId AND id = :id")
    Document getDocumentById(String userId, int id);

    @Query("SELECT * FROM documents WHERE synced = 0")
    List<Document> getUnsyncedDocuments();

    @Query("UPDATE documents SET synced = 1, firebaseId = :firebaseId WHERE id = :id")
    void markSynced(int id, String firebaseId);
}
