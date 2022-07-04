package com.example.elegantcount.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "products"
)
data class Product(
    @PrimaryKey
    @NonNull
    val pId: String,
    val name: String?,
    val periodNo: String?,
    val expireDate: String?,
    val boxNumber: String?
): Serializable
