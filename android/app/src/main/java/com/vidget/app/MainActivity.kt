package com.vidget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidget.ui.navigation.AppNavGraph
import com.vidget.ui.navigation.Route
import com.vidget.ui.theme.VidGetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val splashViewModel: SplashViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    @javax.inject.Inject
    lateinit var permissionManager: com.vidget.core.permission.PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Android 12 SplashScreen API before calling super.onCreate
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash screen visible until initialization and onboardingDone check complete (max 2 seconds)
        splashScreen.setKeepOnScreenCondition {
            splashViewModel.uiState.value is SplashUiState.Loading
        }

        setContent {
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by mainViewModel.dynamicColor.collectAsStateWithLifecycle()
            val splashState by splashViewModel.uiState.collectAsStateWithLifecycle()

            VidGetTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val state = splashState) {
                        is SplashUiState.Loading -> {
                            // Empty background while system splash screen is showing
                            Box(modifier = Modifier.fillMaxSize())
                        }
                        is SplashUiState.Success -> {
                            AppNavGraph(
                                startDestination = state.startDestination,
                                onOnboardingFinished = splashViewModel::completeOnboarding,
                                onOpenBatteryOptimization = {
                                    permissionManager.openBatteryOptimizationSettings(this@MainActivity)
                                },
                                activeDownloadsCount = 0
                            )
                        }
                    }
                }
            }
        }
    }
}
