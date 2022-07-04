package com.example.elegantcount.db

import androidx.room.*
import com.example.elegantcount.model.Product

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateOrInsert(product: Product): Long

    @Query("SELECT * FROM products")
    fun getAllProducts(): List<Product>

    @Delete
    fun deleteProduct(product: Product)

    @Query("DELETE FROM products")
    fun deleteAllData()
}