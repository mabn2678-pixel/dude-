package com.vidget.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vidget.ui.screens.onboarding.OnboardingScreen
import com.vidget.ui.screens.placeholder.AboutScreen
import com.vidget.ui.screens.placeholder.DownloadsScreen
import com.vidget.ui.screens.placeholder.HomeScreen
import com.vidget.ui.screens.placeholder.LanguageSettingsScreen
import com.vidget.ui.screens.placeholder.LibraryScreen
import com.vidget.ui.screens.placeholder.PlayerScreen
import com.vidget.ui.screens.placeholder.SettingsScreen

/**
 * Main Navigation Graph connecting all application destinations with Type-Safe Routes.
 * Automatically shows/hides the Bottom Bar based on destination, preserves state across tabs,
 * and handles back navigation to return to Home before exiting.
 */
@Composable
fun AppNavGraph(
    startDestination: Route,
    onOnboardingFinished: () -> Unit,
    onOpenBatteryOptimization: () -> Unit = {},
    activeDownloadsCount: Int = 0,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Check if current screen is one of the 4 bottom bar destinations
    val showBottomBar = BottomNavDestination.entries.any { destination ->
        currentDestination?.hasRoute(destination.route::class) == true
    }

    // Back button behavior: from any tab other than Home, navigate back to Home.
    // If on Home, BackHandler does not intercept and allows exiting the app.
    val isTopTabOtherThanHome = (
        currentDestination?.hasRoute(Route.Downloads::class) == true ||
        currentDestination?.hasRoute(Route.Library::class) == true ||
        currentDestination?.hasRoute(Route.Settings::class) == true
    )

    BackHandler(enabled = isTopTabOtherThanHome) {
        navController.navigate(Route.Home) {
            popUpTo(Route.Home) {
                saveState = true
                inclusive = false
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                VidGetBottomBar(
                    navController = navController,
                    currentDestination = currentDestination,
                    activeDownloadsCount = activeDownloadsCount
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Onboarding Screen
            composable<Route.Onboarding> {
                OnboardingScreen(
                    onComplete = {
                        onOnboardingFinished()
                        navController.navigate(Route.Home) {
                            popUpTo(Route.Onboarding) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 2. Top-Level Bottom Navigation Tabs
            composable<Route.Home> {
                HomeScreen()
            }

            composable<Route.Downloads> {
                com.vidget.ui.screens.downloads.DownloadsScreen(
                    onNavigateToPlayer = { videoUri ->
                        navController.navigate(Route.Player(videoUri))
                    }
                )
            }

            composable<Route.Library> {
                LibraryScreen(
                    onNavigateToPlayer = { videoUri ->
                        navController.navigate(Route.Player(videoUri))
                    }
                )
            }

            composable<Route.Settings> {
                com.vidget.ui.screens.settings.SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onOpenBatteryOptimization = onOpenBatteryOptimization
                )
            }

            // 3. Internal Destinations (No Bottom Bar)
            composable<Route.Player> { backStackEntry ->
                val videoUri = backStackEntry.arguments?.getString("videoUri")
                    ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                com.vidget.ui.screens.player.VideoPlayerScreen(
                    videoUri = videoUri,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.LanguageSettings> {
                LanguageSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.AboutScreen> {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
