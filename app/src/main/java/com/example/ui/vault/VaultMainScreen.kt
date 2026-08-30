package com.example.ui.vault

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SecretNote
import com.example.data.model.SecurityConfig
import com.example.data.model.SecurityLog
import com.example.data.model.VaultItem
import com.example.ui.MainViewModel
import com.example.ui.VaultTab
import com.example.ui.VaultUiState
import com.example.ui.theme.*
import com.example.ui.vault.files.FilesScreen
import com.example.ui.vault.logs.IntruderLogsScreen
import com.example.ui.vault.notes.NotesScreen
import com.example.ui.vault.photos.PhotosScreen
import com.example.ui.vault.settings.VaultSettingsScreen
import com.example.ui.vault.videos.VideosScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultMainScreen(
    viewModel: MainViewModel,
    vaultState: VaultUiState,
    vaultItems: List<VaultItem>,
    secretNotes: List<SecretNote>,
    securityLogs: List<SecurityLog>,
    securityConfig: SecurityConfig?
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Quick pick photo launcher for FAB
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMediaUris(uris, "PHOTO")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchExpanded) {
                        OutlinedTextField(
                            value = vaultState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search hidden items...", color = TextTertiary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentAmber,
                                unfocusedBorderColor = BorderGlass,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (vaultState.isDecoyMode) "Decoy Vault" else "Secret Vault",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (vaultState.isDecoyMode) AccentPurple else AccentAmber
                            )
                            if (vaultState.isDecoyMode) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0x33BF5AF2)
                                ) {
                                    Text(
                                        "DECOY",
                                        color = AccentPurple,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) viewModel.setSearchQuery("")
                    }) {
                        Icon(
                            if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextSecondary
                        )
                    }

                    // Panic Lock Button
                    IconButton(
                        onClick = { viewModel.lockVaultToCalculator() },
                        modifier = Modifier.testTag("panic_lock_top_button")
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Panic Lock",
                            tint = AccentRose
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = TextPrimary,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                VaultBottomNavItem(
                    selected = vaultState.currentTab == VaultTab.PHOTOS,
                    onClick = { viewModel.setVaultTab(VaultTab.PHOTOS) },
                    icon = Icons.Default.Photo,
                    label = "Photos",
                    testTag = "tab_photos"
                )
                VaultBottomNavItem(
                    selected = vaultState.currentTab == VaultTab.VIDEOS,
                    onClick = { viewModel.setVaultTab(VaultTab.VIDEOS) },
                    icon = Icons.Default.Videocam,
                    label = "Videos",
                    testTag = "tab_videos"
                )
                VaultBottomNavItem(
                    selected = vaultState.currentTab == VaultTab.FILES,
                    onClick = { viewModel.setVaultTab(VaultTab.FILES) },
                    icon = Icons.Default.Folder,
                    label = "Files",
                    testTag = "tab_files"
                )
                VaultBottomNavItem(
                    selected = vaultState.currentTab == VaultTab.NOTES,
                    onClick = { viewModel.setVaultTab(VaultTab.NOTES) },
                    icon = Icons.Default.EditNote,
                    label = "Notes",
                    testTag = "tab_notes"
                )
                VaultBottomNavItem(
                    selected = vaultState.currentTab == VaultTab.LOGS,
                    onClick = { viewModel.setVaultTab(VaultTab.LOGS) },
                    icon = Icons.Default.Shield,
                    label = "Logs",
                    testTag = "tab_logs"
                )
                VaultBottomNavItem(
                    selected = vaultState.currentTab == VaultTab.SETTINGS,
                    onClick = { viewModel.setVaultTab(VaultTab.SETTINGS) },
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    testTag = "tab_settings"
                )
            }
        },
        floatingActionButton = {
            if (vaultState.currentTab == VaultTab.PHOTOS || vaultState.currentTab == VaultTab.NOTES) {
                FloatingActionButton(
                    onClick = {
                        if (vaultState.currentTab == VaultTab.PHOTOS) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else if (vaultState.currentTab == VaultTab.NOTES) {
                            viewModel.setDialogVisibility(addNote = true)
                        }
                    },
                    containerColor = AccentAmber,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("vault_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        },
        snackbarHost = {
            if (vaultState.bannerMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentEmerald,
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable { viewModel.clearBanner() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                        Text(
                            text = vaultState.bannerMessage,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (vaultState.currentTab) {
                VaultTab.PHOTOS -> PhotosScreen(
                    viewModel = viewModel,
                    vaultState = vaultState,
                    items = vaultItems
                )
                VaultTab.VIDEOS -> VideosScreen(
                    viewModel = viewModel,
                    vaultState = vaultState,
                    items = vaultItems
                )
                VaultTab.FILES -> FilesScreen(
                    viewModel = viewModel,
                    vaultState = vaultState,
                    items = vaultItems
                )
                VaultTab.NOTES -> NotesScreen(
                    viewModel = viewModel,
                    vaultState = vaultState,
                    notes = secretNotes
                )
                VaultTab.LOGS -> IntruderLogsScreen(
                    viewModel = viewModel,
                    logs = securityLogs
                )
                VaultTab.SETTINGS -> VaultSettingsScreen(
                    viewModel = viewModel,
                    vaultState = vaultState,
                    config = securityConfig
                )
            }
        }
    }
}

@Composable
fun RowScope.VaultBottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    testTag: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = AccentAmber,
            indicatorColor = AccentAmber,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary
        ),
        modifier = Modifier.testTag(testTag)
    )
}
