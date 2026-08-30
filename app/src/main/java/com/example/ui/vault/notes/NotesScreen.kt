package com.example.ui.vault.notes

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import com.example.data.model.SecretNote
import com.example.security.FileManager
import com.example.ui.MainViewModel
import com.example.ui.VaultUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.VaultSectionHeader
import com.example.ui.theme.*

val NoteColorPalette = listOf(
    0xFF161B22, // Graphite Default
    0xFF1E2D3B, // Deep Navy
    0xFF1C2E24, // Emerald Forest
    0xFF362419, // Deep Amber
    0xFF331C2D, // Velvet Berry
    0xFF2A1C36  // Royal Violet
)

@Composable
fun NotesScreen(
    viewModel: MainViewModel,
    vaultState: VaultUiState,
    notes: List<SecretNote>
) {
    val filteredNotes = remember(notes, vaultState.searchQuery, vaultState.activeFilterCategory) {
        notes.filter {
            if (vaultState.activeFilterCategory != "All" && vaultState.activeFilterCategory != "Favorites") {
                it.category == vaultState.activeFilterCategory
            } else true
        }.filter {
            if (vaultState.searchQuery.isNotBlank()) {
                it.title.contains(vaultState.searchQuery, ignoreCase = true) ||
                        it.content.contains(vaultState.searchQuery, ignoreCase = true)
            } else true
        }
    }

    val categories = listOf("All", "Passwords", "Finance", "Diary", "General")

    Column(modifier = Modifier.fillMaxSize()) {
        VaultSectionHeader(
            title = "Secret Notes & Passwords",
            count = filteredNotes.size,
            subtitle = "Encrypted private memos and credentials"
        )

        // Categories chip row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = (vaultState.activeFilterCategory == category) || (category == "All" && vaultState.activeFilterCategory !in categories.drop(1)),
                    onClick = { viewModel.setCategoryFilter(category) },
                    label = { Text(category, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentAmber,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextSecondary
                    ),
                    border = null
                )
            }
        }

        if (filteredNotes.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.Lock,
                title = "No Secret Notes",
                description = "Create protected notes for credit card PINs, crypto keys, journal entries, or private thoughts.",
                actionLabel = "Create First Note",
                onAction = { viewModel.setDialogVisibility(addNote = true) }
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(150.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalItemSpacing = 10.dp,
                verticalItemSpacing = 10.dp,
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    SecretNoteCard(
                        note = note,
                        onClick = { viewModel.selectNote(note) },
                        onDelete = { viewModel.deleteCurrentNote(note) }
                    )
                }
            }
        }
    }

    // Add or Edit Note Dialog
    if (vaultState.isAddNoteDialogVisible || vaultState.isEditingNote) {
        NoteEditorDialog(
            initialNote = vaultState.selectedNote,
            onDismiss = {
                viewModel.setDialogVisibility(addNote = false)
                viewModel.setEditingNote(false)
                viewModel.selectNote(null)
            },
            onSave = { title, content, category, colorHex ->
                viewModel.saveNote(title, content, category, colorHex)
            },
            onDelete = if (vaultState.selectedNote != null) {
                { viewModel.deleteCurrentNote(vaultState.selectedNote!!) }
            } else null
        )
    }
}

@Composable
fun SecretNoteCard(
    note: SecretNote,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(note.colorHex),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("note_card_${note.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x33FFFFFF)
                ) {
                    Text(
                        text = note.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = FileManager.formatDate(note.updatedTimestamp),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun NoteEditorDialog(
    initialNote: SecretNote?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String, colorHex: Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var category by remember { mutableStateOf(initialNote?.category ?: "General") }
    var selectedColor by remember { mutableStateOf(initialNote?.colorHex ?: NoteColorPalette.first()) }

    val categories = listOf("General", "Passwords", "Finance", "Diary", "Private")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(selectedColor),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (initialNote == null) "New Secret Note" else "Edit Secret Note",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRose)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    placeholder = { Text("e.g. Master Vault Password") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentAmber,
                        unfocusedBorderColor = BorderGlass,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentAmber,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                // Color picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NoteColorPalette.forEach { colorVal ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    if (selectedColor == colorVal) 2.dp else 1.dp,
                                    if (selectedColor == colorVal) AccentAmber else BorderGlass,
                                    CircleShape
                                )
                                .clickable { selectedColor = colorVal }
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Content") },
                    placeholder = { Text("Write encrypted text here...") },
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentAmber,
                        unfocusedBorderColor = BorderGlass,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() || content.isNotBlank()) {
                        onSave(
                            if (title.isBlank()) "Untitled Note" else title.trim(),
                            content.trim(),
                            category,
                            selectedColor
                        )
                    }
                },
                enabled = title.isNotBlank() || content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Color.Black)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
