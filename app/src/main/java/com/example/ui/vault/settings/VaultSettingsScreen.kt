package com.example.ui.vault.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SecurityConfig
import com.example.security.BiometricHelper
import com.example.ui.MainViewModel
import com.example.ui.VaultUiState
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun VaultSettingsScreen(
    viewModel: MainViewModel,
    vaultState: VaultUiState,
    config: SecurityConfig?
) {
    val context = LocalContext.current
    val hasBiometricHardware = remember { BiometricHelper.isBiometricAvailable(context) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Vault Status Card
        item {
            GlassCard(
                backgroundColor = DarkSurface,
                borderColor = if (vaultState.isDecoyMode) AccentPurple else AccentEmerald
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (vaultState.isDecoyMode) AccentPurple else AccentEmerald,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (vaultState.isDecoyMode) Icons.Default.VisibilityOff else Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (vaultState.isDecoyMode) "Decoy Vault Mode" else "Master Vault Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (vaultState.isDecoyMode) "Showing secondary safe vault" else "All secret items are encrypted locally",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Security Configuration Section
        item {
            Text(
                text = "ACCESS & AUTHENTICATION",
                style = MaterialTheme.typography.labelSmall,
                color = AccentAmber,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.Password,
                title = "Change Master Passcode",
                subtitle = "Update the 4-8 digit stealth PIN used in calculator",
                onClick = { viewModel.setDialogVisibility(changePass = true) },
                testTag = "change_passcode_btn"
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.Fingerprint,
                title = "Biometric Unlock",
                subtitle = if (hasBiometricHardware) "Unlock vault instantly with Fingerprint / Face" else "Biometric sensor not available on this device",
                trailing = {
                    Switch(
                        checked = config?.biometricEnabled == true && hasBiometricHardware,
                        onCheckedChange = { enabled ->
                            viewModel.toggleBiometricSetting(enabled)
                        },
                        enabled = hasBiometricHardware,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AccentCyan
                        )
                    )
                }
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.VisibilityOff,
                title = "Decoy Vault PIN",
                subtitle = if (config?.decoyPasscodeHash?.isNotEmpty() == true) "Decoy passcode is ACTIVE" else "Set a secondary fake PIN to deceive intruders",
                onClick = { viewModel.setDialogVisibility(decoyPass = true) },
                testTag = "decoy_pin_btn"
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.HelpOutline,
                title = "Security Recovery Question",
                subtitle = config?.securityQuestion ?: "Set security question for forgotten passcode",
                onClick = { viewModel.setDialogVisibility(securityQuestion = true) },
                testTag = "security_question_btn"
            )
        }

        // Preferences Section
        item {
            Text(
                text = "CALCULATOR PREFERENCES",
                style = MaterialTheme.typography.labelSmall,
                color = AccentAmber,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp)
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.Vibration,
                title = "Tactile Haptic Feedback",
                subtitle = "Vibrate softly when tapping calculator keys",
                trailing = {
                    Switch(
                        checked = config?.vibrationEnabled != false,
                        onCheckedChange = { viewModel.toggleHaptics(it, config?.soundEnabled ?: true) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AccentAmber
                        )
                    )
                }
            )
        }

        // How to use / Stealth Guide
        item {
            Text(
                text = "HOW TO ACCESS SECRET VAULT",
                style = MaterialTheme.typography.labelSmall,
                color = AccentAmber,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp)
            )
        }

        item {
            GlassCard(backgroundColor = DarkSurfaceVariant) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Stealth Calculator Instructions:",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "1. Open the calculator and enter your secret passcode digits.\n2. Tap the '=' button to immediately reveal the Secret Vault.\n3. Alternatively, tap the fingerprint icon for instant biometric entry.\n4. Tap the lock icon anytime for instant panic disguise back to calculator.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Panic Lock Button
        item {
            Button(
                onClick = { viewModel.lockVaultToCalculator() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRose,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("panic_lock_btn")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Panic Lock (Return to Calculator)", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Passcode Change Dialog
    if (vaultState.isPasscodeChangeDialogVisible) {
        PasscodeChangeDialog(
            onDismiss = { viewModel.setDialogVisibility(changePass = false) },
            onSave = { newPass -> viewModel.updateMasterPasscode(newPass) }
        )
    }

    // Decoy Passcode Dialog
    if (vaultState.isDecoyPasscodeDialogVisible) {
        DecoyPasscodeDialog(
            currentConfig = config,
            onDismiss = { viewModel.setDialogVisibility(decoyPass = false) },
            onSave = { decoyPass -> viewModel.setDecoyPasscode(decoyPass) }
        )
    }

    // Security Question Dialog
    if (vaultState.isSecurityQuestionDialogVisible) {
        SecurityQuestionDialog(
            currentQuestion = config?.securityQuestion ?: "What is your secret master number?",
            onDismiss = { viewModel.setDialogVisibility(securityQuestion = false) },
            onSave = { question, answer ->
                viewModel.setSecurityQuestionAndAnswer(question, answer)
            }
        )
    }
}

@Composable
fun SettingsItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    testTag: String? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = DarkSurfaceElevated,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(22.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary)
            }
        }
    }
}

@Composable
fun PasscodeChangeDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pass1 by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Change Master Passcode", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pass1,
                    onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) pass1 = it },
                    label = { Text("New 4-8 Digit Passcode") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentAmber,
                        unfocusedBorderColor = BorderGlass,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pass2,
                    onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) pass2 = it },
                    label = { Text("Confirm New Passcode") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentAmber,
                        unfocusedBorderColor = BorderGlass,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = AccentRose, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pass1.length < 4) {
                        error = "Passcode must be at least 4 digits"
                    } else if (pass1 != pass2) {
                        error = "Passcodes do not match"
                    } else {
                        onSave(pass1)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Color.Black)
            ) {
                Text("Update Passcode", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun DecoyPasscodeDialog(
    currentConfig: SecurityConfig?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var decoyPass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Set Decoy Vault PIN", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "When someone forces you to open your vault, typing this Decoy PIN into the calculator opens a separate, empty decoy vault.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = decoyPass,
                    onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) decoyPass = it },
                    label = { Text("Decoy PIN (digits)") },
                    placeholder = { Text("Leave empty to disable") },
                    singleLine = true,
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
                onClick = { onSave(decoyPass) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Color.Black)
            ) {
                Text("Save Decoy PIN", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun SecurityQuestionDialog(
    currentQuestion: String,
    onDismiss: () -> Unit,
    onSave: (question: String, answer: String) -> Unit
) {
    var question by remember { mutableStateOf(currentQuestion) }
    var answer by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Recovery Question", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Security Question") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentAmber,
                        unfocusedBorderColor = BorderGlass,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Secret Answer") },
                    singleLine = true,
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
                    if (answer.isNotBlank()) {
                        onSave(question.trim(), answer.trim())
                    }
                },
                enabled = answer.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Color.Black)
            ) {
                Text("Save Recovery", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
