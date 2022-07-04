package com.example.elegantcount.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "drugs"
)
data class Drug(
    @PrimaryKey
    @NonNull
    val id: String,
    val name: String?
): Serializable
