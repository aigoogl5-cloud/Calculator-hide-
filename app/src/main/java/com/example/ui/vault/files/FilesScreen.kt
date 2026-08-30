package com.example.ui.vault.files

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.VaultItem
import com.example.security.FileManager
import com.example.ui.MainViewModel
import com.example.ui.VaultUiState
import com.example.ui.components.CreateSecretDocumentDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.VaultSectionHeader
import com.example.ui.theme.*
import java.io.File

@Composable
fun FilesScreen(
    viewModel: MainViewModel,
    vaultState: VaultUiState,
    items: List<VaultItem>
) {
    val fileItems = remember(items, vaultState.searchQuery) {
        items.filter { it.type == "FILE" }
            .filter {
                if (vaultState.searchQuery.isNotBlank()) {
                    it.title.contains(vaultState.searchQuery, ignoreCase = true)
                } else true
            }
    }

    var selectedFileForPreview by remember { mutableStateOf<VaultItem?>(null) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMediaUris(uris, "FILE")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        VaultSectionHeader(
            title = "Secret Files & Docs",
            count = fileItems.size,
            subtitle = "Encrypted documents, archives & confidential notes"
        )

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.setDialogVisibility(addFile = true) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Secret Doc", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { filePickerLauncher.launch("*/*") },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Import File")
            }
        }

        if (fileItems.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.FolderZip,
                title = "No Hidden Files",
                description = "Keep contracts, seed phrases, recovery keys, spreadsheets, and PDFs protected inside the secret vault.",
                actionLabel = "Create Secret File",
                onAction = { viewModel.setDialogVisibility(addFile = true) }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(fileItems, key = { it.id }) { item ->
                    FileListItem(
                        item = item,
                        onClick = { selectedFileForPreview = item },
                        onDelete = { viewModel.deleteCurrentItem(item) }
                    )
                }
            }
        }
    }

    // Create Secret Document Dialog
    if (vaultState.isAddFileDialogVisible) {
        CreateSecretDocumentDialog(
            onDismiss = { viewModel.setDialogVisibility(addFile = false) },
            onSave = { title, content ->
                viewModel.createSecretDocument(title, content)
            }
        )
    }

    // View text document dialog
    if (selectedFileForPreview != null) {
        val currentFile = selectedFileForPreview!!
        val fileContent = remember(currentFile.filePath) {
            try {
                val f = File(currentFile.filePath)
                if (f.exists() && (currentFile.mimeType.contains("text") || currentFile.originalName.endsWith(".txt"))) {
                    f.readText(Charsets.UTF_8)
                } else null
            } catch (e: Exception) {
                null
            }
        }

        AlertDialog(
            onDismissRequest = { selectedFileForPreview = null },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = AccentAmber)
                    Text(currentFile.title, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (fileContent != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                        ) {
                            Text(
                                text = fileContent,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    Text("Original Name: ${currentFile.originalName}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("Size: ${FileManager.formatFileSize(currentFile.sizeBytes)}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("Encrypted Storage: ${FileManager.formatDate(currentFile.addedTimestamp)}", color = AccentEmerald, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedFileForPreview = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Color.Black)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.deleteCurrentItem(currentFile)
                    selectedFileForPreview = null
                }) {
                    Text("Delete", color = AccentRose)
                }
            }
        )
    }
}

@Composable
fun FileListItem(
    item: VaultItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("file_item_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurfaceElevated,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when {
                            item.mimeType.contains("pdf") || item.originalName.endsWith(".pdf") -> Icons.Default.PictureAsPdf
                            item.mimeType.contains("audio") -> Icons.Default.AudioFile
                            item.mimeType.contains("zip") || item.originalName.endsWith(".zip") -> Icons.Default.FolderZip
                            else -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = FileManager.formatFileSize(item.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    Text(
                        text = FileManager.formatDate(item.addedTimestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondary)
            }
        }
    }
}
