@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)

package skywa1ker404.flickdocket.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import skywa1ker404.flickdocket.R
import skywa1ker404.flickdocket.data.prefs.AppSettings
import skywa1ker404.flickdocket.di.AppContainer
import skywa1ker404.flickdocket.ui.components.SharedScopes
import skywa1ker404.flickdocket.ui.screens.AppViewModelFactory
import skywa1ker404.flickdocket.ui.screens.detail.DetailScreen
import skywa1ker404.flickdocket.ui.screens.discover.DiscoverScreen
import skywa1ker404.flickdocket.ui.screens.library.LibraryScreen
import skywa1ker404.flickdocket.ui.screens.settings.SettingsScreen
import skywa1ker404.flickdocket.ui.screens.stats.StatsScreen
import skywa1ker404.flickdocket.ui.theme.LocalReducedMotion
import androidx.lifecycle.viewmodel.compose.viewModel

enum class TopDest(val route: String, val label: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    Library("library", R.string.nav_library, Icons.Outlined.VideoLibrary, Icons.Rounded.VideoLibrary),
    Discover("discover", R.string.nav_discover, Icons.Outlined.Explore, Icons.Rounded.Explore),
    Stats("stats", R.string.nav_stats, Icons.Outlined.BarChart, Icons.Rounded.BarChart),
    Settings("settings", R.string.nav_settings, Icons.Outlined.Settings, Icons.Rounded.Settings),
}

private const val DETAIL = "detail"

@Composable
fun AppNav(container: AppContainer, settings: AppSettings) {
    val nav = rememberNavController()
    val factory = AppViewModelFactory(container)
    val reduced = LocalReducedMotion.current
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val onTop = TopDest.entries.any { it.route == route }

    fun enter(): EnterTransition = if (reduced) EnterTransition.None else fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 24 }
    fun exit(): ExitTransition = if (reduced) ExitTransition.None else fadeOut(tween(160))

    Scaffold(
        bottomBar = {
            AnimatedVisibility(onTop, enter = if (reduced) EnterTransition.None else slideInVertically { it }, exit = if (reduced) ExitTransition.None else slideOutVertically { it }) {
                NavigationBar {
                    TopDest.entries.forEach { d ->
                        val selected = backStack?.destination?.route == d.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(d.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) d.selectedIcon else d.icon, contentDescription = null) },
                            label = { Text(stringResource(d.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        SharedTransitionLayout {
            NavHost(nav, startDestination = TopDest.Library.route, modifier = Modifier.padding(padding),
                enterTransition = { enter() }, exitTransition = { exit() }, popEnterTransition = { enter() }, popExitTransition = { exit() }) {
                composable(TopDest.Library.route) {
                    LibraryScreen(viewModel(factory = factory), SharedScopes(this@SharedTransitionLayout, this@composable)) { nav.navigate("$DETAIL/$it") }
                }
                composable(TopDest.Discover.route) { DiscoverScreen() }
                composable(TopDest.Stats.route) { StatsScreen(viewModel(factory = factory)) }
                composable(TopDest.Settings.route) { SettingsScreen(viewModel(factory = factory), settings) }
                composable("$DETAIL/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments?.getLong("id") ?: return@composable
                    DetailScreen(id, viewModel(key = "detail-$id", factory = factory), SharedScopes(this@SharedTransitionLayout, this@composable)) { nav.popBackStack() }
                }
            }
        }
    }
}
