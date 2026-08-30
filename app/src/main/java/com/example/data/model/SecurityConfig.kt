package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_config")
data class SecurityConfig(
    @PrimaryKey val id: Int = 1,
    val passcodeHash: String = "",
    val decoyPasscodeHash: String = "",
    val biometricEnabled: Boolean = true,
    val securityQuestion: String = "What is your secret master number?",
    val securityAnswerHash: String = "",
    val isConfigured: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true
)
