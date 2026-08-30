package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.SmartCalculatorTheme
import com.example.ui.vault.VaultMainScreen

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartCalculatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val currentScreen by viewModel.appScreen.collectAsStateWithLifecycle()
                    val calcState by viewModel.calcState.collectAsStateWithLifecycle()
                    val vaultState by viewModel.vaultState.collectAsStateWithLifecycle()
                    val securityConfig by viewModel.securityConfig.collectAsStateWithLifecycle()
                    val vaultItems by viewModel.vaultItems.collectAsStateWithLifecycle()
                    val secretNotes by viewModel.secretNotes.collectAsStateWithLifecycle()
                    val securityLogs by viewModel.securityLogs.collectAsStateWithLifecycle()

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState == AppScreen.VAULT) {
                                (fadeIn() + scaleIn(initialScale = 0.92f))
                                    .togetherWith(fadeOut() + scaleOut(targetScale = 1.05f))
                            } else {
                                (fadeIn() + scaleIn(initialScale = 1.05f))
                                    .togetherWith(fadeOut() + scaleOut(targetScale = 0.92f))
                            }
                        },
                        label = "AppScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.CALCULATOR -> {
                                CalculatorScreen(
                                    viewModel = viewModel,
                                    calcState = calcState,
                                    securityConfig = securityConfig
                                )
                            }
                            AppScreen.VAULT -> {
                                VaultMainScreen(
                                    viewModel = viewModel,
                                    vaultState = vaultState,
                                    vaultItems = vaultItems,
                                    secretNotes = secretNotes,
                                    securityLogs = securityLogs,
                                    securityConfig = securityConfig
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
