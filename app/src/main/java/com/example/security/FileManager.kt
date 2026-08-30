package com.example.security

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object FileManager {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[index])
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    suspend fun importUriToVault(
        context: Context,
        uri: Uri,
        type: String, // "PHOTO", "VIDEO", "FILE"
        albumName: String = "Default"
    ): VaultItem? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val originalName = queryFileName(contentResolver, uri) ?: "file_${System.currentTimeMillis()}"
            val mimeType = contentResolver.getType(uri) ?: when (type) {
                "PHOTO" -> "image/jpeg"
                "VIDEO" -> "video/mp4"
                else -> "application/octet-stream"
            }

            // Create vault subfolder
            val vaultDir = File(context.filesDir, "vault_${type.lowercase()}")
            if (!vaultDir.exists()) vaultDir.mkdirs()

            val ext = originalName.substringAfterLast('.', "")
            val safeFileName = "enc_${UUID.randomUUID()}.${if (ext.isNotEmpty()) ext else "dat"}"
            val destFile = File(vaultDir, safeFileName)

            var fileSize = 0L
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        fileSize += read
                    }
                }
            }

            VaultItem(
                type = type,
                title = originalName.substringBeforeLast('.'),
                originalName = originalName,
                filePath = destFile.absolutePath,
                mimeType = mimeType,
                sizeBytes = fileSize,
                albumName = albumName
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun createTextSecretFile(
        context: Context,
        title: String,
        content: String
    ): VaultItem? = withContext(Dispatchers.IO) {
        try {
            val vaultDir = File(context.filesDir, "vault_file")
            if (!vaultDir.exists()) vaultDir.mkdirs()

            val safeFileName = "secret_doc_${UUID.randomUUID()}.txt"
            val destFile = File(vaultDir, safeFileName)
            destFile.writeText(content, Charsets.UTF_8)

            VaultItem(
                type = "FILE",
                title = title,
                originalName = "$title.txt",
                filePath = destFile.absolutePath,
                mimeType = "text/plain",
                sizeBytes = destFile.length(),
                albumName = "Documents"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteVaultFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun queryFileName(resolver: ContentResolver, uri: Uri): String? {
        return try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
