package com.example.ui.vault.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SecurityLog
import com.example.security.FileManager
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.VaultSectionHeader
import com.example.ui.theme.*

@Composable
fun IntruderLogsScreen(
    viewModel: MainViewModel,
    logs: List<SecurityLog>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        VaultSectionHeader(
            title = "Security & Intruder Audit",
            count = logs.size,
            subtitle = "Detailed audit trail of access attempts",
            actionIcon = if (logs.isNotEmpty()) Icons.Default.DeleteSweep else null,
            onAction = if (logs.isNotEmpty()) {
                { viewModel.clearSecurityLogs() }
            } else null
        )

        if (logs.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Default.Shield,
                title = "No Security Alerts",
                description = "Every successful unlock, biometric entry, passcode update, and unauthorized attempt is logged here."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogCard(log = log)
                }
            }
        }
    }
}

@Composable
fun LogCard(log: SecurityLog) {
    val isFailed = !log.wasSuccessful || log.eventType == "FAILED_ATTEMPT"
    val isBiometric = log.eventType.contains("BIOMETRIC")
    val isDecoy = log.eventType.contains("DECOY")

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isFailed) Color(0x33FF453A) else DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFailed) AccentRose else BorderGlass),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("log_card_${log.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isFailed) AccentRose else if (isBiometric) AccentCyan else if (isDecoy) AccentPurple else AccentEmerald,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when {
                            isFailed -> Icons.Default.Warning
                            isBiometric -> Icons.Default.Fingerprint
                            isDecoy -> Icons.Default.VisibilityOff
                            else -> Icons.Default.Check
                        },
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (log.eventType) {
                        "UNLOCKED_PASSCODE" -> "Passcode Vault Unlocked"
                        "UNLOCKED_BIOMETRIC" -> "Biometric Vault Unlocked"
                        "DECOY_UNLOCKED" -> "Decoy Vault Unlocked"
                        "FAILED_ATTEMPT" -> "⚠️ Unauthorized Passcode Attempt"
                        "PASSCODE_CHANGED" -> "Master Passcode Updated"
                        else -> log.eventType
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFailed) AccentRose else TextPrimary
                )
                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = FileManager.formatDate(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
