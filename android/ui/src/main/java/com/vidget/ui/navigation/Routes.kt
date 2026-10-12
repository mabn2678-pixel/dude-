package com.vidget.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.vidget.ui.R
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for Navigation Compose.
 */
sealed interface Route {
    // 1. Onboarding Screen (shown only once on first run)
    @Serializable
    data object Onboarding : Route

    // 2. Bottom Navigation Destinations (Ordered: Home, Downloads, Library, Settings)
    @Serializable
    data object Home : Route

    @Serializable
    data object Downloads : Route

    @Serializable
    data object Library : Route

    @Serializable
    data object Settings : Route

    // 3. Internal Full-Screen Destinations (No Bottom Navigation Bar)
    @Serializable
    data class Player(val videoUri: String? = null) : Route

    @Serializable
    data object LanguageSettings : Route

    @Serializable
    data object AboutScreen : Route
}

/**
 * Representation of a bottom navigation bar tab item.
 */
enum class BottomNavDestination(
    val route: Route,
    @StringRes val labelResId: Int,
    val icon: ImageVector
) {
    HOME(
        route = Route.Home,
        labelResId = R.string.nav_home,
        icon = Icons.Rounded.Home
    ),
    DOWNLOADS(
        route = Route.Downloads,
        labelResId = R.string.nav_downloads,
        icon = Icons.Rounded.Download
    ),
    LIBRARY(
        route = Route.Library,
        labelResId = R.string.nav_library,
        icon = Icons.Rounded.VideoLibrary
    ),
    SETTINGS(
        route = Route.Settings,
        labelResId = R.string.nav_settings,
        icon = Icons.Rounded.Settings
    )
}
