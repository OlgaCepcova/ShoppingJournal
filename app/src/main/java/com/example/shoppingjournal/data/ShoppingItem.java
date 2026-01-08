package com.example.shoppingjournal.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shopping_items")
public class ShoppingItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public double price;

    public ShoppingItem(String name, double price) {
        this.name = name;
        this.price = price;
    }
}


