package com.example.ui.vault.videos

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
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.VaultSectionHeader
import com.example.ui.components.VideoDetailDialog
import com.example.ui.theme.*

@Composable
fun VideosScreen(
    viewModel: MainViewModel,
    vaultState: VaultUiState,
    items: List<VaultItem>
) {
    val videoItems = remember(items, vaultState.searchQuery, vaultState.activeFilterCategory) {
        items.filter { it.type == "VIDEO" }
            .filter {
                if (vaultState.activeFilterCategory == "Favorites") it.isFavorite else true
            }
            .filter {
                if (vaultState.searchQuery.isNotBlank()) {
                    it.title.contains(vaultState.searchQuery, ignoreCase = true)
                } else true
            }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMediaUris(uris, "VIDEO")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        VaultSectionHeader(
            title = "Secret Video Vault",
            count = videoItems.size,
            subtitle = "Protected media vault with playback privacy"
        )

        if (videoItems.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.VideoLibrary,
                title = "No Hidden Videos",
                description = "Import personal and private video clips directly into your encrypted vault storage.",
                actionLabel = "Import Videos",
                onAction = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(videoItems, key = { it.id }) { item ->
                    VideoCard(
                        item = item,
                        onClick = { viewModel.selectVaultItem(item) }
                    )
                }
            }
        }
    }

    if (vaultState.selectedItem != null && vaultState.selectedItem.type == "VIDEO") {
        VideoDetailDialog(
            item = vaultState.selectedItem,
            onDismiss = { viewModel.selectVaultItem(null) },
            onDelete = { viewModel.deleteCurrentItem(vaultState.selectedItem) },
            onToggleFavorite = { viewModel.toggleFavoriteItem(vaultState.selectedItem) }
        )
    }
}

@Composable
fun VideoCard(
    item: VaultItem,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("video_card_${item.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x88000000),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = AccentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                // Size Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xAA000000),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
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

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = FileManager.formatDate(item.addedTimestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
