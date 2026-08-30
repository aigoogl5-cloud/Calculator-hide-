package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.VaultDao
import com.example.data.model.SecretNote
import com.example.data.model.SecurityConfig
import com.example.data.model.SecurityLog
import com.example.data.model.VaultItem
import com.example.security.CryptoUtils
import com.example.security.FileManager
import kotlinx.coroutines.flow.Flow

class VaultRepository(
    private val vaultDao: VaultDao,
    private val context: Context
) {
    // Flow of items
    fun getVaultItems(type: String? = null, isDecoy: Boolean = false): Flow<List<VaultItem>> {
        return if (type == null) {
            vaultDao.getAllVaultItems(isDecoy)
        } else {
            vaultDao.getVaultItemsByType(type, isDecoy)
        }
    }

    fun getFavoriteVaultItems(isDecoy: Boolean = false): Flow<List<VaultItem>> {
        return vaultDao.getFavoriteVaultItems(isDecoy)
    }

    suspend fun importMedia(uri: Uri, type: String, albumName: String = "Default", isDecoy: Boolean = false): Boolean {
        val vaultItem = FileManager.importUriToVault(context, uri, type, albumName)
        return if (vaultItem != null) {
            val finalItem = if (isDecoy) vaultItem.copy(isDecoy = true) else vaultItem
            vaultDao.insertVaultItem(finalItem)
            true
        } else {
            false
        }
    }

    suspend fun createSecretDocument(title: String, content: String, isDecoy: Boolean = false): Boolean {
        val vaultItem = FileManager.createTextSecretFile(context, title, content)
        return if (vaultItem != null) {
            val finalItem = if (isDecoy) vaultItem.copy(isDecoy = true) else vaultItem
            vaultDao.insertVaultItem(finalItem)
            true
        } else {
            false
        }
    }

    suspend fun toggleFavorite(item: VaultItem) {
        vaultDao.updateVaultItem(item.copy(isFavorite = !item.isFavorite))
    }

    suspend fun deleteVaultItem(item: VaultItem) {
        FileManager.deleteVaultFile(item.filePath)
        vaultDao.deleteVaultItemById(item.id)
    }

    // Notes
    fun getSecretNotes(isDecoy: Boolean = false): Flow<List<SecretNote>> = vaultDao.getAllNotes(isDecoy)

    suspend fun saveNote(note: SecretNote): Long = vaultDao.insertNote(note)

    suspend fun deleteNote(note: SecretNote) = vaultDao.deleteNote(note)

    // Security logs
    fun getSecurityLogs(): Flow<List<SecurityLog>> = vaultDao.getAllLogs()

    suspend fun logSecurityEvent(eventType: String, details: String, wasSuccessful: Boolean) {
        vaultDao.insertLog(
            SecurityLog(
                eventType = eventType,
                details = details,
                wasSuccessful = wasSuccessful
            )
        )
    }

    suspend fun clearLogs() = vaultDao.clearLogs()

    // Config
    fun getSecurityConfigFlow(): Flow<SecurityConfig?> = vaultDao.getSecurityConfigFlow()

    suspend fun getSecurityConfig(): SecurityConfig {
        var config = vaultDao.getSecurityConfig()
        if (config == null) {
            config = SecurityConfig()
            vaultDao.saveSecurityConfig(config)
        }
        return config
    }

    suspend fun setMasterPasscode(passcode: String, securityQuestion: String = "", securityAnswer: String = "") {
        val current = getSecurityConfig()
        val updated = current.copy(
            passcodeHash = CryptoUtils.hashString(passcode),
            isConfigured = true,
            securityQuestion = if (securityQuestion.isNotBlank()) securityQuestion else current.securityQuestion,
            securityAnswerHash = if (securityAnswer.isNotBlank()) CryptoUtils.hashString(securityAnswer.trim().lowercase()) else current.securityAnswerHash
        )
        vaultDao.saveSecurityConfig(updated)
        logSecurityEvent("PASSCODE_CHANGED", "Master passcode configured or updated", true)
    }

    suspend fun setDecoyPasscode(decoyPasscode: String) {
        val current = getSecurityConfig()
        val updated = current.copy(
            decoyPasscodeHash = if (decoyPasscode.isNotBlank()) CryptoUtils.hashString(decoyPasscode) else ""
        )
        vaultDao.saveSecurityConfig(updated)
    }

    suspend fun updateBiometricSetting(enabled: Boolean) {
        val current = getSecurityConfig()
        vaultDao.saveSecurityConfig(current.copy(biometricEnabled = enabled))
    }

    suspend fun updateHaptics(vibration: Boolean, sound: Boolean) {
        val current = getSecurityConfig()
        vaultDao.saveSecurityConfig(current.copy(vibrationEnabled = vibration, soundEnabled = sound))
    }
}
