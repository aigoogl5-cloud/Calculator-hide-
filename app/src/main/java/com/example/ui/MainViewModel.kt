package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.CalculatorEngine
import com.example.calculator.CalculationHistoryItem
import com.example.calculator.CalculatorUiState
import com.example.calculator.SetupStep
import com.example.data.local.VaultDatabase
import com.example.data.model.SecretNote
import com.example.data.model.SecurityConfig
import com.example.data.model.SecurityLog
import com.example.data.model.VaultItem
import com.example.data.repository.VaultRepository
import com.example.security.CryptoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    CALCULATOR,
    VAULT
}

enum class VaultTab {
    PHOTOS,
    VIDEOS,
    FILES,
    NOTES,
    LOGS,
    SETTINGS
}

data class VaultUiState(
    val currentTab: VaultTab = VaultTab.PHOTOS,
    val isDecoyMode: Boolean = false,
    val selectedItem: VaultItem? = null,
    val selectedNote: SecretNote? = null,
    val isEditingNote: Boolean = false,
    val searchQuery: String = "",
    val activeFilterCategory: String = "All",
    val isAddFileDialogVisible: Boolean = false,
    val isAddNoteDialogVisible: Boolean = false,
    val isPasscodeChangeDialogVisible: Boolean = false,
    val isDecoyPasscodeDialogVisible: Boolean = false,
    val isSecurityQuestionDialogVisible: Boolean = false,
    val isRecoveryDialogVisible: Boolean = false,
    val bannerMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VaultRepository

    private val _appScreen = MutableStateFlow(AppScreen.CALCULATOR)
    val appScreen: StateFlow<AppScreen> = _appScreen.asStateFlow()

    private val _calcState = MutableStateFlow(CalculatorUiState())
    val calcState: StateFlow<CalculatorUiState> = _calcState.asStateFlow()

    private val _vaultState = MutableStateFlow(VaultUiState())
    val vaultState: StateFlow<VaultUiState> = _vaultState.asStateFlow()

    val securityConfig: StateFlow<SecurityConfig?>

    val vaultItems: StateFlow<List<VaultItem>>
    val secretNotes: StateFlow<List<SecretNote>>
    val securityLogs: StateFlow<List<SecurityLog>>

    init {
        val db = VaultDatabase.getDatabase(application)
        repository = VaultRepository(db.vaultDao(), application)

        securityConfig = repository.getSecurityConfigFlow()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        vaultItems = repository.getVaultItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        secretNotes = repository.getSecretNotes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        securityLogs = repository.getSecurityLogs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        viewModelScope.launch {
            val config = repository.getSecurityConfig()
            if (!config.isConfigured) {
                _calcState.update {
                    it.copy(
                        isFirstTimeSetup = true,
                        setupStep = SetupStep.ENTER_NEW_PASSCODE,
                        statusMessage = "Set your 4-8 digit secret passcode & press ="
                    )
                }
            }
        }
    }

    // ================= CALCULATOR & PASSCODE LOGIC =================

    fun onCalculatorDigit(digit: String) {
        _calcState.update { current ->
            val newExpr = if (current.expression == "0" && digit != ".") digit else current.expression + digit
            val preview = if (newExpr.length > 2 && containsOperator(newExpr)) {
                val eval = CalculatorEngine.evaluate(newExpr)
                if (eval.success) eval.resultString else ""
            } else ""
            current.copy(expression = newExpr, previewResult = preview, statusMessage = null)
        }
    }

    fun onCalculatorOperator(op: String) {
        _calcState.update { current ->
            val expr = current.expression
            if (expr.isEmpty() && (op == "-" || op == "+" || op == "√")) {
                current.copy(expression = op)
            } else if (expr.isNotEmpty()) {
                val lastChar = expr.last().toString()
                val updated = if (isOperatorChar(lastChar)) {
                    expr.dropLast(1) + op
                } else {
                    expr + op
                }
                current.copy(expression = updated)
            } else current
        }
    }

    fun onCalculatorClear() {
        _calcState.update {
            it.copy(expression = "", previewResult = "", statusMessage = null)
        }
    }

    fun onCalculatorBackspace() {
        _calcState.update { current ->
            if (current.expression.isNotEmpty()) {
                val newExpr = current.expression.dropLast(1)
                val preview = if (newExpr.length > 2 && containsOperator(newExpr)) {
                    val eval = CalculatorEngine.evaluate(newExpr)
                    if (eval.success) eval.resultString else ""
                } else ""
                current.copy(expression = newExpr, previewResult = preview)
            } else current
        }
    }

    fun toggleScientific() {
        _calcState.update { it.copy(isScientificExpanded = !it.isScientificExpanded) }
    }

    fun toggleHistoryDrawer() {
        _calcState.update { it.copy(showHistoryDrawer = !it.showHistoryDrawer) }
    }

    fun onEqualsPressed() {
        val current = _calcState.value
        val expr = current.expression.trim()

        viewModelScope.launch {
            val config = repository.getSecurityConfig()

            // 1. Check First-Time Setup Flow
            if (!config.isConfigured || current.isFirstTimeSetup) {
                handleFirstTimeSetup(expr)
                return@launch
            }

            // 2. Check Passcode match for Vault Unlock
            val cleanPass = expr.replace(" ", "")
            if (cleanPass.isNotEmpty() && (cleanPass.all { it.isDigit() } || cleanPass.matches(Regex("^[0-9+*#]+$")))) {
                val inputHash = CryptoUtils.hashString(cleanPass)

                if (inputHash == config.passcodeHash) {
                    // Unlock Main Vault
                    unlockVault(isDecoy = false, trigger = "PASSCODE")
                    return@launch
                } else if (config.decoyPasscodeHash.isNotEmpty() && inputHash == config.decoyPasscodeHash) {
                    // Unlock Decoy Vault
                    unlockVault(isDecoy = true, trigger = "DECOY_PASSCODE")
                    return@launch
                }
            }

            // 3. Normal Calculator Calculation
            val eval = CalculatorEngine.evaluate(expr)
            if (eval.success) {
                val newHistoryItem = CalculationHistoryItem(
                    expression = expr,
                    result = eval.resultString
                )
                _calcState.update {
                    it.copy(
                        expression = eval.resultString,
                        previewResult = "",
                        history = listOf(newHistoryItem) + it.history
                    )
                }
            } else {
                // If it was purely digits that failed passcode
                if (expr.length >= 4 && expr.all { it.isDigit() }) {
                    repository.logSecurityEvent("FAILED_ATTEMPT", "Incorrect passcode attempt: $expr", false)
                }
                _calcState.update {
                    it.copy(statusMessage = eval.errorMessage ?: "Calculation Error")
                }
            }
        }
    }

    private suspend fun handleFirstTimeSetup(expr: String) {
        val current = _calcState.value
        if (current.setupStep == SetupStep.ENTER_NEW_PASSCODE || current.setupStep == SetupStep.IDLE) {
            if (expr.length in 4..12 && expr.all { it.isDigit() }) {
                _calcState.update {
                    it.copy(
                        tempPasscode = expr,
                        setupStep = SetupStep.CONFIRM_PASSCODE,
                        expression = "",
                        statusMessage = "Re-enter passcode & press = to confirm"
                    )
                }
            } else {
                _calcState.update {
                    it.copy(statusMessage = "Passcode must be 4 to 8 digits")
                }
            }
        } else if (current.setupStep == SetupStep.CONFIRM_PASSCODE) {
            if (expr == current.tempPasscode) {
                repository.setMasterPasscode(expr)
                _calcState.update {
                    it.copy(
                        isFirstTimeSetup = false,
                        setupStep = SetupStep.IDLE,
                        tempPasscode = "",
                        expression = "",
                        statusMessage = "Passcode saved successfully!"
                    )
                }
                unlockVault(isDecoy = false, trigger = "INITIAL_SETUP")
            } else {
                _calcState.update {
                    it.copy(
                        setupStep = SetupStep.ENTER_NEW_PASSCODE,
                        tempPasscode = "",
                        expression = "",
                        statusMessage = "Passcodes did not match. Enter new 4-8 digit passcode & press ="
                    )
                }
            }
        }
    }

    fun onBiometricUnlockSuccess() {
        unlockVault(isDecoy = false, trigger = "BIOMETRIC")
    }

    private fun unlockVault(isDecoy: Boolean, trigger: String) {
        viewModelScope.launch {
            val eventType = if (isDecoy) "DECOY_UNLOCKED" else if (trigger == "BIOMETRIC") "UNLOCKED_BIOMETRIC" else "UNLOCKED_PASSCODE"
            repository.logSecurityEvent(eventType, "Vault accessed via $trigger", true)
        }
        _vaultState.update { it.copy(isDecoyMode = isDecoy, currentTab = VaultTab.PHOTOS) }
        _calcState.update { it.copy(expression = "", previewResult = "", statusMessage = null) }
        _appScreen.value = AppScreen.VAULT
    }

    fun lockVaultToCalculator() {
        // Panic lock
        _appScreen.value = AppScreen.CALCULATOR
        _vaultState.update {
            it.copy(
                selectedItem = null,
                selectedNote = null,
                isEditingNote = false,
                isAddFileDialogVisible = false,
                isAddNoteDialogVisible = false
            )
        }
        _calcState.update { it.copy(expression = "", previewResult = "") }
    }

    // ================= VAULT ACTIONS =================

    fun setVaultTab(tab: VaultTab) {
        _vaultState.update { it.copy(currentTab = tab, selectedItem = null, selectedNote = null) }
    }

    fun selectVaultItem(item: VaultItem?) {
        _vaultState.update { it.copy(selectedItem = item) }
    }

    fun selectNote(note: SecretNote?) {
        _vaultState.update { it.copy(selectedNote = note, isEditingNote = note != null) }
    }

    fun setEditingNote(editing: Boolean) {
        _vaultState.update { it.copy(isEditingNote = editing) }
    }

    fun setSearchQuery(query: String) {
        _vaultState.update { it.copy(searchQuery = query) }
    }

    fun setCategoryFilter(category: String) {
        _vaultState.update { it.copy(activeFilterCategory = category) }
    }

    fun importMediaUris(uris: List<Uri>, type: String) {
        viewModelScope.launch {
            val isDecoy = _vaultState.value.isDecoyMode
            var successCount = 0
            for (uri in uris) {
                val success = repository.importMedia(uri, type, isDecoy = isDecoy)
                if (success) successCount++
            }
            _vaultState.update {
                it.copy(bannerMessage = "Imported $successCount $type(s) securely into vault")
            }
        }
    }

    fun createSecretDocument(title: String, content: String) {
        viewModelScope.launch {
            val isDecoy = _vaultState.value.isDecoyMode
            val success = repository.createSecretDocument(title, content, isDecoy)
            if (success) {
                _vaultState.update {
                    it.copy(
                        isAddFileDialogVisible = false,
                        bannerMessage = "Secret file '$title' saved securely"
                    )
                }
            }
        }
    }

    fun saveNote(title: String, content: String, category: String, colorHex: Long) {
        viewModelScope.launch {
            val currentSelected = _vaultState.value.selectedNote
            val isDecoy = _vaultState.value.isDecoyMode
            val note = currentSelected?.copy(
                title = title,
                content = content,
                category = category,
                colorHex = colorHex,
                updatedTimestamp = System.currentTimeMillis()
            ) ?: SecretNote(
                title = title,
                content = content,
                category = category,
                colorHex = colorHex,
                isDecoy = isDecoy
            )
            repository.saveNote(note)
            _vaultState.update {
                it.copy(
                    isAddNoteDialogVisible = false,
                    isEditingNote = false,
                    selectedNote = null,
                    bannerMessage = "Note saved"
                )
            }
        }
    }

    fun deleteCurrentItem(item: VaultItem) {
        viewModelScope.launch {
            repository.deleteVaultItem(item)
            _vaultState.update { it.copy(selectedItem = null, bannerMessage = "Item removed from vault") }
        }
    }

    fun deleteCurrentNote(note: SecretNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
            _vaultState.update { it.copy(selectedNote = null, isEditingNote = false, bannerMessage = "Note deleted") }
        }
    }

    fun toggleFavoriteItem(item: VaultItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
            _vaultState.update { current ->
                if (current.selectedItem?.id == item.id) {
                    current.copy(selectedItem = item.copy(isFavorite = !item.isFavorite))
                } else current
            }
        }
    }

    fun updateMasterPasscode(newPass: String) {
        viewModelScope.launch {
            repository.setMasterPasscode(newPass)
            _vaultState.update {
                it.copy(
                    isPasscodeChangeDialogVisible = false,
                    bannerMessage = "Master passcode updated successfully"
                )
            }
        }
    }

    fun setDecoyPasscode(decoyPass: String) {
        viewModelScope.launch {
            repository.setDecoyPasscode(decoyPass)
            _vaultState.update {
                it.copy(
                    isDecoyPasscodeDialogVisible = false,
                    bannerMessage = if (decoyPass.isNotBlank()) "Decoy PIN configured" else "Decoy PIN disabled"
                )
            }
        }
    }

    fun setSecurityQuestionAndAnswer(question: String, answer: String) {
        viewModelScope.launch {
            val current = repository.getSecurityConfig()
            repository.setMasterPasscode(
                passcode = "", // keeps existing because repository checks
                securityQuestion = question,
                securityAnswer = answer
            )
            val updated = current.copy(
                securityQuestion = question,
                securityAnswerHash = CryptoUtils.hashString(answer.trim().lowercase())
            )
            val db = VaultDatabase.getDatabase(getApplication())
            db.vaultDao().saveSecurityConfig(updated)
            _vaultState.update {
                it.copy(
                    isSecurityQuestionDialogVisible = false,
                    bannerMessage = "Recovery question updated"
                )
            }
        }
    }

    fun toggleBiometricSetting(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateBiometricSetting(enabled)
        }
    }

    fun toggleHaptics(vibration: Boolean, sound: Boolean) {
        viewModelScope.launch {
            repository.updateHaptics(vibration, sound)
        }
    }

    fun clearSecurityLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _vaultState.update { it.copy(bannerMessage = "Audit logs cleared") }
        }
    }

    fun setDialogVisibility(
        addFile: Boolean = false,
        addNote: Boolean = false,
        changePass: Boolean = false,
        decoyPass: Boolean = false,
        securityQuestion: Boolean = false,
        recovery: Boolean = false
    ) {
        _vaultState.update {
            it.copy(
                isAddFileDialogVisible = addFile,
                isAddNoteDialogVisible = addNote,
                isPasscodeChangeDialogVisible = changePass,
                isDecoyPasscodeDialogVisible = decoyPass,
                isSecurityQuestionDialogVisible = securityQuestion,
                isRecoveryDialogVisible = recovery
            )
        }
    }

    fun clearBanner() {
        _vaultState.update { it.copy(bannerMessage = null) }
    }

    private fun isOperatorChar(c: String): Boolean {
        return c in listOf("+", "-", "×", "÷", "*", "/", "%", "^")
    }

    private fun containsOperator(expr: String): Boolean {
        return expr.any { it in "+-×÷*/%^" }
    }
}
