package com.example.calculator

data class CalculationHistoryItem(
    val id: Long = System.currentTimeMillis(),
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CalculatorUiState(
    val expression: String = "",
    val previewResult: String = "",
    val isScientificExpanded: Boolean = false,
    val memoryValue: Double = 0.0,
    val hasMemory: Boolean = false,
    val history: List<CalculationHistoryItem> = emptyList(),
    val showHistoryDrawer: Boolean = false,
    val isFirstTimeSetup: Boolean = false,
    val setupStep: SetupStep = SetupStep.IDLE, // IDLE, ENTER_NEW_PASSCODE, CONFIRM_PASSCODE
    val tempPasscode: String = "",
    val statusMessage: String? = null
)

enum class SetupStep {
    IDLE,
    ENTER_NEW_PASSCODE,
    CONFIRM_PASSCODE
}
