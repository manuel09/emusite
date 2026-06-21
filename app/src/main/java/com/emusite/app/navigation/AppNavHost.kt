package com.emusite.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.emusite.app.data.MainViewModel
import com.emusite.app.ui.screens.*
import com.emusite.app.ui.player.PlayerScreen
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onItemClick = { sourceId, url ->
                    val encodedUrl = URLEncoder.encode(url, "UTF-8")
                    navController.navigate(Screen.Details.createRoute(sourceId, encodedUrl))
                },
                onSectionClick = { sectionIndex ->
                    navController.navigate(Screen.BrowseSection.createRoute(sectionIndex))
                },
                viewModel = viewModel
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onItemClick = { sourceId, url ->
                    val encodedUrl = URLEncoder.encode(url, "UTF-8")
                    navController.navigate(Screen.Details.createRoute(sourceId, encodedUrl))
                },
                viewModel = viewModel
            )
        }

        composable(
            route = Screen.Details.route,
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sourceId = backStackEntry.arguments?.getString("sourceId") ?: return@composable
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: return@composable
            val url = try { URLDecoder.decode(encodedUrl, "UTF-8") } catch (_: Exception) { encodedUrl }

            DetailsScreen(
                sourceId = sourceId,
                url = url,
                onBack = { navController.popBackStack() },
                onEpisodeClick = { sId, episodeUrl, title ->
                    val encodedUrl = URLEncoder.encode(episodeUrl, "UTF-8")
                    val encodedTitle = URLEncoder.encode(title, "UTF-8")
                    navController.navigate(
                        Screen.Player.createRoute(sId, encodedUrl, encodedTitle)
                    )
                },
                viewModel = viewModel
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sourceId = backStackEntry.arguments?.getString("sourceId") ?: return@composable
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: return@composable
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: return@composable
            val url = try { URLDecoder.decode(encodedUrl, "UTF-8") } catch (_: Exception) { encodedUrl }
            val title = try { URLDecoder.decode(encodedTitle, "UTF-8") } catch (_: Exception) { encodedTitle }

            PlayerScreen(
                sourceId = sourceId,
                url = url,
                title = title,
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable(Screen.Plugins.route) {
            PluginsScreen(
                onRepoClick = { repoUrl ->
                    val encoded = URLEncoder.encode(repoUrl, "UTF-8")
                    navController.navigate(Screen.RepoDetail.createRoute(encoded))
                },
                viewModel = viewModel
            )
        }

        composable(
            route = Screen.RepoDetail.route,
            arguments = listOf(
                navArgument("repoUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("repoUrl") ?: return@composable
            val repoUrl = URLDecoder.decode(encodedUrl, "UTF-8")
            RepoDetailScreen(
                repoUrl = repoUrl,
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = viewModel)
        }

        composable(
            route = Screen.BrowseSection.route,
            arguments = listOf(
                navArgument("sectionIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val sectionIndex = backStackEntry.arguments?.getInt("sectionIndex") ?: 0
            BrowseSectionScreen(
                sectionIndex = sectionIndex,
                onBack = { navController.popBackStack() },
                onItemClick = { sourceId, url ->
                    val encodedUrl = URLEncoder.encode(url, "UTF-8")
                    navController.navigate(Screen.Details.createRoute(sourceId, encodedUrl))
                },
                viewModel = viewModel
            )
        }
    }
}
