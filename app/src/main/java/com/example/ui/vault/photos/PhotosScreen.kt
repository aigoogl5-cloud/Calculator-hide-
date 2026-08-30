package com.example.ui.vault.photos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.VaultItem
import com.example.security.FileManager
import com.example.ui.MainViewModel
import com.example.ui.VaultUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FullscreenPhotoViewerDialog
import com.example.ui.components.VaultSectionHeader
import com.example.ui.theme.*
import java.io.File

@Composable
fun PhotosScreen(
    viewModel: MainViewModel,
    vaultState: VaultUiState,
    items: List<VaultItem>
) {
    val photoItems = remember(items, vaultState.searchQuery, vaultState.activeFilterCategory) {
        items.filter { it.type == "PHOTO" }
            .filter {
                if (vaultState.activeFilterCategory == "Favorites") it.isFavorite else true
            }
            .filter {
                if (vaultState.searchQuery.isNotBlank()) {
                    it.title.contains(vaultState.searchQuery, ignoreCase = true)
                } else true
            }
    }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMediaUris(uris, "PHOTO")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Section Header & Filters
        VaultSectionHeader(
            title = "Secret Photo Vault",
            count = photoItems.size,
            subtitle = "Encrypted & hidden from device gallery"
        )

        // Filter pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = vaultState.activeFilterCategory == "All",
                onClick = { viewModel.setCategoryFilter("All") },
                label = { Text("All Photos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentAmber,
                    selectedLabelColor = Color.Black,
                    containerColor = DarkSurfaceVariant,
                    labelColor = TextSecondary
                ),
                border = null
            )
            FilterChip(
                selected = vaultState.activeFilterCategory == "Favorites",
                onClick = { viewModel.setCategoryFilter("Favorites") },
                label = { Text("Favorites") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (vaultState.activeFilterCategory == "Favorites") Color.Black else AccentRose
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentAmber,
                    selectedLabelColor = Color.Black,
                    containerColor = DarkSurfaceVariant,
                    labelColor = TextSecondary
                ),
                border = null
            )
        }

        if (photoItems.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.PhotoLibrary,
                title = if (vaultState.activeFilterCategory == "Favorites") "No Favorite Photos" else "Photo Vault is Empty",
                description = "Hide confidential personal pictures, sensitive documents, and receipts securely inside this encrypted vault.",
                actionLabel = "Import Photos",
                onAction = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(photoItems, key = { it.id }) { item ->
                    PhotoGridCard(
                        item = item,
                        onClick = { viewModel.selectVaultItem(item) },
                        onToggleFavorite = { viewModel.toggleFavoriteItem(item) }
                    )
                }
            }
        }
    }

    // Fullscreen viewer dialog
    if (vaultState.selectedItem != null && vaultState.selectedItem.type == "PHOTO") {
        FullscreenPhotoViewerDialog(
            item = vaultState.selectedItem,
            onDismiss = { viewModel.selectVaultItem(null) },
            onDelete = { viewModel.deleteCurrentItem(vaultState.selectedItem) },
            onToggleFavorite = { viewModel.toggleFavoriteItem(vaultState.selectedItem) }
        )
    }
}

@Composable
fun PhotoGridCard(
    item: VaultItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val file = remember(item.filePath) { File(item.filePath) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("photo_card_${item.id}")
    ) {
        AsyncImage(
            model = file,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Favorite Indicator
        if (item.isFavorite) {
            Surface(
                shape = CircleShape,
                color = Color(0x99000000),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = AccentRose,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // File size tag at bottom
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xAA000000),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
        ) {
            Text(
                text = FileManager.formatFileSize(item.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
