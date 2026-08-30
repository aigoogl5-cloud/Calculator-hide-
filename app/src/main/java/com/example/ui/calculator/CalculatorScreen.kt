package com.example.ui.calculator

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.calculator.CalculationHistoryItem
import com.example.calculator.CalculatorUiState
import com.example.calculator.SetupStep
import com.example.data.model.SecurityConfig
import com.example.security.BiometricHelper
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: MainViewModel,
    calcState: CalculatorUiState,
    securityConfig: SecurityConfig?
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Disguise Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "RAD",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        modifier = Modifier
                            .background(DarkSurfaceElevated, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "Smart Calculator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Biometric Unlock button (discreet stealth trigger)
                    if (securityConfig?.biometricEnabled == true && BiometricHelper.isBiometricAvailable(context) && activity != null) {
                        IconButton(
                            onClick = {
                                triggerHapticFeedback(context)
                                BiometricHelper.showBiometricPrompt(
                                    activity = activity,
                                    title = "Vault Biometric Login",
                                    subtitle = "Authenticate to open private vault",
                                    onSuccess = {
                                        viewModel.onBiometricUnlockSuccess()
                                    },
                                    onError = { /* discreetly handle */ }
                                )
                            },
                            modifier = Modifier.testTag("biometric_unlock_button")
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = "Biometric Unlock",
                                tint = AccentCyan
                            )
                        }
                    }

                    // History Drawer Toggle
                    IconButton(onClick = {
                        triggerHapticFeedback(context)
                        viewModel.toggleHistoryDrawer()
                    }) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History",
                            tint = if (calcState.showHistoryDrawer) AccentAmber else TextSecondary
                        )
                    }

                    // Scientific Expander Toggle
                    IconButton(onClick = {
                        triggerHapticFeedback(context)
                        viewModel.toggleScientific()
                    }) {
                        Icon(
                            Icons.Default.Functions,
                            contentDescription = "Scientific Functions",
                            tint = if (calcState.isScientificExpanded) AccentAmber else TextSecondary
                        )
                    }
                }
            }

            // Setup / Status Banner
            AnimatedVisibility(
                visible = calcState.statusMessage != null || calcState.isFirstTimeSetup,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (calcState.isFirstTimeSetup) DarkSurfaceVariant else DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (calcState.isFirstTimeSetup) AccentAmber else BorderGlass),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (calcState.isFirstTimeSetup) Icons.Default.Security else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (calcState.isFirstTimeSetup) AccentAmber else AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = calcState.statusMessage ?: if (calcState.setupStep == SetupStep.CONFIRM_PASSCODE) "Confirm your passcode & press =" else "Setup: Enter 4-8 digit passcode & press =",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Calculator Display Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Expression string
                Text(
                    text = if (calcState.expression.isEmpty()) "0" else calcState.expression,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = if (calcState.expression.length > 14) 28.sp else if (calcState.expression.length > 9) 36.sp else 46.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                // Preview evaluated result
                if (calcState.previewResult.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "= ${calcState.previewResult}",
                        style = MaterialTheme.typography.titleLarge,
                        color = AccentAmber,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Keypad Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Scientific row (collapsible or expand)
                AnimatedVisibility(
                    visible = calcState.isScientificExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScientificKey("sin", Modifier.weight(1f)) { viewModel.onCalculatorOperator("sin(") }
                            ScientificKey("cos", Modifier.weight(1f)) { viewModel.onCalculatorOperator("cos(") }
                            ScientificKey("tan", Modifier.weight(1f)) { viewModel.onCalculatorOperator("tan(") }
                            ScientificKey("ln", Modifier.weight(1f)) { viewModel.onCalculatorOperator("ln(") }
                            ScientificKey("log", Modifier.weight(1f)) { viewModel.onCalculatorOperator("log(") }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScientificKey("√", Modifier.weight(1f)) { viewModel.onCalculatorOperator("√(") }
                            ScientificKey("π", Modifier.weight(1f)) { viewModel.onCalculatorDigit("π") }
                            ScientificKey("e", Modifier.weight(1f)) { viewModel.onCalculatorDigit("e") }
                            ScientificKey("^", Modifier.weight(1f)) { viewModel.onCalculatorOperator("^") }
                            ScientificKey("!", Modifier.weight(1f)) { viewModel.onCalculatorOperator("!") }
                        }
                    }
                }

                // Standard Keypad Grid (5 rows)
                // Row 1: AC, ( ), %, ÷
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcKey(
                        text = "AC",
                        modifier = Modifier.weight(1f),
                        backgroundColor = KeyActionBg,
                        textColor = AccentRose,
                        onClick = { viewModel.onCalculatorClear() }
                    )
                    CalcKey(
                        text = "( )",
                        modifier = Modifier.weight(1f),
                        backgroundColor = KeyActionBg,
                        textColor = AccentCyan,
                        onClick = {
                            val expr = calcState.expression
                            val openCount = expr.count { it == '(' }
                            val closeCount = expr.count { it == ')' }
                            if (openCount > closeCount && expr.isNotEmpty() && expr.last().isDigit()) {
                                viewModel.onCalculatorDigit(")")
                            } else {
                                viewModel.onCalculatorDigit("(")
                            }
                        }
                    )
                    CalcKey(
                        text = "%",
                        modifier = Modifier.weight(1f),
                        backgroundColor = KeyActionBg,
                        textColor = AccentCyan,
                        onClick = { viewModel.onCalculatorOperator("%") }
                    )
                    CalcKey(
                        text = "÷",
                        modifier = Modifier.weight(1f),
                        backgroundColor = KeyOpBg,
                        textColor = AccentAmber,
                        onClick = { viewModel.onCalculatorOperator("÷") }
                    )
                }

                // Row 2: 7, 8, 9, ×
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcKey(text = "7", modifier = Modifier.weight(1f)) { viewModel.onCalculatorDigit("7") }
                    CalcKey(text = "8", modifier = Modifier.weight(1f)) { viewModel.onCalculatorDigit("8") }
                    CalcKey(text = "9", modifier = Modifier.weight(1f)) { viewModel.onCalculatorDigit("9") }
                    CalcKey(
                        text = "×",
                        modifier = Modifier.weight(1f),
                        backgroundColor = KeyOpBg,
                        textColor = AccentAmber,
                        onClick = { viewModel.onCalculatorOperator("×") }
                    )
                }

                // Row 3: 4, 5, 6, −
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcKey(text = "4", modifier = Modifier.weight(1f)) { viewModel.onCalculatorDigit("4") }
                    CalcKey(text = "5", modifier = Modifier.weight(1f)) { viewModel.onCalculatorDigit("5") }
                    CalcKey(text = "6", modifier = Modifier.weight(1f)) { viewModel.onCalculatorDigit("6") }
                    CalcKey(
                        text = "−",
                        modifier = Modifier.weight(1f),
                        backgroundColor = KeyOpBg,
                        textColor = AccentAmber,
                        onClick = { viewModel.onCalculatorOperator("-") }
                    )
                }

                // Row 4: 1, 2, 3, +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcKey(text = "1", modifier = Modifier.weight(1f)) { viewModel.onCalculatorDigit("1") }
                    CalcKey(text = "2", modifier = Modifier.weight(1f)) { viewModel.onCalculatorDigit("2") }
                    CalcKey(text = "3", modifier = Modifier.weight(1f)) { viewModel.onCalculatorDigit("3") }
                    CalcKey(
                        text = "+",
                        modifier = Modifier.weight(1f),
                        backgroundColor = KeyOpBg,
                        textColor = AccentAmber,
                        onClick = { viewModel.onCalculatorOperator("+") }
                    )
                }

                // Row 5: 0, ., ⌫, =
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalcKey(text = "0", modifier = Modifier.weight(1f)) { viewModel.onCalculatorDigit("0") }
                    CalcKey(text = ".", modifier = Modifier.weight(1f)) { viewModel.onCalculatorDigit(".") }
                    CalcKey(
                        text = "⌫",
                        modifier = Modifier.weight(1f),
                        backgroundColor = KeyNumBg,
                        textColor = TextSecondary,
                        onClick = { viewModel.onCalculatorBackspace() }
                    )
                    CalcKey(
                        text = "=",
                        modifier = Modifier.weight(1f),
                        backgroundColor = KeyEqualsBg,
                        textColor = Color.Black,
                        isPrimaryAction = true,
                        onClick = { viewModel.onEqualsPressed() }
                    )
                }
            }
        }

        // History Drawer / BottomSheet
        if (calcState.showHistoryDrawer) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleHistoryDrawer() },
                containerColor = DarkSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Calculation History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        TextButton(onClick = { viewModel.toggleHistoryDrawer() }) {
                            Text("Done", color = AccentAmber)
                        }
                    }

                    if (calcState.history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No recent calculations", color = TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(calcState.history) { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onCalculatorDigit(item.result)
                                            viewModel.toggleHistoryDrawer()
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(item.expression, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                        Text("= ${item.result}", color = AccentAmber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalcKey(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = KeyNumBg,
    textColor: Color = TextPrimary,
    isPrimaryAction: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                triggerHapticFeedback(context)
                onClick()
            }
            .testTag("calc_key_$text"),
        color = backgroundColor,
        shape = RoundedCornerShape(18.dp),
        border = if (isPrimaryAction) null else androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = if (text.length > 2) 20.sp else 24.sp,
                    fontWeight = if (isPrimaryAction) FontWeight.ExtraBold else FontWeight.Medium
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ScientificKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                triggerHapticFeedback(context)
                onClick()
            }
            .testTag("sci_key_$text"),
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = AccentCyan,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
