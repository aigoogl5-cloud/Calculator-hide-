package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "PHOTO", "VIDEO", "FILE"
    val title: String,
    val originalName: String,
    val filePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val albumName: String = "Default",
    val durationSeconds: Int = 0,
    val isDecoy: Boolean = false
)
