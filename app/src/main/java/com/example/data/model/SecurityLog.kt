package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_logs")
data class SecurityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "UNLOCKED_PASSCODE", "UNLOCKED_BIOMETRIC", "FAILED_ATTEMPT", "PASSCODE_CHANGED", "DECOY_UNLOCKED"
    val details: String,
    val wasSuccessful: Boolean
)
