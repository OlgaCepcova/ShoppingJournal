package com.example.shoppingjournal.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ShoppingItemDao {

    @Insert
    long insert(ShoppingItem item);

    @Query("SELECT * FROM shopping_items ORDER BY id DESC")
    List<ShoppingItem> getAll();

    @Query("SELECT SUM(price) FROM shopping_items")
    Double getTotal();
}


