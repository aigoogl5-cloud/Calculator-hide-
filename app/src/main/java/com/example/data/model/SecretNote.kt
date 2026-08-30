package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secret_notes")
data class SecretNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "General", // "General", "Passwords", "Finance", "Diary", "Private"
    val colorHex: Long = 0xFF21262D,
    val updatedTimestamp: Long = System.currentTimeMillis(),
    val isDecoy: Boolean = false
)
