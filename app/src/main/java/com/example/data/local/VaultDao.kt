package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SecretNote
import com.example.data.model.SecurityConfig
import com.example.data.model.SecurityLog
import com.example.data.model.VaultItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    // Vault Items
    @Query("SELECT * FROM vault_items WHERE isDecoy = :isDecoy ORDER BY addedTimestamp DESC")
    fun getAllVaultItems(isDecoy: Boolean = false): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE type = :type AND isDecoy = :isDecoy ORDER BY addedTimestamp DESC")
    fun getVaultItemsByType(type: String, isDecoy: Boolean = false): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE isFavorite = 1 AND isDecoy = :isDecoy ORDER BY addedTimestamp DESC")
    fun getFavoriteVaultItems(isDecoy: Boolean = false): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getVaultItemById(id: Long): VaultItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItem): Long

    @Update
    suspend fun updateVaultItem(item: VaultItem)

    @Delete
    suspend fun deleteVaultItem(item: VaultItem)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItemById(id: Long)

    // Secret Notes
    @Query("SELECT * FROM secret_notes WHERE isDecoy = :isDecoy ORDER BY updatedTimestamp DESC")
    fun getAllNotes(isDecoy: Boolean = false): Flow<List<SecretNote>>

    @Query("SELECT * FROM secret_notes WHERE id = :id")
    suspend fun getNoteById(id: Long): SecretNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: SecretNote): Long

    @Update
    suspend fun updateNote(note: SecretNote)

    @Delete
    suspend fun deleteNote(note: SecretNote)

    // Security Logs
    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<SecurityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SecurityLog): Long

    @Query("DELETE FROM security_logs")
    suspend fun clearLogs()

    // Security Config
    @Query("SELECT * FROM security_config WHERE id = 1")
    fun getSecurityConfigFlow(): Flow<SecurityConfig?>

    @Query("SELECT * FROM security_config WHERE id = 1")
    suspend fun getSecurityConfig(): SecurityConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSecurityConfig(config: SecurityConfig)
}
