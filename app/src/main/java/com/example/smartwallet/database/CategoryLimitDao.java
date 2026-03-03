package com.example.smartwallet.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.smartwallet.models.CategoryLimit;

import java.util.List;

@Dao
public interface CategoryLimitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(CategoryLimit limit);

    @Update
    void update(CategoryLimit limit);

    @Delete
    void delete(CategoryLimit limit);

    @Query("SELECT * FROM category_limits WHERE userId = :userId")
    List<CategoryLimit> getAllLimits(String userId);

    @Query("SELECT * FROM category_limits WHERE userId = :userId AND category = :category LIMIT 1")
    CategoryLimit getLimitForCategory(String userId, String category);

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND category = :category AND date LIKE :month || '%'")
    double getCategoryTotal(String userId, String category, String month);
}
